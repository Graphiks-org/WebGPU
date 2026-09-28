package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * A pass with no draw call still clears and stores its attachment: every pixel of the red clear is
 * preserved.
 */
@AcidTest(
    id = AcidCaseId.RenderClearOnly,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderPassColorAttachment_loadOp,
        ApiSymbols.GPURenderPassColorAttachment_storeOp,
        ApiSymbols.GPURenderPassColorAttachment_clearValue,
        ApiSymbols.GPULoadOp_Clear,
        ApiSymbols.GPUStoreOp_Store,
    ],
)
suspend fun clearOnly(device: GPUDevice) = withValidationScope(device) {
    createColorTarget(device, 16, 16).use { target ->
        val pixels = renderAndRead(device, target, 16, 16, Color(1.0, 0.0, 0.0, 1.0)) { }
        assertPixel(pixels, 16, 0, 0, 255, 0, 0, 255)
        assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
        assertPixel(pixels, 16, 15, 15, 255, 0, 0, 255)
    }
}
