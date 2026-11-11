import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.SessionState
import org.graphiks.webgpu.toolkit.withSession
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * JVM-only shutdown semantics that need a REAL blocked thread: a native `pumpEvents()` that
 * never returns occupies its thread instead of suspending, so no virtual-time fake can model
 * it. The session runs on [Dispatchers.Default]; its pump loop blocks a worker thread on a
 * latch, exactly like a stuck native pump.
 */
class SessionShutdownJvmTest : FreeSpec({
    "shutdown timeout bounds a pump blocked in pumpEvents" {
        runBlocking {
            val pumpGate = CountDownLatch(1)
            val fake = FakeExtension().apply {
                hangShutdown = true // keep the release stuck so the state stays ShutdownTimedOut
                onPump = { pumpGate.await() } // the pump call never returns until released
            }
            val config = SessionConfig(shutdownTimeout = 100.milliseconds)
            // The unbounded join this test guards against would hang past the whole budget.
            withTimeout(10.seconds) {
                withContext(Dispatchers.Default) {
                    withSession(fake, config) { session ->
                        delay(200.milliseconds) // let the pump loop enter the blocked call
                        session.shutdown()
                        session.state.value shouldBe SessionState.ShutdownTimedOut
                    }
                }
            }
            fake.instanceClosed shouldBe false
            fake.shutdownCalls shouldBe 0
            // Unblock everything so no worker thread stays parked after the test.
            fake.releaseShutdown()
            pumpGate.countDown()
        }
    }
})
