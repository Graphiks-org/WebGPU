package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
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
import kotlin.test.assertEquals

private const val REINTERPRET_LOAD_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureLoad(tex, vec2i(0, 0), 0);
}
"""

/**
 * A `viewFormats` entry lets the same `RGBA8Unorm` bytes be read two ways: the linear view returns
 * 128/255 ≈ 0.50196 and the `Rgba8UnormSrgb` view returns the linearised ≈ 0.21586, for identical
 * bytes. Alpha is identical in both because sRGB never applies to alpha.
 */
@AcidTest(
    id = AcidCaseId.TexturesSrgbViewFormat,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureDescriptor_viewFormats,
        ApiSymbols.GPUTextureViewDescriptor_format,
        ApiSymbols.GPUTexture_createView,
    ],
)
suspend fun reinterpretSrgbView(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(1u, 1u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
            viewFormats = listOf(GPUTextureFormat.RGBA8UnormSrgb),
        ),
    ).use { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte())),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 4u, rowsPerImage = 1u),
            Extent3D(1u, 1u, 1u),
        )

        val linear = texture.createView().use { view ->
            ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = REINTERPRET_LOAD_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                    outputBytes = 16uL,
                ),
            ).toFloatArray()
        }
        val srgb = texture.createView(
            TextureViewDescriptor(format = GPUTextureFormat.RGBA8UnormSrgb),
        ).use { view ->
            ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = REINTERPRET_LOAD_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                    outputBytes = 16uL,
                ),
            ).toFloatArray()
        }

        val raw = 128f / 255f
        val decoded = 0.2158605f
        for (channel in 0..2) {
            assertEquals(raw, linear[channel], 1e-6f, "Linear view channel $channel is the raw byte/255")
            assertEquals(decoded, srgb[channel], 0.001f, "sRGB view channel $channel decodes the same byte")
        }
        assertEquals(raw, linear[3], 1e-6f, "Linear view alpha is the raw byte/255")
        assertEquals(raw, srgb[3], 1e-6f, "sRGB view alpha is the raw byte/255")
    }
}
