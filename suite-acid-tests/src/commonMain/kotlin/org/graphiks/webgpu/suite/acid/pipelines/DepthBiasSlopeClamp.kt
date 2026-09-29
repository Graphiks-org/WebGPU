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

private fun slopeShader(offset: String, r: Int, g: Int, b: Int) = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    let p = points[i];
    return vec4f(p, 0.5 + 0.25 * p.x - $offset, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(${r}.0, ${g}.0, ${b}.0, 1.0); }
"""

/**
 * A reference plane `z = 0.5 + 0.25 * x` writes green; an overlay at the same slope but 0.0625
 * nearer is red. With no slope bias the nearer overlay passes (`Less`) and the centre is red. A
 * slope-scale of 4 over the 16-pixel viewport gives a 0.125 bias, which pushes the overlay behind
 * the reference and leaves green. Clamping that bias to 0.015625 restores the red overlay. The
 * constant bias is 0 here so only slope scale and clamp are exercised.
 */
@AcidTest(
    id = AcidCaseId.DepthBiasSlopeClamp,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDepthStencilState_depthBias,
        ApiSymbols.GPUDepthStencilState_depthBiasSlopeScale,
        ApiSymbols.GPUDepthStencilState_depthBiasClamp,
        ApiSymbols.GPUCompareFunction_Less,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun depthBiasSlopeClamp(device: GPUDevice) = withValidationScope(device) {
    val format = GPUTextureFormat.Depth32Float
    val base = DepthStencilState(format, depthWriteEnabled = true, depthCompare = GPUCompareFunction.Less)

    createRenderPipeline(device, slopeShader("0.0", 0, 1, 0), depthStencil = base).use { reference ->
        createRenderPipeline(
            device,
            slopeShader("0.0625", 1, 0, 0),
            depthStencil = base.copy(depthBiasSlopeScale = 0f, depthBiasClamp = 0f),
        ).use { overlayNoBias ->
            createRenderPipeline(
                device,
                slopeShader("0.0625", 1, 0, 0),
                depthStencil = base.copy(depthBiasSlopeScale = 4f, depthBiasClamp = 0f),
            ).use { overlaySlopeOnly ->
                createRenderPipeline(
                    device,
                    slopeShader("0.0625", 1, 0, 0),
                    depthStencil = base.copy(depthBiasSlopeScale = 4f, depthBiasClamp = 0.015625f),
                ).use { overlayClamped ->
                    val noBias = renderWithOverlay(device, format, reference, overlayNoBias)
                    val slopeOnly = renderWithOverlay(device, format, reference, overlaySlopeOnly)
                    val clamped = renderWithOverlay(device, format, reference, overlayClamped)

                    assertPixel(noBias, 16, 8, 8, 255, 0, 0, 255)
                    assertPixel(slopeOnly, 16, 8, 8, 0, 255, 0, 255)
                    assertPixel(clamped, 16, 8, 8, 255, 0, 0, 255)
                }
            }
        }
    }
}

private suspend fun renderWithOverlay(
    device: GPUDevice,
    format: GPUTextureFormat,
    reference: org.graphiks.webgpu.GPURenderPipeline,
    overlay: org.graphiks.webgpu.GPURenderPipeline,
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
