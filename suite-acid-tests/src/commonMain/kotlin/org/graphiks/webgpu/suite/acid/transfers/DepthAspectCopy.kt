package org.graphiks.webgpu.suite.acid.transfers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureAspect
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A Depth32Float texture cleared to 0.25 is copied with `aspect = DepthOnly` into a staging buffer
 * at offset 256 with a 256-byte row stride. The four copied floats read 0.25 exactly and every byte
 * outside those texels keeps the `0xa5` prefill, so an offset-ignoring or range-overrunning copy is
 * caught. Depth32Float has a defined copied representation here; this is not generalised to
 * Depth24Plus.
 */
@AcidTest(
    id = AcidCaseId.TransfersTextureCopyAspect,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureFormat_Depth32Float,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_CopySrc,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTexelCopyTextureInfo_aspect,
        ApiSymbols.GPUTextureAspect_DepthOnly,
        ApiSymbols.GPUTexelCopyBufferInfo,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUBufferUsage_MapRead,
    ],
)
suspend fun depthAspectCopy(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.Depth32Float,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        ),
    ).use { texture ->
        texture.createView().use { view ->
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = emptyList(),
                        depthStencilAttachment = RenderPassDepthStencilAttachment(
                            view = view,
                            depthClearValue = 0.25f,
                            depthLoadOp = GPULoadOp.Clear,
                            depthStoreOp = GPUStoreOp.Store,
                        ),
                    ),
                )
                pass.end()
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        device.createBuffer(
            BufferDescriptor(
                size = 1024uL,
                usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead,
                mappedAtCreation = true,
            ),
        ).use { staging ->
            staging.getMappedRange().setBytes(0uL, ByteArray(1024) { 0xA5.toByte() })
            staging.unmap()

            device.createCommandEncoder().use { encoder ->
                encoder.copyTextureToBuffer(
                    TexelCopyTextureInfo(texture = texture, aspect = GPUTextureAspect.DepthOnly),
                    TexelCopyBufferInfo(staging, offset = 256uL, bytesPerRow = 256u, rowsPerImage = 2u),
                    Extent3D(2u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            staging.mapAsync(org.graphiks.webgpu.GPUMapMode.Read).getOrThrow()
            try {
                val mapped = staging.getMappedRange()
                val bytes = mapped.toByteArray()
                assertTrue(
                    mapped.size == 1024uL && bytes.size == 1024,
                    "Depth readback must expose exactly 1024 bytes, observed ${mapped.size} in the range and ${bytes.size} in the array",
                )
                val row0 = 256 until 264
                val row1 = 512 until 520
                val copied = row0 + row1
                bytes.forEachIndexed { index, byte ->
                    if (index !in copied) {
                        assertEquals(0xA5.toByte(), byte, "Byte $index outside the copied texels must stay intact")
                    }
                }
                val row0Floats = ArrayBuffer.of(bytes.copyOfRange(256, 264)).toFloatArray()
                val row1Floats = ArrayBuffer.of(bytes.copyOfRange(512, 520)).toFloatArray()
                (row0Floats + row1Floats).forEachIndexed { index, value ->
                    assertEquals(0.25f, value, 0.0f, "Copied depth texel $index")
                }
            } finally {
                staging.unmap()
            }
        }
    }
}
