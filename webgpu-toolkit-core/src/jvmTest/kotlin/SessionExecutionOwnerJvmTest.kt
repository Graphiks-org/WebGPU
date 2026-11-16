@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.DelicateCoroutinesApi::class)

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.withSession

/**
 * Tests for [org.graphiks.webgpu.toolkit.Session.executionOwner] that need real OS threads, so
 * they live in the JVM source set rather than in commonTest.
 */
class SessionExecutionOwnerJvmTest : FreeSpec({

    "the session reports the dispatcher it started on as its execution owner" {
        val dispatcher = newSingleThreadContext("toolkit-owner-test")
        try {
            runBlocking(dispatcher) {
                withSession(FakeExtension(), SessionConfig()) { session ->
                    session.executionOwner shouldBe dispatcher
                }
            }
        } finally {
            dispatcher.close()
        }
    }

    "work sent to the execution owner runs on the owning thread" {
        val dispatcher = newSingleThreadContext("toolkit-owner-run-test")
        try {
            runBlocking(dispatcher) {
                withSession(FakeExtension(), SessionConfig()) { session ->
                    val owner = Thread.currentThread()
                    // A different dispatcher first, so coming back to the owner is observable.
                    val elsewhere = withContext(Dispatchers.Default) { Thread.currentThread() }
                    (elsewhere === owner) shouldBe false
                    val back = withContext(checkNotNull(session.executionOwner)) { Thread.currentThread() }
                    back shouldBe owner
                }
            }
        } finally {
            dispatcher.close()
        }
    }
})
