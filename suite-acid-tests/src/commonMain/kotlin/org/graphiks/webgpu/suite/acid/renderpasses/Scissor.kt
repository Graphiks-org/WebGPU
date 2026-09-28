package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * `setScissorRect` discards fragments outside the rectangle: the fullscreen triangle paints only
 * the 8×4 band at (4, 6), and rows above and below stay black.
 */
@AcidTest(
    id = AcidCaseId.RenderScissor,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderPassEncoder_setScissorRect,
        ApiSymbols.GPURenderCommandsMixin_draw,
    ],
)
suspend fun scissor(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(device, FULLSCREEN_TRIANGLE_WGSL).use { pipeline ->
        createColorTarget(device, 16, 16).use { target ->
            val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                pass.setPipeline(pipeline)
                pass.setScissorRect(4u, 6u, 8u, 4u)
                pass.draw(3u)
            }
            assertPixel(pixels, 16, 5, 7, 255, 0, 0, 255)
            assertPixel(pixels, 16, 10, 8, 255, 0, 0, 255)
            assertPixel(pixels, 16, 5, 4, 0, 0, 0, 255)
            assertPixel(pixels, 16, 5, 12, 0, 0, 0, 255)
        }
    }
}
