package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.MultisampleState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

private fun coverageShader(alpha: Int) = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1.0, 0.0, 0.0, ${alpha}.0); }
"""

/**
 * With alpha-to-coverage enabled over an opaque blue clear, a red fragment with alpha 0 covers no
 * sample and resolves to the blue clear, while alpha 1 covers every sample and resolves to opaque
 * red. With alpha-to-coverage disabled the alpha-0 fragment is written as-is and resolves to red
 * with alpha 0. The extremes are asserted exactly, not intermediate thresholds or sample positions.
 */
@AcidTest(
    id = AcidCaseId.MsaaAlphaToCoverageExtremes,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUMultisampleState_alphaToCoverageEnabled,
        ApiSymbols.GPUMultisampleState_count,
        ApiSymbols.GPUTextureDescriptor_sampleCount,
        ApiSymbols.GPURenderPassColorAttachment_resolveTarget,
        ApiSymbols.GPUColorTargetState_blend,
    ],
)
suspend fun alphaToCoverageExtremes(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        coverageShader(0),
        multisample = MultisampleState(count = 4u, alphaToCoverageEnabled = true),
    ).use { alphaZeroCovered ->
        createRenderPipeline(
            device,
            coverageShader(1),
            multisample = MultisampleState(count = 4u, alphaToCoverageEnabled = true),
        ).use { alphaOneCovered ->
            createRenderPipeline(
                device,
                coverageShader(0),
                multisample = MultisampleState(count = 4u, alphaToCoverageEnabled = false),
            ).use { alphaZeroPlain ->
                val coveredZero = renderResolved(device, alphaZeroCovered)
                val coveredOne = renderResolved(device, alphaOneCovered)
                val plainZero = renderResolved(device, alphaZeroPlain)

                assertPixel(coveredZero, 16, 8, 8, 0, 0, 255, 255)
                assertPixel(coveredOne, 16, 8, 8, 255, 0, 0, 255)
                assertPixel(plainZero, 16, 8, 8, 255, 0, 0, 0)
            }
        }
    }
}

private suspend fun renderResolved(
    device: GPUDevice,
    pipeline: org.graphiks.webgpu.GPURenderPipeline,
): ByteArray {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(16u, 16u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment,
            sampleCount = 4u,
        ),
    ).use { msaa ->
        createColorTarget(device, 16, 16).use { resolved ->
            msaa.createView().use { msaaView ->
                resolved.createView().use { resolveView ->
                    device.createCommandEncoder().use { encoder ->
                        val pass = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = msaaView,
                                        loadOp = GPULoadOp.Clear,
                                        storeOp = GPUStoreOp.Store,
                                        clearValue = Color(0.0, 0.0, 1.0, 1.0),
                                        resolveTarget = resolveView,
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
            }
            return readRgba8(device, resolved, 16, 16)
        }
    }
}
