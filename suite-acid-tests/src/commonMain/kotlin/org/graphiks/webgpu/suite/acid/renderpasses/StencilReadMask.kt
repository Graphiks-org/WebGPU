package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUStencilOperation
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.descriptors.StencilFaceState
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

private const val GREEN_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0,1,0,1); }
"""

private const val RED_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

/**
 * The stencil read mask limits the comparison bits: with a cleared stencil 0xA5 and read mask 0x0F,
 * reference 0x15 matches (green) while reference 0x16 does not, so the red draw is rejected.
 */
@AcidTest(
    id = AcidCaseId.StencilReadMask,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUDepthStencilState_stencilReadMask,
        ApiSymbols.GPUCompareFunction_Equal,
        ApiSymbols.GPURenderPassEncoder_setStencilReference,
    ],
)
suspend fun stencilReadMask(device: GPUDevice) = withValidationScope(device) {
    val format = GPUTextureFormat.Depth24PlusStencil8
    val masked = { compare: GPUCompareFunction ->
        DepthStencilState(
            format = format,
            depthWriteEnabled = false,
            depthCompare = GPUCompareFunction.Always,
            stencilFront = StencilFaceState(compare = compare, passOp = GPUStencilOperation.Keep),
            stencilBack = StencilFaceState(compare = compare, passOp = GPUStencilOperation.Keep),
            stencilReadMask = 0x0Fu,
            stencilWriteMask = 0xFFFFFFFFu,
        )
    }
    createRenderPipeline(device, GREEN_SHADER, depthStencil = masked(GPUCompareFunction.Equal)).use { green ->
        createRenderPipeline(device, RED_SHADER, depthStencil = masked(GPUCompareFunction.Equal)).use { red ->
            createColorTarget(device, 16, 16).use { target ->
                createDepthStencilTarget(device, 16, 16, format).use { depthStencil ->
                    val pixels = renderDepthAndRead(
                        device = device,
                        target = target,
                        width = 16,
                        height = 16,
                        colorClear = Color(0.0, 0.0, 0.0, 1.0),
                        depthTexture = depthStencil,
                        depthClearValue = 1f,
                        depthLoadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                        depthStoreOp = org.graphiks.webgpu.GPUStoreOp.Store,
                        stencilClearValue = 0xA5u,
                        stencilLoadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                        stencilStoreOp = org.graphiks.webgpu.GPUStoreOp.Store,
                    ) { pass ->
                        pass.setPipeline(green)
                        pass.setStencilReference(0x15u)
                        pass.draw(3u)

                        pass.setPipeline(red)
                        pass.setStencilReference(0x16u)
                        pass.draw(3u)
                    }
                    assertPixel(pixels, 16, 8, 8, 0, 255, 0, 255)
                }
            }
        }
    }
}
