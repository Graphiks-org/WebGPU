import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.AlphaMode

class ExtensionSpiTest : FreeSpec({
    "alpha mode covers web and native composite modes" {
        AlphaMode.entries.map { it.name } shouldBe
            listOf("Opaque", "Premultiplied", "PostMultiplied")
    }

    "surface capabilities expose a preferred format and supported sets" {
        val caps = SurfaceCapabilities(
            preferredFormat = GPUTextureFormat.BGRA8Unorm,
            formats = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm),
            alphaModes = listOf(AlphaMode.Opaque, AlphaMode.Premultiplied),
        )
        caps.preferredFormat shouldBe GPUTextureFormat.BGRA8Unorm
    }
})
