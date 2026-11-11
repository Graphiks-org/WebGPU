import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.SessionConfig
import kotlin.time.Duration.Companion.seconds

class TargetProtocolTest : FreeSpec({
    "toolkit config defaults to ten second timeouts" {
        val config = SessionConfig()
        config.startupTimeout shouldBe 10.seconds
        config.shutdownTimeout shouldBe 10.seconds
    }
    "target state carries generation, physical extent and availability" {
        val state = TargetState(generation = 2, extent = Extent2D(640u, 480u), available = false)
        state.toString() shouldBe "TargetState(generation=2, extent=Extent2D(width=640, height=480), available=false)"
    }
})
