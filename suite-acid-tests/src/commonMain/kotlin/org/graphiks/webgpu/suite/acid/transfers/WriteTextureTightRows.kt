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
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

/**
 * `writeTexture` accepts a tightly packed row stride (12 bytes for three RGBA8 texels); the
 * 256-byte multiple constraint is a `copyBufferToTexture` rule, not a `writeTexture` one. The two
 * rows come back red then green.
 */
@AcidTest(
    id = AcidCaseId.TransfersWriteTextureTightRows,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTexelCopyBufferLayout_bytesPerRow,
    ],
)
suspend fun writeTextureTightRows(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(3u, 2u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
        ),
    ).use { texture ->
        val data = rgbaRow(OPAQUE_RED, 3) + rgbaRow(OPAQUE_GREEN, 3)
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = texture),
            ArrayBuffer.of(data),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 12u, rowsPerImage = 2u),
            Extent3D(3u, 2u, 1u),
        )

        val pixels = readRgba8(device, texture, 3, 2)
        assertContentEquals(rgbaRow(OPAQUE_RED, 3) + rgbaRow(OPAQUE_GREEN, 3), pixels)
        assertPixel(pixels, 3, 0, 0, 255, 0, 0, 255)
        assertPixel(pixels, 3, 2, 0, 255, 0, 0, 255)
        assertPixel(pixels, 3, 0, 1, 0, 255, 0, 255)
        assertPixel(pixels, 3, 2, 1, 0, 255, 0, 255)
    }
}
