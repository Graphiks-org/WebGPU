package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.Color
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
import kotlin.test.assertIs

private fun flatShader(r: Int, g: Int, b: Int) = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(${r}.0, ${g}.0, ${b}.0, 1.0); }
"""

/**
 * A pass whose `maxDrawCount` is 2 accepts the same two draws and the second (green) is the final
 * colour. The identical commands under `maxDrawCount = 1` exceed the cap and report a validation
 * error, so the limit is exercised with draws rather than with a bare descriptor.
 */
@AcidTest(
    id = AcidCaseId.RenderMaxDrawCount,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPURenderPassDescriptor_maxDrawCount,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUValidationError,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderCommandsMixin_draw,
    ],
)
suspend fun maxDrawCount(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(device, flatShader(1, 0, 0)).use { red ->
        createRenderPipeline(device, flatShader(0, 1, 0)).use { green ->
            createColorTarget(device, 16, 16).use { target ->
                target.createView().use { view ->
                    device.createCommandEncoder().use { encoder ->
                        val pass = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(colorAttachment(view)),
                                maxDrawCount = 2uL,
                            ),
                        )
                        pass.setPipeline(red)
                        pass.draw(3u)
                        pass.setPipeline(green)
                        pass.draw(3u)
                        pass.end()
                        encoder.finish().use { device.queue.submit(listOf(it)) }
                    }
                }
                val pixels = readRgba8(device, target, 16, 16)
                assertPixel(pixels, 16, 8, 8, 0, 255, 0, 255)
            }

            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                createColorTarget(device, 16, 16).use { target ->
                    target.createView().use { view ->
                        device.createCommandEncoder().use { encoder ->
                            val pass = encoder.beginRenderPass(
                                RenderPassDescriptor(
                                    colorAttachments = listOf(colorAttachment(view)),
                                    maxDrawCount = 1uL,
                                ),
                            )
                            pass.setPipeline(red)
                            pass.draw(3u)
                            pass.setPipeline(green)
                            pass.draw(3u)
                            pass.end()
                            runCatching { encoder.finish() }.getOrNull()?.use { device.queue.submit(listOf(it)) }
                        }
                    }
                }
            } finally {
                assertIs<GPUValidationError>(
                    device.popErrorScope().getOrThrow(),
                    "A pass that draws more times than maxDrawCount must report a validation error",
                )
            }
        }
    }
}

private fun colorAttachment(view: org.graphiks.webgpu.GPUTextureView) = RenderPassColorAttachment(
    view = view,
    loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
    storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
    clearValue = Color(0.0, 0.0, 0.0, 1.0),
)
