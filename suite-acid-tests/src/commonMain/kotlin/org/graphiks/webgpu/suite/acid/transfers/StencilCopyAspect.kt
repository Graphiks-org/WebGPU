package org.graphiks.webgpu.suite.acid.transfers

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
 * A Depth24PlusStencil8 texture cleared to stencil 7 is copied with `aspect = StencilOnly` into a
 * staging buffer at offset 256 with a 256-byte row stride. Every copied stencil texel is the byte
 * 7 and every byte outside those texels keeps the `0xa5` prefill, so an offset-ignoring or
 * range-overrunning copy is caught. The sibling `transfers.texture-copy-aspect` proves the depth
 * aspect; the stencil texel is one byte here.
 */
@AcidTest(
    id = AcidCaseId.TransfersStencilCopyAspect,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureFormat_Depth24PlusStencil8,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_CopySrc,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTexelCopyTextureInfo_aspect,
        ApiSymbols.GPUTextureAspect_StencilOnly,
        ApiSymbols.GPUTexelCopyBufferInfo,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUBufferUsage_MapRead,
    ],
)
suspend fun stencilCopyAspect(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.Depth24PlusStencil8,
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
                            stencilClearValue = 7u,
                            stencilLoadOp = GPULoadOp.Clear,
                            stencilStoreOp = GPUStoreOp.Store,
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
                    TexelCopyTextureInfo(texture = texture, aspect = GPUTextureAspect.StencilOnly),
                    TexelCopyBufferInfo(staging, offset = 256uL, bytesPerRow = 256u, rowsPerImage = 2u),
                    Extent3D(2u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            staging.mapAsync(org.graphiks.webgpu.GPUMapMode.Read).getOrThrow()
            try {
                val bytes = staging.getMappedRange().toByteArray()
                assertTrue(
                    bytes.size == 1024,
                    "Stencil readback must expose exactly 1024 bytes, observed ${bytes.size}",
                )
                // Row 0: two stencil texels at 256-257, row 1: two stencil texels at 512-513.
                val copied = (256..257) + (512..513)
                bytes.forEachIndexed { index, byte ->
                    if (index !in copied) {
                        assertEquals(0xA5.toByte(), byte, "Byte $index outside the copied texels must stay intact")
                    }
                }
                copied.forEach { index ->
                    assertEquals(7, bytes[index].toInt() and 255, "Copied stencil texel at byte $index")
                }
            } finally {
                staging.unmap()
            }
        }
    }
}
