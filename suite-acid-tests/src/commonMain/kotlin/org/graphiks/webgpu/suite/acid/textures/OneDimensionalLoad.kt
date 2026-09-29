package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
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
import kotlin.test.assertContentEquals

private const val ONE_D_LOAD_SHADER = """
@group(0) @binding(0) var tex: texture_1d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<u32>;

fn pack(offset: u32, c: vec4f) {
    out[offset] = u32(round(c.r * 255.0));
    out[offset + 1u] = u32(round(c.g * 255.0));
    out[offset + 2u] = u32(round(c.b * 255.0));
    out[offset + 3u] = u32(round(c.a * 255.0));
}

@compute @workgroup_size(1)
fn main() {
    pack(0u, textureLoad(tex, 0, 0));
    pack(4u, textureLoad(tex, 1, 0));
    pack(8u, textureLoad(tex, 2, 0));
    pack(12u, textureLoad(tex, 3, 0));
    out[16] = textureDimensions(tex);
}
"""

/**
 * A one-dimensional RGBA8 texture of width 4 is bound as `texture_1d<f32>`; the four distinct texels
 * load into an exact 4×4 channel matrix and `textureDimensions` reports 4.
 */
@AcidTest(
    id = AcidCaseId.TexturesOneDimensionalLoad,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureDescriptor_dimension,
        ApiSymbols.GPUTextureDimension_OneD,
        ApiSymbols.GPUTextureViewDimension_OneD,
        ApiSymbols.GPUTextureSampleType_Float,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUTextureUsage_CopyDst,
        ApiSymbols.GPUTextureUsage_TextureBinding,
    ],
)
suspend fun oneDimensionalLoad(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 1u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
            dimension = org.graphiks.webgpu.GPUTextureDimension.OneD,
        ),
    ).use { texture ->
        val texels = byteArrayOf(
            10, 20, 30, 255.toByte(),
            40, 50, 60, 255.toByte(),
            70, 80, 90, 255.toByte(),
            100, 110, 120, 255.toByte(),
        )
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(texels),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 16u, rowsPerImage = 1u),
            Extent3D(4u, 1u, 1u),
        )

        texture.createView().use { view ->
            val words = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = ONE_D_LOAD_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(
                        sampleType = GPUTextureSampleType.Float,
                        viewDimension = GPUTextureViewDimension.OneD,
                    ),
                    outputBytes = 68uL,
                ),
            ).toUIntArray()

            assertContentEquals(
                uintArrayOf(
                    10u, 20u, 30u, 255u,
                    40u, 50u, 60u, 255u,
                    70u, 80u, 90u, 255u,
                    100u, 110u, 120u, 255u,
                    4u,
                ),
                words,
                "texture_1d loads the four texels and reports its width",
            )
        }
    }
}
