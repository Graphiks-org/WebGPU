package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * An attachment stored with `GPUStoreOp.Discard` is cleared to zero by the pass, so a following
 * `Load` pass reads it as transparent black — while its sibling, stored normally, keeps its clear
 * colour. If `Discard` were ignored and the colour stored, the second pass would load the old green
 * instead of (0,0,0,0) and the case fails.
 */
@AcidTest(
    id = AcidCaseId.RenderDiscardReinit,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPURenderPassColorAttachment_loadOp,
        ApiSymbols.GPURenderPassColorAttachment_storeOp,
        ApiSymbols.GPULoadOp_Clear,
        ApiSymbols.GPULoadOp_Load,
        ApiSymbols.GPUStoreOp_Store,
        ApiSymbols.GPUStoreOp_Discard,
    ],
)
suspend fun discardReinit(device: GPUDevice) = withValidationScope(device) {
    createColorTarget(device, 16, 16).use { stored ->
        createColorTarget(device, 16, 16).use { discarded ->
            stored.createView().use { storedView ->
                discarded.createView().use { discardedView ->
                    device.createCommandEncoder().use { encoder ->
                        val first = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = storedView,
                                        loadOp = GPULoadOp.Clear,
                                        storeOp = GPUStoreOp.Store,
                                        clearValue = Color(1.0, 0.0, 0.0, 1.0),
                                    ),
                                    RenderPassColorAttachment(
                                        view = discardedView,
                                        loadOp = GPULoadOp.Clear,
                                        storeOp = GPUStoreOp.Discard,
                                        clearValue = Color(0.0, 1.0, 0.0, 1.0),
                                    ),
                                ),
                            ),
                        )
                        first.end()

                        // No draw: the second pass only loads and stores, so the observed values are
                        // exactly what the first pass left behind.
                        val second = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = storedView,
                                        loadOp = GPULoadOp.Load,
                                        storeOp = GPUStoreOp.Store,
                                    ),
                                    RenderPassColorAttachment(
                                        view = discardedView,
                                        loadOp = GPULoadOp.Load,
                                        storeOp = GPUStoreOp.Store,
                                    ),
                                ),
                            ),
                        )
                        second.end()
                        encoder.finish().use { device.queue.submit(listOf(it)) }
                    }
                }
            }

            val storedPixels = readRgba8(device, stored, 16, 16)
            assertPixel(storedPixels, 16, 8, 8, 255, 0, 0, 255)
            assertPixel(storedPixels, 16, 1, 1, 255, 0, 0, 255)

            val discardedPixels = readRgba8(device, discarded, 16, 16)
            assertPixel(discardedPixels, 16, 8, 8, 0, 0, 0, 0)
            assertPixel(discardedPixels, 16, 1, 1, 0, 0, 0, 0)
        }
    }
}
