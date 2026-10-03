package org.graphiks.webgpu.suite.acid.textures

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
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope
import org.graphiks.webgpu.suite.acid.transfers.rgbaImage
import kotlin.test.assertContentEquals

private val TRANSPARENT_BLACK = byteArrayOf(0, 0, 0, 0)

/**
 * A fresh texture reads as transparent black — alpha included — and a partial write changes exactly
 * the addressed texels. The two checks use two separate fresh textures: the untouched-zero control
 * is never written, and the partial texture is written before its first read, so a backend cannot
 * make a partially written subresource look initialized by folding the write into a lazy
 * full initialization triggered by an earlier read.
 */
@AcidTest(
    id = AcidCaseId.TexturesPartialInitialization,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTexelCopyTextureInfo_origin,
        ApiSymbols.GPUTexelCopyBufferLayout_bytesPerRow,
    ],
)
suspend fun partialInitialization(device: GPUDevice) = withValidationScope(device) {
    // Control: a fresh texture that is never written reads as RGBA (0,0,0,0).
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopySrc,
        ),
    ).use { fresh ->
        assertContentEquals(
            ByteArray(64),
            readRgba8(device, fresh, 4, 4),
            "A fresh, never-written texture must read as transparent black, alpha included",
        )
    }

    // Probe: a partial write lands on a fresh texture before any read of that texture.
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
        ),
    ).use { partial ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = partial, origin = Origin3D(1u, 1u, 0u)),
            ArrayBuffer.of(rgbaImage(SOLID_GREEN, 2, 2)),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 8u, rowsPerImage = 2u),
            Extent3D(2u, 2u, 1u),
        )

        val expected = ByteArray(64)
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val color = if (x in 1..2 && y in 1..2) SOLID_GREEN else TRANSPARENT_BLACK
                color.copyInto(expected, (y * 4 + x) * 4)
            }
        }
        assertContentEquals(
            expected,
            readRgba8(device, partial, 4, 4),
            "Only the written 2x2 block at (1,1) may be green; every other texel stays transparent black",
        )
    }
}
