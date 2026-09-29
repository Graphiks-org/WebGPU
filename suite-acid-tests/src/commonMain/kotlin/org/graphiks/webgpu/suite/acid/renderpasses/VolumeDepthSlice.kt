package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureDimension
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.Origin3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * A 2×2×2 `ThreeD` texture starts blue; a render pass whose colour attachment selects
 * `depthSlice = 1` clears that slice red. Reading z0 back as blue and z1 as red proves the
 * attachment wrote one slice of the 3D texture rather than the whole texture or a 2D view.
 */
@AcidTest(
    id = AcidCaseId.RenderVolumeDepthSlice,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureDescriptor_dimension,
        ApiSymbols.GPUTextureDimension_ThreeD,
        ApiSymbols.GPUTextureUsage_CopyDst,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_CopySrc,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor_dimension,
        ApiSymbols.GPUTextureViewDimension_ThreeD,
        ApiSymbols.GPUQueue_writeTexture,
        ApiSymbols.GPURenderPassColorAttachment_depthSlice,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun volumeDepthSlice(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 2u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            dimension = GPUTextureDimension.ThreeD,
        ),
    ).use { texture ->
        // Eight blue texels across both depth slices.
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(ByteArray(32) { index -> if (index % 4 >= 2) 255.toByte() else 0 }),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 8u, rowsPerImage = 2u),
            Extent3D(2u, 2u, 2u),
        )

        texture.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.ThreeD)).use { view ->
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = listOf(
                            RenderPassColorAttachment(
                                view = view,
                                loadOp = GPULoadOp.Clear,
                                storeOp = GPUStoreOp.Store,
                                clearValue = Color(1.0, 0.0, 0.0, 1.0),
                                depthSlice = 1u,
                            ),
                        ),
                    ),
                )
                pass.end()
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        val z0 = readRgba8(device, texture, 2, 2, origin = Origin3D(0u, 0u, 0u))
        val z1 = readRgba8(device, texture, 2, 2, origin = Origin3D(0u, 0u, 1u))
        for (y in 0 until 2) {
            for (x in 0 until 2) {
                assertPixel(z0, 2, x, y, 0, 0, 255, 255)
                assertPixel(z1, 2, x, y, 255, 0, 0, 255)
            }
        }
    }
}
