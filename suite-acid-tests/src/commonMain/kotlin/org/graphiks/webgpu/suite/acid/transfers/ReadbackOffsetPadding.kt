package org.graphiks.webgpu.suite.acid.transfers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

/**
 * `copyTextureToBuffer` writes at the destination offset and row stride it is given: with offset
 * 256 and `bytesPerRow` 256 the two rows land at bytes 256 and 512, and every byte outside those
 * ranges keeps the `0x5a` sentinel written before the copy. The copy is encoded here on purpose so
 * the raw layout is what is asserted, not a padding-stripping helper.
 */
@AcidTest(
    id = AcidCaseId.TransfersReadbackOffsetPadding,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTexelCopyBufferInfo,
        ApiSymbols.GPUTexelCopyBufferLayout_bytesPerRow,
    ],
)
suspend fun readbackOffsetPadding(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(3u, 2u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
        ),
    ).use { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = texture),
            ArrayBuffer.of(rgbaRow(OPAQUE_RED, 3) + rgbaRow(OPAQUE_GREEN, 3)),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 12u, rowsPerImage = 2u),
            Extent3D(3u, 2u, 1u),
        )

        device.createBuffer(
            BufferDescriptor(
                size = 1024uL,
                usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead,
                mappedAtCreation = true,
            ),
        ).use { staging ->
            staging.getMappedRange().setBytes(0uL, ByteArray(1024) { 0x5a.toByte() })
            staging.unmap()

            device.createCommandEncoder().use { encoder ->
                encoder.copyTextureToBuffer(
                    TexelCopyTextureInfo(texture),
                    TexelCopyBufferInfo(staging, offset = 256uL, bytesPerRow = 256u, rowsPerImage = 2u),
                    Extent3D(3u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            val raw = try {
                staging.getMappedRange().toByteArray()
            } finally {
                staging.unmap()
            }

            val expected = ByteArray(1024) { 0x5a.toByte() }
            rgbaRow(OPAQUE_RED, 3).copyInto(expected, 256)
            rgbaRow(OPAQUE_GREEN, 3).copyInto(expected, 512)
            assertContentEquals(expected, raw, "Only the copied rows may change; every sentinel stays 0x5a")

            assertPixel(raw.copyOfRange(256, 1024), 3, 0, 0, 255, 0, 0, 255)
            assertPixel(raw.copyOfRange(512, 1024), 3, 0, 0, 0, 255, 0, 255)
        }
    }
}
