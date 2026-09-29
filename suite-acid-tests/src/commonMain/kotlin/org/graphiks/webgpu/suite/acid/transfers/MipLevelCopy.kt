package org.graphiks.webgpu.suite.acid.transfers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

/**
 * A texture copy addresses one mip level: copying the blue 2×2 mip level 2 of an 8×8 source into a
 * 2×2 destination gives blue everywhere, not the red of mip level 0. Mip 0 is uploaded last, so an
 * address that ignores `mipLevel` on the upload or the copy lands on red and fails instead of
 * coincidentally matching.
 */
@AcidTest(
    id = AcidCaseId.TransfersMipLevelCopy,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUCommandEncoder_copyTextureToTexture,
        ApiSymbols.GPUTexelCopyTextureInfo_mipLevel,
        ApiSymbols.GPUTextureDescriptor_mipLevelCount,
    ],
)
suspend fun mipLevelCopy(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(8u, 8u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
            mipLevelCount = 3u,
        ),
    ).use { source ->
        writeMip(device, source, mipLevel = 2u, color = OPAQUE_BLUE, width = 2, height = 2)
        writeMip(device, source, mipLevel = 1u, color = OPAQUE_GREEN, width = 4, height = 4)
        writeMip(device, source, mipLevel = 0u, color = OPAQUE_RED, width = 8, height = 8)

        device.createTexture(
            TextureDescriptor(
                size = Extent3D(2u, 2u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
            ),
        ).use { destination ->
            device.createCommandEncoder().use { encoder ->
                encoder.copyTextureToTexture(
                    TexelCopyTextureInfo(source, mipLevel = 2u),
                    TexelCopyTextureInfo(destination),
                    Extent3D(2u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            assertContentEquals(
                rgbaImage(OPAQUE_BLUE, 2, 2),
                readRgba8(device, destination, 2, 2),
                "Copying mip level 2 must give the blue level, not mip level 0",
            )
        }
    }
}

private fun writeMip(
    device: GPUDevice,
    texture: org.graphiks.webgpu.GPUTexture,
    mipLevel: UInt,
    color: ByteArray,
    width: Int,
    height: Int,
) {
    device.queue.writeTexture(
        TexelCopyTextureInfo(texture = texture, mipLevel = mipLevel),
        ArrayBuffer.of(rgbaImage(color, width, height)),
        TexelCopyBufferLayout(offset = 0uL, bytesPerRow = (width * 4).toUInt(), rowsPerImage = height.toUInt()),
        Extent3D(width.toUInt(), height.toUInt(), 1u),
    )
}
