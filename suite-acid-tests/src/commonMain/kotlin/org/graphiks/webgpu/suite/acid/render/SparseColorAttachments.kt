package org.graphiks.webgpu.suite.acid.render

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val LOCATION_ONE_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(1) vec4f { return vec4f(0, 1, 0, 1); }
"""

/**
 * A null slot in `GPUFragmentState.targets` and in `GPURenderPassDescriptor.colorAttachments`
 * keeps the index of the following entry: the pipeline writes location 1 into the attachment at
 * index 1, and the readback of that attachment is green.
 */
@AcidTest(
    id = AcidCaseId.RenderSparseColorAttachments,
    family = AcidFamily.RenderSparseTargets,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUFragmentState_targets,
        ApiSymbols.GPURenderPassDescriptor_colorAttachments,
        ApiSymbols.GPURenderPassColorAttachment_view,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun sparseColorAttachments(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        LOCATION_ONE_SHADER,
        colorTargets = listOf(null, ColorTargetState(GPUTextureFormat.RGBA8Unorm)),
    ).use { pipeline ->
        createColorTarget(device, 16, 16).use { target ->
            target.createView().use { view ->
                device.createCommandEncoder().use { encoder ->
                    val pass = encoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachments = listOf(
                                null,
                                RenderPassColorAttachment(
                                    view = view,
                                    loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                    storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                    clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                ),
                            ),
                        ),
                    )
                    pass.setPipeline(pipeline)
                    pass.draw(3u)
                    pass.end()
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            }

            val pixels = readRgba8(device, target, 16, 16)
            assertPixel(pixels, 16, 8, 8, 0, 255, 0, 255)
            assertPixel(pixels, 16, 2, 2, 0, 255, 0, 255)
            assertPixel(pixels, 16, 15, 15, 0, 255, 0, 255)
        }
    }
}
