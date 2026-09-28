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
 * `copyTextureToTexture` copies a rectangular region: the 2×2 block at source origin (1, 1) lands
 * at destination origin (0, 2) with its encoded `R = 32 * x`, `G = 32 * y` values, and the rest of
 * the destination stays black.
 */
@AcidTest(
    id = AcidCaseId.TransfersTextureRegionCopy,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUCommandEncoder_copyTextureToTexture,
        ApiSymbols.GPUTexelCopyTextureInfo_origin,
    ],
)
suspend fun textureRegionCopy(device: GPUDevice) = withValidationScope(device) {
    val sourceBytes = ByteArray(4 * 4 * 4)
    for (y in 0 until 4) {
        for (x in 0 until 4) {
            val offset = (y * 4 + x) * 4
            sourceBytes[offset] = (32 * x).toByte()
            sourceBytes[offset + 1] = (32 * y).toByte()
            sourceBytes[offset + 2] = 0
            sourceBytes[offset + 3] = 255.toByte()
        }
    }

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
        ),
    ).use { source ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(4u, 4u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
            ),
        ).use { destination ->
            device.queue.writeTexture(
                TexelCopyTextureInfo(source),
                ArrayBuffer.of(sourceBytes),
                TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 16u, rowsPerImage = 4u),
                Extent3D(4u, 4u, 1u),
            )
            device.queue.writeTexture(
                TexelCopyTextureInfo(destination),
                ArrayBuffer.of(rgbaImage(OPAQUE_BLACK, 4, 4)),
                TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 16u, rowsPerImage = 4u),
                Extent3D(4u, 4u, 1u),
            )

            device.createCommandEncoder().use { encoder ->
                encoder.copyTextureToTexture(
                    TexelCopyTextureInfo(source, origin = Origin3D(1u, 1u, 0u)),
                    TexelCopyTextureInfo(destination, origin = Origin3D(0u, 2u, 0u)),
                    Extent3D(2u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            val expected = ByteArray(64)
            for (y in 0 until 4) {
                for (x in 0 until 4) {
                    val offset = (y * 4 + x) * 4
                    if (x in 0..1 && y in 2..3) {
                        expected[offset] = (32 * (x + 1)).toByte()
                        expected[offset + 1] = (32 * (y - 1)).toByte()
                        expected[offset + 2] = 0
                        expected[offset + 3] = 255.toByte()
                    } else {
                        OPAQUE_BLACK.copyInto(expected, offset)
                    }
                }
            }
            val pixels = readRgba8(device, destination, 4, 4)
            assertContentEquals(expected, pixels, "The copied region and the untouched black must match")
            assertPixel(pixels, 4, 0, 2, 32, 32, 0, 255)
            assertPixel(pixels, 4, 1, 3, 64, 64, 0, 255)
            assertPixel(pixels, 4, 0, 0, 0, 0, 0, 255)
        }
    }
}
