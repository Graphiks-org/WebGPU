package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
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

private fun fullscreenZShader(z: String, r: Int, g: Int, b: Int) = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], $z, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(${r}.0, ${g}.0, ${b}.0, 1.0); }
"""

/**
 * Pass 1 writes depth 0.25 and red. Pass 2 is `depthReadOnly = true` with no depth load/store: its
 * green z = 0.75 fails `Less` and its blue z = 0.1 passes, but no depth is written. Pass 3 loads the
 * depth and draws green z = 0.2, which passes only because the stored depth is still 0.25; had pass
 * 2 written 0.1, the centre would stay blue.
 */
@AcidTest(
    id = AcidCaseId.DepthReadOnlyAttachment,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPURenderPassDepthStencilAttachment_depthReadOnly,
        ApiSymbols.GPUDepthStencilState_depthWriteEnabled,
        ApiSymbols.GPUDepthStencilState_depthCompare,
        ApiSymbols.GPUCompareFunction_Less,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun depthReadOnly(device: GPUDevice) = withValidationScope(device) {
    val format = GPUTextureFormat.Depth32Float
    val writesDepth = DepthStencilState(format, depthWriteEnabled = true, depthCompare = GPUCompareFunction.Less)
    val testsOnly = DepthStencilState(format, depthWriteEnabled = false, depthCompare = GPUCompareFunction.Less)

    createRenderPipeline(device, fullscreenZShader("0.25", 1, 0, 0), depthStencil = writesDepth).use { red025 ->
        createRenderPipeline(device, fullscreenZShader("0.75", 0, 1, 0), depthStencil = testsOnly).use { green075 ->
            createRenderPipeline(device, fullscreenZShader("0.1", 0, 0, 1), depthStencil = testsOnly).use { blue010 ->
                createRenderPipeline(device, fullscreenZShader("0.2", 0, 1, 0), depthStencil = testsOnly).use { green020 ->
                    createColorTarget(device, 16, 16).use { target ->
                        createDepthStencilTarget(device, 16, 16, format).use { depth ->
                            target.createView().use { colorView ->
                                depth.createView().use { depthView ->
                                    device.createCommandEncoder().use { encoder ->
                                        val first = encoder.beginRenderPass(
                                            RenderPassDescriptor(
                                                colorAttachments = listOf(
                                                    RenderPassColorAttachment(
                                                        view = colorView,
                                                        loadOp = GPULoadOp.Clear,
                                                        storeOp = GPUStoreOp.Store,
                                                        clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                                    ),
                                                ),
                                                depthStencilAttachment = RenderPassDepthStencilAttachment(
                                                    view = depthView,
                                                    depthClearValue = 1f,
                                                    depthLoadOp = GPULoadOp.Clear,
                                                    depthStoreOp = GPUStoreOp.Store,
                                                ),
                                            ),
                                        )
                                        first.setPipeline(red025)
                                        first.draw(3u)
                                        first.end()

                                        val second = encoder.beginRenderPass(
                                            RenderPassDescriptor(
                                                colorAttachments = listOf(
                                                    RenderPassColorAttachment(
                                                        view = colorView,
                                                        loadOp = GPULoadOp.Load,
                                                        storeOp = GPUStoreOp.Store,
                                                        clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                                    ),
                                                ),
                                                depthStencilAttachment = RenderPassDepthStencilAttachment(
                                                    view = depthView,
                                                    depthReadOnly = true,
                                                ),
                                            ),
                                        )
                                        second.setPipeline(green075)
                                        second.draw(3u)
                                        second.setPipeline(blue010)
                                        second.draw(3u)
                                        second.end()

                                        val third = encoder.beginRenderPass(
                                            RenderPassDescriptor(
                                                colorAttachments = listOf(
                                                    RenderPassColorAttachment(
                                                        view = colorView,
                                                        loadOp = GPULoadOp.Load,
                                                        storeOp = GPUStoreOp.Store,
                                                        clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                                    ),
                                                ),
                                                depthStencilAttachment = RenderPassDepthStencilAttachment(
                                                    view = depthView,
                                                    depthLoadOp = GPULoadOp.Load,
                                                    depthStoreOp = GPUStoreOp.Store,
                                                ),
                                            ),
                                        )
                                        third.setPipeline(green020)
                                        third.draw(3u)
                                        third.end()

                                        encoder.finish().use { device.queue.submit(listOf(it)) }
                                    }
                                }
                            }

                            val pixels = readRgba8(device, target, 16, 16)
                            assertPixel(pixels, 16, 8, 8, 0, 255, 0, 255)
                        }
                    }
                }
            }
        }
    }
}
