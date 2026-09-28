package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUColorWrite
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUStencilOperation
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
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
 * A `Replace` limited by stencilWriteMask 0x0F turns a cleared 0xA0 into 0xA5, so a following `Equal`
 * against 0xA5 passes (green) while a comparison against 0x05 fails and keeps the green.
 */
@AcidTest(
    id = AcidCaseId.StencilWriteMask,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUDepthStencilState_stencilWriteMask,
        ApiSymbols.GPUStencilOperation_Replace,
        ApiSymbols.GPUStencilOperation_Keep,
    ],
)
suspend fun stencilWriteMask(device: GPUDevice) = withValidationScope(device) {
    val format = GPUTextureFormat.Depth24PlusStencil8
    createRenderPipeline(
        device,
        GREEN_SHADER,
        colorTargets = listOf(ColorTargetState(GPUTextureFormat.RGBA8Unorm, writeMask = GPUColorWrite.None)),
        depthStencil = DepthStencilState(
            format = format,
            depthWriteEnabled = false,
            depthCompare = GPUCompareFunction.Always,
            stencilFront = StencilFaceState(compare = GPUCompareFunction.Always, passOp = GPUStencilOperation.Replace),
            stencilBack = StencilFaceState(compare = GPUCompareFunction.Always, passOp = GPUStencilOperation.Replace),
            stencilReadMask = 0xFFFFFFFFu,
            stencilWriteMask = 0x0Fu,
        ),
    ).use { stamp ->
        val equal = { shader: String ->
            createRenderPipeline(
                device,
                shader,
                depthStencil = DepthStencilState(
                    format = format,
                    depthWriteEnabled = false,
                    depthCompare = GPUCompareFunction.Always,
                    stencilFront = StencilFaceState(compare = GPUCompareFunction.Equal, passOp = GPUStencilOperation.Keep),
                    stencilBack = StencilFaceState(compare = GPUCompareFunction.Equal, passOp = GPUStencilOperation.Keep),
                    stencilReadMask = 0xFFFFFFFFu,
                    stencilWriteMask = 0xFFFFFFFFu,
                ),
            )
        }
        equal(GREEN_SHADER).use { green ->
            equal(RED_SHADER).use { red ->
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
                            stencilClearValue = 0xA0u,
                            stencilLoadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                            stencilStoreOp = org.graphiks.webgpu.GPUStoreOp.Store,
                        ) { pass ->
                            pass.setPipeline(stamp)
                            pass.setStencilReference(0x05u)
                            pass.draw(3u)

                            pass.setPipeline(green)
                            pass.setStencilReference(0xA5u)
                            pass.draw(3u)

                            pass.setPipeline(red)
                            pass.setStencilReference(0x05u)
                            pass.draw(3u)
                        }
                        assertPixel(pixels, 16, 8, 8, 0, 255, 0, 255)
                    }
                }
            }
        }
    }
}
