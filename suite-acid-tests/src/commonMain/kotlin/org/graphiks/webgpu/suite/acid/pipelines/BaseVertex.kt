package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.VertexAttribute
import org.graphiks.webgpu.descriptors.VertexBufferLayout
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val POSITION_SHADER = """
@vertex fn vertexMain(@location(0) position: vec2f) -> @builtin(position) vec4f {
    return vec4f(position, 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

/**
 * `drawIndexed(baseVertex = 3)` offsets every index by three vertices, so indices 0, 1, 2 select the
 * fullscreen triangle after the sentinels.
 */
@AcidTest(
    id = AcidCaseId.RenderBaseVertex,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPURenderCommandsMixin_setIndexBuffer,
        ApiSymbols.GPURenderCommandsMixin_drawIndexed,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun baseVertex(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst)).use { vertices ->
        device.queue.writeBuffer(
            vertices,
            0uL,
            ArrayBuffer.of(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, -1f, -1f, 3f, -1f, -1f, 3f)),
        )
        device.createBuffer(BufferDescriptor(8uL, GPUBufferUsage.Index or GPUBufferUsage.CopyDst)).use { indices ->
            device.queue.writeBuffer(indices, 0uL, ArrayBuffer.of(ushortArrayOf(0u, 1u, 2u, 0u)))
            createRenderPipeline(
                device,
                POSITION_SHADER,
                vertexLayouts = listOf(
                    VertexBufferLayout(8uL, listOf(VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 0u))),
                ),
            ).use { pipeline ->
                createColorTarget(device, 16, 16).use { target ->
                    val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                        pass.setPipeline(pipeline)
                        pass.setVertexBuffer(0u, vertices)
                        pass.setIndexBuffer(indices, GPUIndexFormat.Uint16)
                        pass.drawIndexed(3u, baseVertex = 3)
                    }
                    assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                    assertPixel(pixels, 16, 4, 4, 255, 0, 0, 255)
                }
            }
        }
    }
}
