package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureSwizzle
import org.graphiks.webgpu.GPUTextureSwizzleSource
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

private const val SWIZZLE_LOAD_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(1)
fn main() {
    let c = textureLoad(tex, vec2i(0, 0), 0);
    out[0] = u32(round(c.r * 255.0));
    out[1] = u32(round(c.g * 255.0));
    out[2] = u32(round(c.b * 255.0));
    out[3] = u32(round(c.a * 255.0));
}
"""

/**
 * Reads one `[17, 61, 149, 233]` texel through three views. The identity view is the control; `b01r`
 * and `argb` map the four output channels to blue/zero/one/red and alpha/red/green/blue, exercising
 * all six swizzle sources. This is an optional-feature case: without `TextureComponentSwizzle` it is
 * reported `unsupported`.
 */
@AcidTest(
    id = AcidCaseId.TexturesViewSwizzle,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUTextureViewDescriptor_swizzle,
        ApiSymbols.GPUTextureSwizzle,
        ApiSymbols.GPUTextureSwizzle_red,
        ApiSymbols.GPUTextureSwizzle_green,
        ApiSymbols.GPUTextureSwizzle_blue,
        ApiSymbols.GPUTextureSwizzle_alpha,
        ApiSymbols.GPUTextureSwizzle_toWebGpuString,
        ApiSymbols.GPUTextureSwizzleSource,
        ApiSymbols.GPUFeatureName_TextureComponentSwizzle,
        ApiSymbols.GPUTexture_createView,
    ],
    requiredFeatures = [GPUFeatureName.TextureComponentSwizzle],
)
suspend fun viewSwizzle(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(1u, 1u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(byteArrayOf(17, 61, 149.toByte(), 233.toByte())),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 4u, rowsPerImage = 1u),
            Extent3D(1u, 1u, 1u),
        )

        val identity = readSwizzled(texture, device, GPUTextureSwizzle())
        val b01r = readSwizzled(
            texture,
            device,
            GPUTextureSwizzle(
                red = GPUTextureSwizzleSource.Blue,
                green = GPUTextureSwizzleSource.Zero,
                blue = GPUTextureSwizzleSource.One,
                alpha = GPUTextureSwizzleSource.Red,
            ),
        )
        val argb = readSwizzled(
            texture,
            device,
            GPUTextureSwizzle(
                red = GPUTextureSwizzleSource.Alpha,
                green = GPUTextureSwizzleSource.Red,
                blue = GPUTextureSwizzleSource.Green,
                alpha = GPUTextureSwizzleSource.Blue,
            ),
        )

        assertContentEquals(uintArrayOf(17u, 61u, 149u, 233u), identity, "Identity swizzle is the control")
        assertContentEquals(uintArrayOf(149u, 0u, 255u, 17u), b01r, "b01r selects blue, zero, one, red")
        assertContentEquals(uintArrayOf(233u, 17u, 61u, 149u), argb, "argb selects alpha, red, green, blue")
    }
}

private suspend fun readSwizzled(
    texture: org.graphiks.webgpu.GPUTexture,
    device: GPUDevice,
    swizzle: GPUTextureSwizzle,
): UIntArray {
    texture.createView(TextureViewDescriptor(swizzle = swizzle)).use { view ->
        return ArrayBuffer.of(
            computeTextureRead(
                device = device,
                shaderCode = SWIZZLE_LOAD_SHADER,
                textureView = view,
                textureLayout = TextureBindingLayout(
                    sampleType = GPUTextureSampleType.Float,
                    viewDimension = GPUTextureViewDimension.TwoD,
                ),
                outputBytes = 16uL,
            ),
        ).toUIntArray()
    }
}
