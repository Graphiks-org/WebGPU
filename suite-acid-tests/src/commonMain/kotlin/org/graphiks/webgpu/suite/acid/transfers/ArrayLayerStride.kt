package org.graphiks.webgpu.suite.acid.transfers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.Origin3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
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
 * For an array texture, `rowsPerImage * bytesPerRow` is the stride between layers: with
 * `rowsPerImage = 3` and `bytesPerRow = 256` the red layer is at byte 0 and the green layer at byte
 * 768, and each layer keeps its own four texels.
 */
@AcidTest(
    id = AcidCaseId.TransfersArrayLayerStride,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUCommandEncoder_copyBufferToTexture,
        ApiSymbols.GPUTexelCopyBufferLayout_rowsPerImage,
        ApiSymbols.GPUTexelCopyTextureInfo_origin,
    ],
)
suspend fun arrayLayerStride(device: GPUDevice) = withValidationScope(device) {
    val buffer = ByteArray(1280)
    rgbaRow(OPAQUE_RED, 2).copyInto(buffer, 0)
    rgbaRow(OPAQUE_RED, 2).copyInto(buffer, 256)
    rgbaRow(OPAQUE_GREEN, 2).copyInto(buffer, 768)
    rgbaRow(OPAQUE_GREEN, 2).copyInto(buffer, 1024)

    device.createBuffer(
        BufferDescriptor(1280uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst),
    ).use { source ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(2u, 2u, 2u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
            ),
        ).use { texture ->
            device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(buffer))
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToTexture(
                    TexelCopyBufferInfo(source, bytesPerRow = 256u, rowsPerImage = 3u),
                    TexelCopyTextureInfo(texture),
                    Extent3D(2u, 2u, 2u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            assertContentEquals(
                rgbaImage(OPAQUE_RED, 2, 2),
                readRgba8(device, texture, 2, 2, origin = Origin3D(0u, 0u, 0u)),
                "Layer 0 must be red",
            )
            assertContentEquals(
                rgbaImage(OPAQUE_GREEN, 2, 2),
                readRgba8(device, texture, 2, 2, origin = Origin3D(0u, 0u, 1u)),
                "Layer 1 must be green",
            )
        }
    }
}
