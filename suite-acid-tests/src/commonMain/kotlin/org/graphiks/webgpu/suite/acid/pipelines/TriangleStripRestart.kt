package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCullMode
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPUPrimitiveTopology
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.PrimitiveState
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
 * One indexed triangle strip holds two separated rectangles; the `0xffff` index between them is the
 * `Uint16` primitive restart, so no triangle bridges the gap. Both rectangle centres are red and the
 * gap stays black. Without the restart, the strip would draw a connecting triangle across the gap.
 *
 * The nine `u16` indices need eighteen bytes; an `Index`/`mappedAtCreation` buffer rounds that to
 * twenty.
 */
@AcidTest(
    id = AcidCaseId.RenderTriangleStripRestart,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUPrimitiveState_topology,
        ApiSymbols.GPUPrimitiveState_stripIndexFormat,
        ApiSymbols.GPUPrimitiveTopology_TriangleStrip,
        ApiSymbols.GPUIndexFormat_Uint16,
        ApiSymbols.GPUCullMode_None,
        ApiSymbols.GPURenderCommandsMixin_drawIndexed,
        ApiSymbols.GPURenderCommandsMixin_setIndexBuffer,
    ],
)
suspend fun triangleStripRestart(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(64uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst),
    ).use { vertices ->
        device.queue.writeBuffer(
            vertices,
            0uL,
            ArrayBuffer.of(
                floatArrayOf(
                    -0.875f, -0.75f, -0.875f, 0.75f, -0.25f, -0.75f, -0.25f, 0.75f,
                    0.25f, -0.75f, 0.25f, 0.75f, 0.875f, -0.75f, 0.875f, 0.75f,
                ),
            ),
        )
        device.createBuffer(
            BufferDescriptor(20uL, GPUBufferUsage.Index or GPUBufferUsage.CopyDst, mappedAtCreation = true),
        ).use { indices ->
            indices.getMappedRange().setShorts(
                0uL,
                // 0xFFFF (the Uint16 restart index) is -1 as a signed Short.
                shortArrayOf(0, 1, 2, 3, -1, 4, 5, 6, 7),
            )
            indices.unmap()

            createRenderPipeline(
                device,
                POSITION_SHADER,
                vertexLayouts = listOf(
                    VertexBufferLayout(8uL, listOf(VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 0u))),
                ),
                primitive = PrimitiveState(
                    topology = GPUPrimitiveTopology.TriangleStrip,
                    stripIndexFormat = GPUIndexFormat.Uint16,
                    cullMode = GPUCullMode.None,
                ),
            ).use { pipeline ->
                createColorTarget(device, 16, 16).use { target ->
                    val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                        pass.setPipeline(pipeline)
                        pass.setVertexBuffer(0u, vertices)
                        pass.setIndexBuffer(indices, GPUIndexFormat.Uint16)
                        pass.drawIndexed(9u)
                    }
                    assertPixel(pixels, 16, 4, 8, 255, 0, 0, 255)
                    assertPixel(pixels, 16, 12, 8, 255, 0, 0, 255)
                    assertPixel(pixels, 16, 7, 10, 0, 0, 0, 255)
                }
            }
        }
    }
}
