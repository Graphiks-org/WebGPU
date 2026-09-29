package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUIndexFormat
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

private const val INDEX_DRIVEN_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

/**
 * The index buffer is `[0,0,0,0,1,2]` and stays bound at offset 0. `drawIndexed(3, firstIndex = 3)`
 * reads indices 3..5, which are the full-screen triangle, so the centre is red. The same draw with
 * `firstIndex = 0` reads three zeros and draws nothing, leaving the blue clear. This isolates
 * `firstIndex` from `setIndexBuffer`'s own offset.
 */
@AcidTest(
    id = AcidCaseId.RenderFirstIndex,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPURenderCommandsMixin_setIndexBuffer,
        ApiSymbols.GPURenderCommandsMixin_drawIndexed,
        ApiSymbols.GPUIndexFormat_Uint16,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBufferUsage_Index,
    ],
)
suspend fun firstIndex(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(12uL, GPUBufferUsage.Index or GPUBufferUsage.CopyDst),
    ).use { indices ->
        device.queue.writeBuffer(
            indices,
            0uL,
            ArrayBuffer.of(ushortArrayOf(0u, 0u, 0u, 0u, 1u, 2u)),
        )
        createRenderPipeline(device, INDEX_DRIVEN_SHADER).use { pipeline ->
            val accepted = createColorTarget(device, 16, 16).use { target ->
                renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 1.0, 1.0)) { pass ->
                    pass.setPipeline(pipeline)
                    pass.setIndexBuffer(indices, GPUIndexFormat.Uint16)
                    pass.drawIndexed(3u, firstIndex = 3u)
                }
            }
            val control = createColorTarget(device, 16, 16).use { target ->
                renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 1.0, 1.0)) { pass ->
                    pass.setPipeline(pipeline)
                    pass.setIndexBuffer(indices, GPUIndexFormat.Uint16)
                    pass.drawIndexed(3u, firstIndex = 0u)
                }
            }
            assertPixel(accepted, 16, 8, 8, 255, 0, 0, 255)
            assertPixel(control, 16, 8, 8, 0, 0, 255, 255)
        }
    }
}
