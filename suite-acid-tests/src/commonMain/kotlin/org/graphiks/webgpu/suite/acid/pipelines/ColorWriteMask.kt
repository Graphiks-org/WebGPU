package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUColorWrite
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
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

private const val RED_ALPHA_ZERO_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,0); }
"""

/**
 * A `writeMask` of red writes only the red channel: over a green clear the result keeps the clear's
 * green, blue and alpha and takes only red from the fragment.
 */
@AcidTest(
    id = AcidCaseId.RenderColorWriteMask,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUColorTargetState_writeMask,
        ApiSymbols.GPUColorWrite_Red,
        ApiSymbols.GPURenderCommandsMixin_draw,
    ],
)
suspend fun colorWriteMask(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        RED_ALPHA_ZERO_SHADER,
        colorTargets = listOf(
            ColorTargetState(GPUTextureFormat.RGBA8Unorm, writeMask = GPUColorWrite.Red),
        ),
    ).use { pipeline ->
        createColorTarget(device, 16, 16).use { target ->
            val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 1.0, 0.0, 1.0)) { pass ->
                pass.setPipeline(pipeline)
                pass.draw(3u)
            }
            assertPixel(pixels, 16, 8, 8, 255, 255, 0, 255)
            assertPixel(pixels, 16, 4, 4, 255, 255, 0, 255)
        }
    }
}
