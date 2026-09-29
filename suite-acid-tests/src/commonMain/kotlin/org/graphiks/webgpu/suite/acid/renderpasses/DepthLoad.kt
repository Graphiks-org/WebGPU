package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
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

private const val RED_NEAR_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.25, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

private const val GREEN_NEAR_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.125, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0,1,0,1); }
"""

private const val BLUE_FAR_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.75, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0,0,1,1); }
"""

/**
 * A `load` depth pass preserves the depth a prior store pass wrote: pass 1 writes depth 0.25 and
 * stores it. In pass 2, a near green fragment at depth 0.125 passes `Less` (left half) while a far
 * blue fragment at depth 0.75 fails and keeps the red colour (right half). Testing both sides
 * distinguishes the preserved depth 0.25 from a reset to zero (which would reject the near
 * fragment) or to one (which would admit the far fragment).
 */
@AcidTest(
    id = AcidCaseId.DepthLoadPreserves,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPURenderPassDepthStencilAttachment,
        ApiSymbols.GPULoadOp_Load,
        ApiSymbols.GPUCompareFunction_Less,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun depthLoadPreserves(device: GPUDevice) = withValidationScope(device) {
    val depthState = DepthStencilState(
        format = GPUTextureFormat.Depth32Float,
        depthWriteEnabled = true,
        depthCompare = GPUCompareFunction.Less,
    )
    createRenderPipeline(device, RED_NEAR_SHADER, depthStencil = depthState).use { red ->
        createRenderPipeline(device, GREEN_NEAR_SHADER, depthStencil = depthState).use { green ->
            createRenderPipeline(device, BLUE_FAR_SHADER, depthStencil = depthState).use { blue ->
                createColorTarget(device, 16, 16).use { target ->
                    createDepthStencilTarget(device, 16, 16, GPUTextureFormat.Depth32Float).use { depth ->
                        target.createView().use { colorView ->
                            depth.createView().use { depthView ->
                                device.createCommandEncoder().use { encoder ->
                                    val first = encoder.beginRenderPass(
                                        RenderPassDescriptor(
                                            colorAttachments = listOf(
                                                RenderPassColorAttachment(
                                                    view = colorView,
                                                    loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                                    storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                                    clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                                ),
                                            ),
                                            depthStencilAttachment = RenderPassDepthStencilAttachment(
                                                view = depthView,
                                                depthClearValue = 1f,
                                                depthLoadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                                depthStoreOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                            ),
                                        ),
                                    )
                                    first.setPipeline(red)
                                    first.draw(3u)
                                    first.end()

                                    val second = encoder.beginRenderPass(
                                        RenderPassDescriptor(
                                            colorAttachments = listOf(
                                                RenderPassColorAttachment(
                                                    view = colorView,
                                                    loadOp = org.graphiks.webgpu.GPULoadOp.Load,
                                                    storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                                ),
                                            ),
                                            depthStencilAttachment = RenderPassDepthStencilAttachment(
                                                view = depthView,
                                                depthLoadOp = org.graphiks.webgpu.GPULoadOp.Load,
                                                depthStoreOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                            ),
                                        ),
                                    )
                                    second.setScissorRect(0u, 0u, 8u, 16u)
                                    second.setPipeline(green)
                                    second.draw(3u)
                                    second.setScissorRect(8u, 0u, 8u, 16u)
                                    second.setPipeline(blue)
                                    second.draw(3u)
                                    second.end()
                                    encoder.finish().use { device.queue.submit(listOf(it)) }
                                }
                            }
                        }

                        val pixels = readRgba8(device, target, 16, 16)
                        assertPixel(pixels, 16, 4, 8, 0, 255, 0, 255)
                        assertPixel(pixels, 16, 12, 8, 255, 0, 0, 255)
                    }
                }
            }
        }
    }
}
