package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createDepthStencilTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderDepthAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val RED_FAR_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.75, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

private const val GREEN_NEAR_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.25, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0,1,0,1); }
"""

/**
 * With `Greater`, the far red fragment (z = 0.75) wins over the clear depth 0, so a later near green
 * fragment (z = 0.25) does not pass and the centre stays red.
 */
@AcidTest(
    id = AcidCaseId.DepthGreater,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUDepthStencilState_depthCompare,
        ApiSymbols.GPUCompareFunction_Greater,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun depthGreater(device: GPUDevice) = withValidationScope(device) {
    val depthState = DepthStencilState(
        format = GPUTextureFormat.Depth32Float,
        depthWriteEnabled = true,
        depthCompare = GPUCompareFunction.Greater,
    )
    createRenderPipeline(device, RED_FAR_SHADER, depthStencil = depthState).use { red ->
        createRenderPipeline(device, GREEN_NEAR_SHADER, depthStencil = depthState).use { green ->
            createColorTarget(device, 16, 16).use { target ->
                createDepthStencilTarget(device, 16, 16, GPUTextureFormat.Depth32Float).use { depth ->
                    val pixels = renderDepthAndRead(
                        device = device,
                        target = target,
                        width = 16,
                        height = 16,
                        colorClear = Color(0.0, 0.0, 0.0, 1.0),
                        depthTexture = depth,
                        depthClearValue = 0f,
                        depthLoadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                        depthStoreOp = org.graphiks.webgpu.GPUStoreOp.Store,
                    ) { pass ->
                        pass.setPipeline(red)
                        pass.draw(3u)
                        pass.setPipeline(green)
                        pass.draw(3u)
                    }
                    assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                }
            }
        }
    }
}
