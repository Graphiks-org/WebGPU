package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUCullMode
import org.graphiks.webgpu.GPUFrontFace
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.PrimitiveState
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val CCW_TRIANGLE_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-0.5,-0.5), vec2f(0.5,-0.5), vec2f(0.0,0.5));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

/**
 * One clip-space triangle is drawn over a blue clear under three primitive states. With the WebGPU
 * face convention, `frontFace = CCW` and `cullMode = Back` keep it, so the centre is red; flipping
 * the declared front face to CW, or culling the front face, leaves the blue clear. The centre pixel
 * is strictly interior, so no edge rasterisation rule is involved.
 */
@AcidTest(
    id = AcidCaseId.RenderFrontFaceCulling,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUPrimitiveState,
        ApiSymbols.GPUPrimitiveState_frontFace,
        ApiSymbols.GPUPrimitiveState_cullMode,
        ApiSymbols.GPUFrontFace_CCW,
        ApiSymbols.GPUFrontFace_CW,
        ApiSymbols.GPUCullMode_Back,
        ApiSymbols.GPUCullMode_Front,
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPURenderCommandsMixin_draw,
    ],
)
suspend fun frontFaceCulling(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        CCW_TRIANGLE_SHADER,
        primitive = PrimitiveState(frontFace = GPUFrontFace.CCW, cullMode = GPUCullMode.Back),
    ).use { ccwBack ->
        createRenderPipeline(
            device,
            CCW_TRIANGLE_SHADER,
            primitive = PrimitiveState(frontFace = GPUFrontFace.CW, cullMode = GPUCullMode.Back),
        ).use { cwBack ->
            createRenderPipeline(
                device,
                CCW_TRIANGLE_SHADER,
                primitive = PrimitiveState(frontFace = GPUFrontFace.CCW, cullMode = GPUCullMode.Front),
            ).use { ccwFront ->
                val kept = renderTriangle(device, ccwBack)
                val wrongFrontFace = renderTriangle(device, cwBack)
                val wrongCullMode = renderTriangle(device, ccwFront)

                assertPixel(kept, 16, 8, 8, 255, 0, 0, 255)
                assertPixel(wrongFrontFace, 16, 8, 8, 0, 0, 255, 255)
                assertPixel(wrongCullMode, 16, 8, 8, 0, 0, 255, 255)
            }
        }
    }
}

private suspend fun renderTriangle(device: GPUDevice, pipeline: GPURenderPipeline): ByteArray {
    createColorTarget(device, 16, 16).use { target: GPUTexture ->
        return renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 1.0, 1.0)) { pass ->
            pass.setPipeline(pipeline)
            pass.draw(3u)
        }
    }
}
