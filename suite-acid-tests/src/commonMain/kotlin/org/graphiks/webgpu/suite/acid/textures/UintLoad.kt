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

private const val UINT_LOAD_SHADER = """
@group(0) @binding(0) var tex: texture_2d<u32>;
@group(0) @binding(1) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureLoad(tex, vec2i(0, 0), 0).x;
    out[1] = textureLoad(tex, vec2i(1, 0), 0).x;
}
"""

/**
 * A `texture_2d<u32>` load preserves the integer texel values exactly: the `R32Uint` words
 * `0x12345678` and `0xffffffff` come back unchanged, with no float conversion.
 */
@AcidTest(
    id = AcidCaseId.TexturesUintLoad,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUTextureFormat,
        ApiSymbols.GPUTextureSampleType_Uint,
    ],
)
suspend fun uintLoad(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 1u, 1u),
            format = GPUTextureFormat.R32Uint,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(uintArrayOf(0x12345678u, 0xffffffffu)),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 8u, rowsPerImage = 1u),
            Extent3D(2u, 1u, 1u),
        )

        texture.createView().use { view ->
            val words = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = UINT_LOAD_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(
                        sampleType = GPUTextureSampleType.Uint,
                        viewDimension = GPUTextureViewDimension.TwoD,
                    ),
                    outputBytes = 8uL,
                ),
            ).toUIntArray()

            assertContentEquals(uintArrayOf(0x12345678u, 0xffffffffu), words)
        }
    }
}
