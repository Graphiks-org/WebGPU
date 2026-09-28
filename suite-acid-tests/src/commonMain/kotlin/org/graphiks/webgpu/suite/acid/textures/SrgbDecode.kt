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
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals

private const val SRGB_LOAD_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureLoad(tex, vec2i(0, 0), 0);
}
"""

/**
 * Loading an `RGBA8UnormSrgb` texel returns the linearised colour: the stored byte 128 reads back
 * as about 0.2159 for RGB, while alpha is used as-is (128/255 ≈ 0.50196).
 */
@AcidTest(
    id = AcidCaseId.TexturesSrgbDecode,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUTextureFormat_RGBA8UnormSrgb,
        ApiSymbols.GPUTexture_createView,
    ],
)
suspend fun srgbDecode(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(1u, 1u, 1u),
            format = GPUTextureFormat.RGBA8UnormSrgb,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte())),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 4u, rowsPerImage = 1u),
            Extent3D(1u, 1u, 1u),
        )

        texture.createView().use { view ->
            val floats = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = SRGB_LOAD_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                    outputBytes = 16uL,
                ),
            ).toFloatArray()

            val decoded = 0.2158605f
            assertEquals(decoded, floats[0], 0.001f, "sRGB 128 decodes to about 0.2159")
            assertEquals(decoded, floats[1], 0.001f, "sRGB 128 decodes to about 0.2159")
            assertEquals(decoded, floats[2], 0.001f, "sRGB 128 decodes to about 0.2159")
            assertEquals(128f / 255f, floats[3], 1e-6f, "Alpha is not sRGB-encoded")
        }
    }
}
