import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.ToolkitAlphaMode

class ExtensionSpiTest : FreeSpec({
    "surface capabilities expose a preferred format and supported sets" {
        val caps = SurfaceCapabilities(
            preferredFormat = GPUTextureFormat.BGRA8Unorm,
            formats = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm),
            alphaModes = listOf(ToolkitAlphaMode.Opaque, ToolkitAlphaMode.Premultiplied),
        )
        caps.preferredFormat shouldBe GPUTextureFormat.BGRA8Unorm
    }
})
