package org.graphiks.webgpu.suite.acid.transfers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
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
import kotlin.test.assertContentEquals

/**
 * `writeTexture` writes a sub-rectangle: into a black 4×4 texture a red 2×2 block at origin (1, 1)
 * changes exactly those four texels and leaves the surrounding border black.
 */
@AcidTest(
    id = AcidCaseId.TransfersWriteSubrectangle,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUTexelCopyTextureInfo_origin,
        ApiSymbols.GPUTexelCopyBufferLayout_bytesPerRow,
    ],
)
suspend fun writeSubrectangle(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
        ),
    ).use { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = texture),
            ArrayBuffer.of(rgbaImage(OPAQUE_BLACK, 4, 4)),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 16u, rowsPerImage = 4u),
            Extent3D(4u, 4u, 1u),
        )
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = texture, origin = Origin3D(1u, 1u, 0u)),
            ArrayBuffer.of(rgbaImage(OPAQUE_RED, 2, 2)),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 8u, rowsPerImage = 2u),
            Extent3D(2u, 2u, 1u),
        )

        val expected = ByteArray(64)
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val color = if (x in 1..2 && y in 1..2) OPAQUE_RED else OPAQUE_BLACK
                color.copyInto(expected, (y * 4 + x) * 4)
            }
        }
        val pixels = readRgba8(device, texture, 4, 4)
        assertContentEquals(expected, pixels, "Only the 2x2 block at (1, 1) may be red")
        assertPixel(pixels, 4, 1, 1, 255, 0, 0, 255)
        assertPixel(pixels, 4, 2, 2, 255, 0, 0, 255)
        assertPixel(pixels, 4, 0, 0, 0, 0, 0, 255)
        assertPixel(pixels, 4, 3, 3, 0, 0, 0, 255)
    }
}
