package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.GPUVertexStepMode
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

private const val INSTANCED_SHADER = """
struct VertexOut {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
}

@vertex fn vertexMain(
    @location(0) position: vec2f,
    @location(1) offset: vec2f,
    @location(2) color: vec4f,
) -> VertexOut {
    var out: VertexOut;
    out.position = vec4f(position + offset, 0.5, 1.0);
    out.color = color;
    return out;
}

@fragment fn fragmentMain(@location(0) color: vec4f) -> @location(0) vec4f {
    return color;
}
"""

/**
 * An `Instance` step-mode buffer supplies a per-instance offset and colour: the same quad is drawn
 * once on the left in red and once on the right in green, over the black clear.
 */
@AcidTest(
    id = AcidCaseId.RenderInstancedAttributes,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPURenderCommandsMixin_setVertexBuffer,
        ApiSymbols.GPURenderCommandsMixin_draw,
        ApiSymbols.GPUVertexStepMode_Instance,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun instancedAttributes(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst)).use { quad ->
        device.queue.writeBuffer(
            quad,
            0uL,
            ArrayBuffer.of(
                floatArrayOf(
                    -0.25f, -0.25f, 0.25f, -0.25f, 0.25f, 0.25f,
                    -0.25f, -0.25f, 0.25f, 0.25f, -0.25f, 0.25f,
                ),
            ),
        )
        device.createBuffer(BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst)).use { instances ->
            device.queue.writeBuffer(
                instances,
                0uL,
                ArrayBuffer.of(
                    floatArrayOf(
                        -0.5f, 0f, 1f, 0f, 0f, 1f,
                        0.5f, 0f, 0f, 1f, 0f, 1f,
                    ),
                ),
            )
            createRenderPipeline(
                device,
                INSTANCED_SHADER,
                vertexLayouts = listOf(
                    VertexBufferLayout(
                        8uL,
                        listOf(VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 0u)),
                        GPUVertexStepMode.Vertex,
                    ),
                    VertexBufferLayout(
                        24uL,
                        listOf(
                            VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 1u),
                            VertexAttribute(GPUVertexFormat.Float32x4, 8uL, 2u),
                        ),
                        GPUVertexStepMode.Instance,
                    ),
                ),
            ).use { pipeline ->
                createColorTarget(device, 16, 16).use { target ->
                    val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                        pass.setPipeline(pipeline)
                        pass.setVertexBuffer(0u, quad)
                        pass.setVertexBuffer(1u, instances)
                        pass.draw(6u, instanceCount = 2u)
                    }
                    assertPixel(pixels, 16, 4, 8, 255, 0, 0, 255)
                    assertPixel(pixels, 16, 12, 8, 0, 255, 0, 255)
                    assertPixel(pixels, 16, 8, 1, 0, 0, 0, 255)
                }
            }
        }
    }
}
