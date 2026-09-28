package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUBlendFactor
import org.graphiks.webgpu.GPUBlendOperation
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.BlendComponent
import org.graphiks.webgpu.descriptors.BlendState
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import org.graphiks.webgpu.suite.acid.assertChannel
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val HALF_RED_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,0.5); }
"""

/**
 * Source-alpha blending composites a half-transparent red fragment over a blue clear: the colour
 * channels average to (0.5, 0, 0.5) and alpha reaches 1.
 */
@AcidTest(
    id = AcidCaseId.BlendSourceAlpha,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUColorTargetState_blend,
        ApiSymbols.GPUBlendComponent_srcFactor,
        ApiSymbols.GPUBlendFactor_SrcAlpha,
        ApiSymbols.GPUBlendFactor_OneMinusSrcAlpha,
        ApiSymbols.GPUBlendOperation_Add,
    ],
)
suspend fun alphaBlend(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        HALF_RED_SHADER,
        colorTargets = listOf(
            ColorTargetState(
                GPUTextureFormat.RGBA8Unorm,
                blend = BlendState(
                    color = BlendComponent(
                        operation = GPUBlendOperation.Add,
                        srcFactor = GPUBlendFactor.SrcAlpha,
                        dstFactor = GPUBlendFactor.OneMinusSrcAlpha,
                    ),
                    alpha = BlendComponent(
                        operation = GPUBlendOperation.Add,
                        srcFactor = GPUBlendFactor.One,
                        dstFactor = GPUBlendFactor.OneMinusSrcAlpha,
                    ),
                ),
            ),
        ),
    ).use { pipeline ->
        createColorTarget(device, 16, 16).use { target ->
            val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 1.0, 1.0)) { pass ->
                pass.setPipeline(pipeline)
                pass.draw(3u)
            }
            assertPixel(pixels, 16, 8, 8, 128, 0, 128, 255, tolerance = 1)
            assertChannel(pixels, 16, 8, 8, channel = 1, expected = 0, label = "G must stay 0")
            assertChannel(pixels, 16, 8, 8, channel = 3, expected = 255, label = "A must reach 255")
        }
    }
}
