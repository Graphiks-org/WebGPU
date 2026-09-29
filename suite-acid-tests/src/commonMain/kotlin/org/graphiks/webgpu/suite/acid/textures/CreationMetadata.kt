package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureDimension
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.Origin3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals

/**
 * An 8×4×3-layer texture exposes exactly the dimensions, mip count, sample count, dimension, format
 * and usage set it was created with, and the data written into mip 1 of layer 2 reads back there.
 * A separate multisampled texture reports its sample count from the getter, without a direct copy of
 * a multisampled resource.
 */
@AcidTest(
    id = AcidCaseId.TexturesCreationMetadata,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureDescriptor,
        ApiSymbols.GPUTextureDescriptor_size,
        ApiSymbols.GPUTextureDescriptor_format,
        ApiSymbols.GPUTextureDescriptor_usage,
        ApiSymbols.GPUTextureDescriptor_mipLevelCount,
        ApiSymbols.GPUTextureDescriptor_sampleCount,
        ApiSymbols.GPUTextureDescriptor_dimension,
        ApiSymbols.GPUTexture_width,
        ApiSymbols.GPUTexture_height,
        ApiSymbols.GPUTexture_depthOrArrayLayers,
        ApiSymbols.GPUTexture_mipLevelCount,
        ApiSymbols.GPUTexture_sampleCount,
        ApiSymbols.GPUTexture_dimension,
        ApiSymbols.GPUTexture_format,
        ApiSymbols.GPUTexture_usage,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUTextureUsage,
        ApiSymbols.GPUTextureUsage_TextureBinding,
        ApiSymbols.GPUTextureUsage_CopyDst,
        ApiSymbols.GPUTextureUsage_CopySrc,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
    ],
)
suspend fun creationMetadata(device: GPUDevice) = withValidationScope(device) {
    val layeredUsage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(8u, 4u, 3u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = layeredUsage,
            mipLevelCount = 3u,
        ),
    ).use { texture ->
        assertEquals(8u, texture.width, "width")
        assertEquals(4u, texture.height, "height")
        assertEquals(3u, texture.depthOrArrayLayers, "depthOrArrayLayers")
        assertEquals(3u, texture.mipLevelCount, "mipLevelCount")
        assertEquals(1u, texture.sampleCount, "sampleCount")
        assertEquals(GPUTextureDimension.TwoD, texture.dimension, "dimension")
        assertEquals(GPUTextureFormat.RGBA8Unorm, texture.format, "format")
        assertEquals(
            setOf(GPUTextureUsage.TextureBinding, GPUTextureUsage.CopyDst, GPUTextureUsage.CopySrc),
            texture.usage,
            "usage must be exactly the created set",
        )

        // Mip 1 is 4x2; green fills all eight texels of layer 2.
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = texture, mipLevel = 1u, origin = Origin3D(0u, 0u, 2u)),
            ArrayBuffer.of(ByteArray(32) { SOLID_GREEN[it % 4] }),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 16u, rowsPerImage = 2u),
            Extent3D(4u, 2u, 1u),
        )
        val pixels = readRgba8(device, texture, 4, 2, mipLevel = 1u, origin = Origin3D(0u, 0u, 2u))
        for (y in 0 until 2) {
            for (x in 0 until 4) assertPixel(pixels, 4, x, y, 0, 255, 0, 255)
        }
    }

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment,
            sampleCount = 4u,
        ),
    ).use { multisampled ->
        assertEquals(4u, multisampled.sampleCount, "A multisampled texture reports its sample count")
        assertEquals(GPUTextureDimension.TwoD, multisampled.dimension)
        assertEquals(1u, multisampled.mipLevelCount, "A multisampled texture keeps one mip level")
        assertEquals(4u, multisampled.width)
        assertEquals(4u, multisampled.height)
    }
}
