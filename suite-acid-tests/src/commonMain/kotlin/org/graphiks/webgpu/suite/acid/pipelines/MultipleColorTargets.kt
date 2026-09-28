package org.graphiks.webgpu.suite.acid.pipelines

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

private const val TWO_TARGETS_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

struct Targets {
    @location(0) first: vec4f,
    @location(1) second: vec4f,
}

@fragment fn fragmentMain() -> Targets {
    var out: Targets;
    out.first = vec4f(1, 0, 0, 1);
    out.second = vec4f(0, 1, 0, 1);
    return out;
}
"""

/**
 * A pipeline writes two colour attachments at once: location 0 is red in the first target and
 * location 1 is green in the second, read back separately.
 */
@AcidTest(
    id = AcidCaseId.RenderMultipleTargets,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUFragmentState_targets,
        ApiSymbols.GPUColorTargetState,
        ApiSymbols.GPURenderPassColorAttachment_view,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun multipleColorTargets(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        TWO_TARGETS_SHADER,
        colorTargets = listOf(
            ColorTargetState(GPUTextureFormat.RGBA8Unorm),
            ColorTargetState(GPUTextureFormat.RGBA8Unorm),
        ),
    ).use { pipeline ->
        createColorTarget(device, 16, 16).use { first ->
            createColorTarget(device, 16, 16).use { second ->
                val firstView = first.createView()
                val secondView = second.createView()
                try {
                    device.createCommandEncoder().use { encoder ->
                        val pass = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = firstView,
                                        loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                        storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                        clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                    ),
                                    RenderPassColorAttachment(
                                        view = secondView,
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
                } finally {
                    firstView.close()
                    secondView.close()
                }

                val firstPixels = readRgba8(device, first, 16, 16)
                val secondPixels = readRgba8(device, second, 16, 16)
                assertPixel(firstPixels, 16, 8, 8, 255, 0, 0, 255)
                assertPixel(firstPixels, 16, 2, 2, 255, 0, 0, 255)
                assertPixel(secondPixels, 16, 8, 8, 0, 255, 0, 255)
                assertPixel(secondPixels, 16, 2, 2, 0, 255, 0, 255)
            }
        }
    }
}
