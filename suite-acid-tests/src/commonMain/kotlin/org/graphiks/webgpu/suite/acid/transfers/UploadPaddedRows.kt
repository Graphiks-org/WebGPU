package org.graphiks.webgpu.suite.acid.transfers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
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
 * `copyBufferToTexture` reads rows at `bytesPerRow` (256 here) so the padding between the two rows
 * never reaches the texture: the red row at byte 0 and the green row at byte 256 produce red and
 * green texels with no `0x55` padding bytes.
 */
@AcidTest(
    id = AcidCaseId.TransfersUploadPaddedRows,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUCommandEncoder_copyBufferToTexture,
        ApiSymbols.GPUTexelCopyBufferLayout_bytesPerRow,
        ApiSymbols.GPUTexelCopyBufferLayout_rowsPerImage,
    ],
)
suspend fun uploadPaddedRows(device: GPUDevice) = withValidationScope(device) {
    val buffer = ByteArray(512) { 0x55.toByte() }
    rgbaRow(OPAQUE_RED, 3).copyInto(buffer, 0)
    rgbaRow(OPAQUE_GREEN, 3).copyInto(buffer, 256)

    device.createBuffer(
        BufferDescriptor(512uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst),
    ).use { source ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(3u, 2u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
            ),
        ).use { texture ->
            device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(buffer))
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToTexture(
                    TexelCopyBufferInfo(source, bytesPerRow = 256u, rowsPerImage = 2u),
                    TexelCopyTextureInfo(texture),
                    Extent3D(3u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            val pixels = readRgba8(device, texture, 3, 2)
            assertContentEquals(rgbaRow(OPAQUE_RED, 3) + rgbaRow(OPAQUE_GREEN, 3), pixels)
            assertPixel(pixels, 3, 0, 0, 255, 0, 0, 255)
            assertPixel(pixels, 3, 2, 1, 0, 255, 0, 255)
        }
    }
}
