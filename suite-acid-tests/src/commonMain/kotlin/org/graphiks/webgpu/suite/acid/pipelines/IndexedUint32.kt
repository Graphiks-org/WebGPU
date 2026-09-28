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
 * A 32-bit index buffer read from `offset = 12` skips three degenerate indices and draws the same
 * centred square as the 16-bit case.
 */
@AcidTest(
    id = AcidCaseId.RenderIndexedU32Offset,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPURenderCommandsMixin_setIndexBuffer,
        ApiSymbols.GPURenderCommandsMixin_drawIndexed,
        ApiSymbols.GPUIndexFormat,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun indexedUint32(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(32uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst)).use { vertices ->
        device.queue.writeBuffer(
            vertices,
            0uL,
            ArrayBuffer.of(floatArrayOf(-0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)),
        )
        device.createBuffer(BufferDescriptor(36uL, GPUBufferUsage.Index or GPUBufferUsage.CopyDst)).use { indices ->
            device.queue.writeBuffer(
                indices,
                0uL,
                ArrayBuffer.of(uintArrayOf(0u, 0u, 0u, 0u, 1u, 2u, 2u, 1u, 3u)),
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
                        pass.setVertexBuffer(0u, vertices)
                        pass.setIndexBuffer(indices, GPUIndexFormat.Uint32, offset = 12uL, size = 24uL)
                        pass.drawIndexed(6u)
                    }
                    assertPixel(pixels, 16, 6, 6, 255, 0, 0, 255)
                    assertPixel(pixels, 16, 9, 9, 255, 0, 0, 255)
                    assertPixel(pixels, 16, 0, 0, 0, 0, 0, 255)
                    assertPixel(pixels, 16, 15, 15, 0, 0, 0, 255)
                }
            }
        }
    }
}
