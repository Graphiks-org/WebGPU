package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val INSTANCE_COLOR_SHADER = """
struct Out {
    @builtin(position) position: vec4f,
    @location(0) @interpolate(flat) instance: u32,
}

@vertex fn vertexMain(@builtin(vertex_index) i: u32, @builtin(instance_index) instance: u32) -> Out {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    var out: Out;
    out.position = vec4f(points[i], 0.5, 1.0);
    out.instance = instance;
    return out;
}

@fragment fn fragmentMain(@location(0) @interpolate(flat) instance: u32) -> @location(0) vec4f {
    if (instance == 1u) { return vec4f(0,1,0,1); }
    return vec4f(1,0,0,1);
}
"""

/**
 * With the `IndirectFirstInstance` feature, `drawIndirect` arguments `[3, 1, 0, 1]` set the base
 * instance to 1, so the shader sees `instance_index == 1` and paints green.
 *
 * Requires the optional `IndirectFirstInstance` feature; the runner reports it as `unsupported`
 * when the adapter lacks it.
 */
@AcidTest(
    id = AcidCaseId.RenderIndirectFirstInstance,
    family = AcidFamily.PipelinesRenderState,
    requiredFeatures = [GPUFeatureName.IndirectFirstInstance],
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBufferUsage_Indirect,
        ApiSymbols.GPURenderCommandsMixin_drawIndirect,
        ApiSymbols.GPUFeatureName_IndirectFirstInstance,
    ],
)
suspend fun indirectFirstInstance(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Indirect or GPUBufferUsage.CopyDst)).use { args ->
        device.queue.writeBuffer(args, 0uL, ArrayBuffer.of(uintArrayOf(3u, 1u, 0u, 1u)))
        createRenderPipeline(device, INSTANCE_COLOR_SHADER).use { pipeline ->
            createColorTarget(device, 16, 16).use { target ->
                val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                    pass.setPipeline(pipeline)
                    pass.drawIndirect(args, 0uL)
                }
                assertPixel(pixels, 16, 8, 8, 0, 255, 0, 255)
            }
        }
    }
}
