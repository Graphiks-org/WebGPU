package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.GPUTextureSampleType
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

private const val MIP_LOAD_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureLoad(tex, vec2i(0, 0), 0);
}
"""

/**
 * A view with `baseMipLevel = 1` and `mipLevelCount = 1` exposes mip level 1 of the texture, so a
 * `textureLoad` at level 0 of the view reads the green level and not the red mip level 0.
 */
@AcidTest(
    id = AcidCaseId.TexturesViewBaseMip,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor_baseMipLevel,
        ApiSymbols.GPUTextureViewDescriptor_mipLevelCount,
        ApiSymbols.GPUTextureDescriptor_mipLevelCount,
    ],
)
suspend fun baseMipView(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(8u, 8u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
            mipLevelCount = 3u,
        ),
    ).use { texture ->
        writeSolid(device, texture, mipLevel = 0u, width = 8, height = 8, bytes = byteArrayOf(255.toByte(), 0, 0, 255.toByte()))
        writeSolid(device, texture, mipLevel = 1u, width = 4, height = 4, bytes = byteArrayOf(0, 255.toByte(), 0, 255.toByte()))
        writeSolid(device, texture, mipLevel = 2u, width = 2, height = 2, bytes = byteArrayOf(0, 0, 255.toByte(), 255.toByte()))

        texture.createView(
            TextureViewDescriptor(baseMipLevel = 1u, mipLevelCount = 1u, dimension = GPUTextureViewDimension.TwoD),
        ).use { view ->
            val floats = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = MIP_LOAD_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                    outputBytes = 16uL,
                ),
            ).toFloatArray()

            assertEquals(0f, floats[0], 1e-6f, "The view starts at mip 1, so red must be 0")
            assertEquals(1f, floats[1], 1e-6f, "The view starts at mip 1, so green must be 1")
            assertEquals(0f, floats[2], 1e-6f, "The view starts at mip 1, so blue must be 0")
            assertEquals(1f, floats[3], 1e-6f, "Alpha must be 1")
        }
    }
}

internal fun writeSolid(
    device: GPUDevice,
    texture: org.graphiks.webgpu.GPUTexture,
    mipLevel: UInt,
    width: Int,
    height: Int,
    bytes: ByteArray,
    layer: UInt = 0u,
) {
    val data = ByteArray(width * height * 4)
    for (pixel in 0 until width * height) bytes.copyInto(data, pixel * 4)
    device.queue.writeTexture(
        TexelCopyTextureInfo(
            texture = texture,
            mipLevel = mipLevel,
            origin = org.graphiks.webgpu.descriptors.Origin3D(0u, 0u, layer),
        ),
        ArrayBuffer.of(data),
        TexelCopyBufferLayout(offset = 0uL, bytesPerRow = (width * 4).toUInt(), rowsPerImage = height.toUInt()),
        Extent3D(width.toUInt(), height.toUInt(), 1u),
    )
}
