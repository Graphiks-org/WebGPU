import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.graphiks.webgpu.GPUTextureSwizzle
import org.graphiks.webgpu.GPUTextureSwizzleSource

class GPUTextureSwizzleTest : FreeSpec({
    "default swizzle encodes the identity mapping" {
        GPUTextureSwizzle().toWebGpuString() shouldBe "rgba"
    }

    "swizzle encodes channel selections and constant components" {
        val swizzle = GPUTextureSwizzle(
            red = GPUTextureSwizzleSource.Blue,
            green = GPUTextureSwizzleSource.Zero,
            blue = GPUTextureSwizzleSource.One,
            alpha = GPUTextureSwizzleSource.Red,
        )

        swizzle.toWebGpuString() shouldBe "b01r"
    }
})
