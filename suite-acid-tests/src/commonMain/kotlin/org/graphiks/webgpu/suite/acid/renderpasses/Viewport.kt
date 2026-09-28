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
 * `setViewport` confines rasterisation to a sub-rectangle of the target: the fullscreen triangle
 * paints only the 8×8 viewport at (4, 4), and the outside stays black.
 */
@AcidTest(
    id = AcidCaseId.RenderViewport,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderPassEncoder_setViewport,
        ApiSymbols.GPURenderCommandsMixin_draw,
    ],
)
suspend fun viewport(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(device, FULLSCREEN_TRIANGLE_WGSL).use { pipeline ->
        createColorTarget(device, 16, 16).use { target ->
            val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                pass.setPipeline(pipeline)
                pass.setViewport(4f, 4f, 8f, 8f, 0f, 1f)
                pass.draw(3u)
            }
            assertPixel(pixels, 16, 6, 6, 255, 0, 0, 255)
            assertPixel(pixels, 16, 1, 1, 0, 0, 0, 255)
            assertPixel(pixels, 16, 14, 14, 0, 0, 0, 255)
        }
    }
}
