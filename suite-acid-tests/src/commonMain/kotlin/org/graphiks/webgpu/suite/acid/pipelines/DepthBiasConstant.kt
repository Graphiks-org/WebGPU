package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPURenderPipeline
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

private fun flatShader(r: Int, g: Int, b: Int) = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(${r}.0, ${g}.0, ${b}.0, 1.0); }
"""

/**
 * A green reference and a red overlay share the exact coplanar depth 0.5 and compare with `Less`. A
 * zero constant bias leaves them equal, so the overlay fails and the centre stays green. A large
 * negative bias pulls the overlay in front and turns it red; the same magnitude positive pushes it
 * behind and leaves green. Only the resulting colours are compared, never the biased depth bits.
 */
@AcidTest(
    id = AcidCaseId.DepthBiasConstant,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDepthStencilState_depthBias,
        ApiSymbols.GPUDepthStencilState_depthBiasSlopeScale,
        ApiSymbols.GPUDepthStencilState_depthBiasClamp,
        ApiSymbols.GPUCompareFunction_Less,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun depthBiasConstant(device: GPUDevice) = withValidationScope(device) {
    val format = GPUTextureFormat.Depth32Float
    val base = DepthStencilState(format, depthWriteEnabled = true, depthCompare = GPUCompareFunction.Less)

    createRenderPipeline(device, flatShader(0, 1, 0), depthStencil = base).use { reference ->
        createRenderPipeline(
            device,
            flatShader(1, 0, 0),
            depthStencil = base.copy(depthBias = 0, depthBiasSlopeScale = 0f, depthBiasClamp = 0f),
        ).use { overlayZero ->
            createRenderPipeline(
                device,
                flatShader(1, 0, 0),
                depthStencil = base.copy(depthBias = -1048576, depthBiasSlopeScale = 0f, depthBiasClamp = 0f),
            ).use { overlayNegative ->
                createRenderPipeline(
                    device,
                    flatShader(1, 0, 0),
                    depthStencil = base.copy(depthBias = 1048576, depthBiasSlopeScale = 0f, depthBiasClamp = 0f),
                ).use { overlayPositive ->
                    val zero = renderOverlay(device, format, reference, overlayZero)
                    val negative = renderOverlay(device, format, reference, overlayNegative)
                    val positive = renderOverlay(device, format, reference, overlayPositive)

                    assertPixel(zero, 16, 8, 8, 0, 255, 0, 255)
                    assertPixel(negative, 16, 8, 8, 255, 0, 0, 255)
                    assertPixel(positive, 16, 8, 8, 0, 255, 0, 255)
                }
            }
        }
    }
}

private suspend fun renderOverlay(
    device: GPUDevice,
    format: GPUTextureFormat,
    reference: GPURenderPipeline,
    overlay: GPURenderPipeline,
): ByteArray =
    createColorTarget(device, 16, 16).use { target ->
        createDepthStencilTarget(device, 16, 16, format).use { depth ->
            renderDepthAndRead(
                device = device,
                target = target,
                width = 16,
                height = 16,
                colorClear = Color(0.0, 0.0, 1.0, 1.0),
                depthTexture = depth,
                depthClearValue = 1f,
                depthLoadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                depthStoreOp = org.graphiks.webgpu.GPUStoreOp.Store,
            ) { pass ->
                pass.setPipeline(reference)
                pass.draw(3u)
                pass.setPipeline(overlay)
                pass.draw(3u)
            }
        }
    }
