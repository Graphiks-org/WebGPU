package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
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
 * `setVertexBuffer` reads the vertex data starting at the byte offset it is given: the first three
 * vertices are degenerate sentinels, and only the triangle at offset 24 covers the red centre.
 */
@AcidTest(
    id = AcidCaseId.RenderVertexBufferOffset,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPURenderCommandsMixin_setVertexBuffer,
        ApiSymbols.GPURenderCommandsMixin_draw,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPUTexture_createView,
    ],
)
suspend fun vertexBufferOffset(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst)).use { buffer ->
        device.queue.writeBuffer(
            buffer,
            0uL,
            ArrayBuffer.of(
                floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, -1f, -1f, 3f, -1f, -1f, 3f),
            ),
        )
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
                    pass.setVertexBuffer(0u, buffer, offset = 24uL, size = 24uL)
                    pass.draw(3u)
                }
                assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                assertPixel(pixels, 16, 12, 12, 255, 0, 0, 255)
            }
        }
    }
}
