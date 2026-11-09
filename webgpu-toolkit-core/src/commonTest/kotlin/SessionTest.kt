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
import org.graphiks.webgpu.toolkit.ToolkitConfig
import org.graphiks.webgpu.toolkit.ToolkitProgression
import org.graphiks.webgpu.toolkit.ToolkitSession
import org.graphiks.webgpu.toolkit.ToolkitSessionState
import org.graphiks.webgpu.toolkit.withSession
import kotlin.time.Duration.Companion.milliseconds

/**
 * Starts a collector on the caller's scope that records every state of the awaited session and
 * returns the recording list plus the collector's [Job].
 */
private fun CoroutineScope.recordStates(
    sessionRef: CompletableDeferred<ToolkitSession>,
): Pair<MutableList<ToolkitSessionState>, Job> {
    val states = mutableListOf<ToolkitSessionState>()
    val job = launch { sessionRef.await().state.collect { states += it } }
    return states to job
}

class SessionTest : FreeSpec({
    "withSession walks Ready to Closed" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<ToolkitSession>()
            val (states, collector) = recordStates(sessionRef)
            val result = withSession(fake) { session ->
                sessionRef.complete(session)
                yield()
                session.device shouldBe fake.device
                session.queue shouldBe fake.device.fakeQueue
                session.state.value shouldBe ToolkitSessionState.Ready
                "block-result"
            }
            result shouldBe "block-result"
            fake.instanceClosed shouldBe true
            advanceUntilIdle()
            collector.cancelAndJoin()
            states shouldContainExactly listOf(
                ToolkitSessionState.Ready,
                ToolkitSessionState.ShuttingDown,
                ToolkitSessionState.Closed,
            )
        }
    }

    "shutdown is idempotent" {
        runTest {
            val fake = FakeExtension()
            withSession(fake) { session ->
                session.shutdown()
                session.shutdown()
                session.state.value shouldBe ToolkitSessionState.Closed
            }
            fake.shutdownCalls shouldBe 1
            fake.instanceClosed shouldBe true
        }
    }

    "shutdown timeout is retryable" {
        runTest {
            val fake = FakeExtension().apply { hangShutdown = true }
            val config = ToolkitConfig(shutdownTimeout = 10.milliseconds)
            withSession(fake, config) { session ->
                session.shutdown()
                session.state.value shouldBe ToolkitSessionState.ShutdownTimedOut
                fake.instanceClosed shouldBe false
                fake.releaseShutdown()
                session.shutdown()
                session.state.value shouldBe ToolkitSessionState.Closed
            }
            fake.openedConfigs shouldContainExactly listOf(config)
            fake.shutdownCalls shouldBe 1
            fake.instanceClosed shouldBe true
        }
    }

    "device loss publishes Lost and cancels admitted work" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<ToolkitSession>()
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
                ToolkitSessionState.Ready,
                ToolkitSessionState.Lost,
                ToolkitSessionState.ShuttingDown,
                ToolkitSessionState.Closed,
            )
        }
    }

    "cancellation shuts down cleanly" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<ToolkitSession>()
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
                ToolkitSessionState.Ready,
                ToolkitSessionState.ShuttingDown,
                ToolkitSessionState.Closed,
            )
        }
    }

    "pump loop runs and stops for Pumped progression" {
        runTest {
            val fake = FakeExtension().apply { progression = ToolkitProgression.Pumped }
            var duringBlock = 0
            withSession(fake) { session ->
                session.state.value shouldBe ToolkitSessionState.Ready
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

    "Manual progression is rejected fast" {
        runTest {
            val fake = FakeExtension().apply { progression = ToolkitProgression.Manual }
            val rejected = shouldThrow<UnsupportedOperationException> {
                withSession(fake) { error("block must not run under Manual progression") }
            }
            rejected.message shouldContain "Pumped"
            rejected.message shouldContain "EventLoop"
            fake.instanceClosed shouldBe true
        }
    }

    "block failure after its own shutdown stays Closed" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<ToolkitSession>()
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
                ToolkitSessionState.Ready,
                ToolkitSessionState.ShuttingDown,
                ToolkitSessionState.Closed,
            )
        }
    }

    "run and handle access are rejected after Closed" {
        runTest {
            val fake = FakeExtension()
            lateinit var session: ToolkitSession
            withSession(fake) { admitted ->
                session = admitted
                admitted.state.value shouldBe ToolkitSessionState.Ready
            }
            session.state.value shouldBe ToolkitSessionState.Closed
            shouldThrow<IllegalStateException> { session.run { } }
            shouldThrow<IllegalStateException> { session.device }
            shouldThrow<IllegalStateException> { session.queue }
        }
    }

    "block failure publishes Failed while Ready" {
        runTest {
            val fake = FakeExtension()
            val sessionRef = CompletableDeferred<ToolkitSession>()
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
                ToolkitSessionState.Ready,
                ToolkitSessionState.Failed,
                ToolkitSessionState.ShuttingDown,
                ToolkitSessionState.Closed,
            )
        }
    }
})
