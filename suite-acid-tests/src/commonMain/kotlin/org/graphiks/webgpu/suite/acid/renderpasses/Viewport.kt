package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.renderbundles.BUNDLE_SQUARE_SHADER
import org.graphiks.webgpu.suite.acid.renderbundles.BUNDLE_SQUARE_VERTICES
import org.graphiks.webgpu.suite.acid.renderbundles.bundleVertexLayout
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * `setViewport` transforms the drawn clip-space geometry into a sub-rectangle: the square spanning
 * NDC [-0.5, 0.5]^2 maps to framebuffer pixels 6..10 of the 16×16 target. Clipping the same geometry
 * with `setScissorRect(4, 4, 8, 8)` would instead paint 4..12, so pixels (5,5) and (11,11) tell the
 * coordinate transform apart from a scissor.
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
    device.createBuffer(
        BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst),
    ).use { vertices ->
        device.queue.writeBuffer(vertices, 0uL, ArrayBuffer.of(BUNDLE_SQUARE_VERTICES))
        createRenderPipeline(device, BUNDLE_SQUARE_SHADER, vertexLayouts = bundleVertexLayout()).use { pipeline ->
            createColorTarget(device, 16, 16).use { target ->
                val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                    pass.setPipeline(pipeline)
                    pass.setVertexBuffer(0u, vertices)
                    pass.setViewport(4f, 4f, 8f, 8f, 0f, 1f)
                    pass.draw(6u)
                }
                assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                assertPixel(pixels, 16, 5, 5, 0, 0, 0, 255)
                assertPixel(pixels, 16, 11, 11, 0, 0, 0, 255)
                assertPixel(pixels, 16, 1, 1, 0, 0, 0, 255)
                assertPixel(pixels, 16, 14, 14, 0, 0, 0, 255)
            }
        }
    }
}
