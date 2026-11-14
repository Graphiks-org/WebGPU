@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.graphiks.webgpu.toolkit.DeviceLostException
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.Progression
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionState
import org.graphiks.webgpu.toolkit.withSession
import kotlin.time.Duration.Companion.milliseconds

/**
 * Starts a collector on the caller's scope that records every state of the awaited session and
 * returns the recording list plus the collector's [Job].
 */
private fun CoroutineScope.recordStates(
    sessionRef: CompletableDeferred<Session>,
): Pair<MutableList<SessionState>, Job> {
    val states = mutableListOf<SessionState>()
    val job = launch { sessionRef.await().state.collect { states += it } }
    return states to job
}

class SessionTest : FreeSpec({
    "withSession walks Ready to Closed" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<Session>()
            val (states, collector) = recordStates(sessionRef)
            val result = withSession(fake) { session ->
                sessionRef.complete(session)
                yield()
                session.device shouldBe fake.device
                session.queue shouldBe fake.device.fakeQueue
                session.state.value shouldBe SessionState.Ready
                "block-result"
            }
            result shouldBe "block-result"
            fake.instanceClosed shouldBe true
            advanceUntilIdle()
            collector.cancelAndJoin()
            states shouldContainExactly listOf(
                SessionState.Ready,
                SessionState.ShuttingDown,
                SessionState.Closed,
            )
        }
    }

    "shutdown is idempotent" {
        runTest {
            val fake = FakeExtension()
            withSession(fake) { session ->
                session.shutdown()
                session.shutdown()
                session.state.value shouldBe SessionState.Closed
            }
            fake.shutdownCalls shouldBe 1
            fake.instanceClosed shouldBe true
        }
    }

    "shutdown timeout is retryable" {
        runTest {
            val fake = FakeExtension().apply { hangShutdown = true }
            val config = SessionConfig(shutdownTimeout = 10.milliseconds)
            withSession(fake, config) { session ->
                session.shutdown()
                session.state.value shouldBe SessionState.ShutdownTimedOut
                fake.instanceClosed shouldBe false
                fake.releaseShutdown()
                session.shutdown()
                session.state.value shouldBe SessionState.Closed
            }
            fake.openedConfigs shouldContainExactly listOf(config)
            fake.shutdownCalls shouldBe 1
            fake.instanceClosed shouldBe true
        }
    }

    "background shutdown retries exhaust and stay ShutdownTimedOut" {
        runTest {
            val fake = FakeExtension().apply { hangShutdown = true }
            lateinit var session: Session
            withSession(fake, SessionConfig(shutdownTimeout = 10.milliseconds)) { admitted ->
                session = admitted
                session.shutdown()
                session.state.value shouldBe SessionState.ShutdownTimedOut
            }
            // The background loop burns its five bounded attempts and then gives up.
            advanceUntilIdle()
            session.state.value shouldBe SessionState.ShutdownTimedOut
            fake.instanceClosed shouldBe false
            fake.shutdownCalls shouldBe 0
        }
    }

    "device loss publishes Lost and cancels admitted work" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<Session>()
            val (states, collector) = recordStates(sessionRef)
            val lost = shouldThrow<DeviceLostException> {
                withSession(fake) { session ->
                    sessionRef.complete(session)
                    yield()
                    fake.loseDevice()
                    delay(10_000)
                }
            }
            lost.info.message shouldBe "fake device lost"
            advanceUntilIdle()
            collector.cancelAndJoin()
            states shouldContainExactly listOf(
                SessionState.Ready,
                SessionState.Lost,
                SessionState.ShuttingDown,
                SessionState.Closed,
            )
        }
    }

    "cancellation shuts down cleanly" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<Session>()
            val (states, collector) = recordStates(sessionRef)
            val job = launch {
                withSession(fake) { session ->
                    sessionRef.complete(session)
                    awaitCancellation()
                }
            }
            repeat(3) { yield() }
            job.cancelAndJoin()
            fake.instanceClosed shouldBe true
            advanceUntilIdle()
            collector.cancelAndJoin()
            states shouldContainExactly listOf(
                SessionState.Ready,
                SessionState.ShuttingDown,
                SessionState.Closed,
            )
        }
    }

    "pump loop runs and stops for Pumped progression" {
        runTest {
            val fake = FakeExtension().apply { progression = Progression.Pumped }
            var duringBlock = 0
            withSession(fake) { session ->
                session.state.value shouldBe SessionState.Ready
                repeat(10) { yield() }
                duringBlock = fake.pumpCount
            }
            duringBlock shouldBeGreaterThan 0
            val afterClose = fake.pumpCount
            advanceUntilIdle()
            delay(100)
            fake.pumpCount shouldBe afterClose
        }
    }

    "Manual progression hands the pump over and shuts down after stop" {
        runTest {
            val fake = FakeExtension().apply { progression = Progression.Manual }
            val sessionRef = CompletableDeferred<Session>()
            withSession(fake) { session ->
                sessionRef.complete(session)
                val manual = checkNotNull(session.manualProgress) {
                    "a Manual session exposes its hand-over"
                }
                val before = fake.pumpCount
                manual.processEvents()
                fake.pumpCount shouldBe before + 1

                // A live driver owns the pump: shutdown is rejected until the hand-over ends.
                shouldThrow<IllegalStateException> { session.shutdown() }
                manual.stop()
                manual.stop() // idempotent
                shouldThrow<IllegalStateException> { manual.processEvents() }
                session.shutdown()
            }
            fake.instanceClosed shouldBe true
            sessionRef.await().state.value shouldBe SessionState.Closed
        }
    }

    "Pumped and EventLoop sessions expose no manual hand-over" {
        runTest {
            for (progression in listOf(Progression.Pumped, Progression.EventLoop)) {
                val fake = FakeExtension().apply { this.progression = progression }
                withSession(fake) { session ->
                    session.manualProgress shouldBe null
                }
            }
        }
    }

    "block failure after its own shutdown stays Closed" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<Session>()
            val (states, collector) = recordStates(sessionRef)
            val boom = shouldThrow<IllegalStateException> {
                withSession(fake) { session ->
                    sessionRef.complete(session)
                    yield()
                    session.shutdown()
                    error("boom after shutdown")
                }
            }
            boom.message shouldBe "boom after shutdown"
            fake.instanceClosed shouldBe true
            advanceUntilIdle()
            collector.cancelAndJoin()
            states shouldContainExactly listOf(
                SessionState.Ready,
                SessionState.ShuttingDown,
                SessionState.Closed,
            )
        }
    }

    "run and handle access are rejected after Closed" {
        runTest {
            val fake = FakeExtension()
            lateinit var session: Session
            withSession(fake) { admitted ->
                session = admitted
                admitted.state.value shouldBe SessionState.Ready
            }
            session.state.value shouldBe SessionState.Closed
            shouldThrow<IllegalStateException> { session.run { } }
            shouldThrow<IllegalStateException> { session.device }
            shouldThrow<IllegalStateException> { session.queue }
        }
    }

    "block failure publishes Failed while Ready" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<Session>()
            val (states, collector) = recordStates(sessionRef)
            shouldThrow<IllegalStateException> {
                withSession(fake) { session ->
                    sessionRef.complete(session)
                    yield()
                    error("boom while ready")
                }
            }
            advanceUntilIdle()
            collector.cancelAndJoin()
            states shouldContainExactly listOf(
                SessionState.Ready,
                SessionState.Failed,
                SessionState.ShuttingDown,
                SessionState.Closed,
            )
        }
    }
})
