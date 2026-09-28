package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * A `load` pass preserves the attachment content a prior pass stored: a first pass clears blue, and
 * a second pass draws a red square limited by a scissor, leaving the rest blue.
 */
@AcidTest(
    id = AcidCaseId.RenderLoadPreserves,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderPassColorAttachment_loadOp,
        ApiSymbols.GPULoadOp_Load,
        ApiSymbols.GPURenderPassEncoder_setScissorRect,
        ApiSymbols.GPURenderCommandsMixin_draw,
    ],
)
suspend fun loadPreserves(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(device, FULLSCREEN_TRIANGLE_WGSL).use { pipeline ->
        createColorTarget(device, 16, 16).use { target ->
            target.createView().use { view ->
                device.createCommandEncoder().use { encoder ->
                    encoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachments = listOf(
                                RenderPassColorAttachment(
                                    view = view,
                                    loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                    storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                    clearValue = Color(0.0, 0.0, 1.0, 1.0),
                                ),
                            ),
                        ),
                    ).end()

                    val pass = encoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachments = listOf(
                                RenderPassColorAttachment(
                                    view = view,
                                    loadOp = org.graphiks.webgpu.GPULoadOp.Load,
                                    storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                ),
                            ),
                        ),
                    )
                    pass.setPipeline(pipeline)
                    pass.setScissorRect(4u, 4u, 8u, 8u)
                    pass.draw(3u)
                    pass.end()
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            }

            val pixels = readRgba8(device, target, 16, 16)
            assertPixel(pixels, 16, 6, 6, 255, 0, 0, 255)
            assertPixel(pixels, 16, 1, 1, 0, 0, 255, 255)
            assertPixel(pixels, 16, 14, 14, 0, 0, 255, 255)
        }
    }
}
