package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUColorWrite
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUCullMode
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUStencilOperation
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.descriptors.PrimitiveState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.StencilFaceState
import org.graphiks.webgpu.descriptors.VertexAttribute
import org.graphiks.webgpu.descriptors.VertexBufferLayout
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createDepthStencilTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val POSITION_SHADER = """
@vertex fn vertexMain(@location(0) position: vec2f) -> @builtin(position) vec4f {
    return vec4f(position, 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

private const val GREEN_POSITION_SHADER = """
@vertex fn vertexMain(@location(0) position: vec2f) -> @builtin(position) vec4f {
    return vec4f(position, 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0,1,0,1); }
"""

/**
 * Two separated triangles with opposite winding are recorded with `Replace` on the front face
 * (reference 3) and `IncrementClamp` on the back face (0 becomes 1); the colour write is masked off.
 * Two later passes compare `Equal`: reference 3 paints only the front-facing triangle red, reference
 * 1 only the back-facing triangle green, over a blue clear.
 */
@AcidTest(
    id = AcidCaseId.StencilFrontBackOperations,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUStencilFaceState,
        ApiSymbols.GPUStencilFaceState_compare,
        ApiSymbols.GPUStencilFaceState_passOp,
        ApiSymbols.GPUCompareFunction_Always,
        ApiSymbols.GPUCompareFunction_Equal,
        ApiSymbols.GPUStencilOperation_Replace,
        ApiSymbols.GPUStencilOperation_IncrementClamp,
        ApiSymbols.GPUDepthStencilState_stencilFront,
        ApiSymbols.GPUDepthStencilState_stencilBack,
        ApiSymbols.GPUColorTargetState_writeMask,
        ApiSymbols.GPUColorWrite_None,
        ApiSymbols.GPURenderPassEncoder_setStencilReference,
    ],
)
suspend fun stencilFrontBack(device: GPUDevice) = withValidationScope(device) {
    val format = GPUTextureFormat.Depth24PlusStencil8
    device.createBuffer(
        BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst),
    ).use { vertices ->
        device.queue.writeBuffer(
            vertices,
            0uL,
            ArrayBuffer.of(
                floatArrayOf(
                    // Left triangle, counter-clockwise in clip space.
                    -0.75f, -0.5f, -0.25f, -0.5f, -0.5f, 0.5f,
                    // Right triangle, the same shape wound the other way.
                    0.5f, 0.5f, 0.75f, -0.5f, 0.25f, -0.5f,
                ),
            ),
        )
        val layout = listOf(
            VertexBufferLayout(8uL, listOf(VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 0u))),
        )
        val always = StencilFaceState(compare = GPUCompareFunction.Always)
        val setupStencil = DepthStencilState(
            format = format,
            depthWriteEnabled = false,
            depthCompare = GPUCompareFunction.Always,
            stencilFront = always.copy(passOp = GPUStencilOperation.Replace),
            stencilBack = always.copy(passOp = GPUStencilOperation.IncrementClamp),
            stencilReadMask = 0xFFu,
            stencilWriteMask = 0xFFu,
        )
        val equalStencil = DepthStencilState(
            format = format,
            depthWriteEnabled = false,
            depthCompare = GPUCompareFunction.Always,
            stencilFront = always.copy(compare = GPUCompareFunction.Equal),
            stencilBack = always.copy(compare = GPUCompareFunction.Equal),
            stencilReadMask = 0xFFu,
            stencilWriteMask = 0xFFu,
        )

        createRenderPipeline(
            device,
            POSITION_SHADER,
            vertexLayouts = layout,
            colorTargets = listOf(ColorTargetState(GPUTextureFormat.RGBA8Unorm, writeMask = GPUColorWrite.None)),
            primitive = PrimitiveState(cullMode = GPUCullMode.None),
            depthStencil = setupStencil,
        ).use { setup ->
            createRenderPipeline(
                device,
                POSITION_SHADER,
                vertexLayouts = layout,
                primitive = PrimitiveState(cullMode = GPUCullMode.None),
                depthStencil = equalStencil,
            ).use { paintRed ->
                createRenderPipeline(
                    device,
                    GREEN_POSITION_SHADER,
                    vertexLayouts = layout,
                    primitive = PrimitiveState(cullMode = GPUCullMode.None),
                    depthStencil = equalStencil,
                ).use { paintGreen ->
                createColorTarget(device, 16, 16).use { frontTarget ->
                    createColorTarget(device, 16, 16).use { backTarget ->
                        createDepthStencilTarget(device, 16, 16, format).use { depth ->
                            depth.createView().use { depthView ->
                                // Pass 1: write the stencil values (masked colour).
                                frontTarget.createView().use { view ->
                                    device.createCommandEncoder().use { encoder ->
                                        val setupPass = encoder.beginRenderPass(
                                            RenderPassDescriptor(
                                                colorAttachments = listOf(colorAttachment(view, GPULoadOp.Clear)),
                                                depthStencilAttachment = RenderPassDepthStencilAttachment(
                                                    view = depthView,
                                                    depthClearValue = 1f,
                                                    depthLoadOp = GPULoadOp.Clear,
                                                    depthStoreOp = GPUStoreOp.Store,
                                                    stencilClearValue = 0u,
                                                    stencilLoadOp = GPULoadOp.Clear,
                                                    stencilStoreOp = GPUStoreOp.Store,
                                                ),
                                            ),
                                        )
                                        setupPass.setStencilReference(3u)
                                        setupPass.setPipeline(setup)
                                        setupPass.setVertexBuffer(0u, vertices)
                                        setupPass.draw(6u)
                                        setupPass.end()
                                        encoder.finish().use { device.queue.submit(listOf(it)) }
                                    }
                                }

                                // Pass 2: reference 3 paints the front-facing triangle red.
                                paintStencil(
                                    device, frontTarget, depthView, paintRed, vertices, reference = 3u,
                                )
                                // Pass 3: reference 1 paints the back-facing triangle green.
                                paintStencil(
                                    device, backTarget, depthView, paintGreen, vertices, reference = 1u,
                                )
                            }

                            val frontPixels = readRgba8(device, frontTarget, 16, 16)
                            val backPixels = readRgba8(device, backTarget, 16, 16)
                            assertPixel(frontPixels, 16, 4, 10, 255, 0, 0, 255)
                            assertPixel(frontPixels, 16, 12, 10, 0, 0, 255, 255)
                            assertPixel(backPixels, 16, 12, 10, 0, 255, 0, 255)
                            assertPixel(backPixels, 16, 4, 10, 0, 0, 255, 255)
                        }
                    }
                }
            }
        }
    }
    }
}

private suspend fun paintStencil(
    device: GPUDevice,
    target: org.graphiks.webgpu.GPUTexture,
    depthView: org.graphiks.webgpu.GPUTextureView,
    pipeline: org.graphiks.webgpu.GPURenderPipeline,
    vertices: org.graphiks.webgpu.GPUBuffer,
    reference: UInt,
) {
    target.createView().use { view ->
        device.createCommandEncoder().use { encoder ->
            val pass = encoder.beginRenderPass(
                RenderPassDescriptor(
                    colorAttachments = listOf(colorAttachment(view, GPULoadOp.Clear)),
                    depthStencilAttachment = RenderPassDepthStencilAttachment(
                        view = depthView,
                        depthReadOnly = true,
                        stencilLoadOp = GPULoadOp.Load,
                        stencilStoreOp = GPUStoreOp.Store,
                    ),
                ),
            )
            pass.setStencilReference(reference)
            pass.setPipeline(pipeline)
            pass.setVertexBuffer(0u, vertices)
            pass.draw(6u)
            pass.end()
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
    }
}

private fun colorAttachment(
    view: org.graphiks.webgpu.GPUTextureView,
    loadOp: GPULoadOp,
) = RenderPassColorAttachment(
    view = view,
    loadOp = loadOp,
    storeOp = GPUStoreOp.Store,
    clearValue = Color(0.0, 0.0, 1.0, 1.0),
)
