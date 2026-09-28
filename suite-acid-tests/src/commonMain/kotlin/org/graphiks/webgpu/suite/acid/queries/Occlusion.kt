package org.graphiks.webgpu.suite.acid.queries

import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createDepthStencilTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertTrue

private const val RED_NEAR_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.25, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

private const val GREEN_FAR_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.75, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0,1,0,1); }
"""

/**
 * Two occlusion queries in one pass: the near fragment passes the depth test and contributes
 * samples, the far fragment is rejected by depth and contributes none. The first query's 64-bit
 * result is non-zero and the second is zero.
 */
@AcidTest(
    id = AcidCaseId.QueriesOcclusion,
    family = AcidFamily.QueriesTimestamps,
    contract = [
        ApiSymbols.GPUDevice_createQuerySet,
        ApiSymbols.GPUQueryType,
        ApiSymbols.GPURenderPassDescriptor_occlusionQuerySet,
        ApiSymbols.GPURenderPassEncoder_beginOcclusionQuery,
        ApiSymbols.GPUCommandEncoder_resolveQuerySet,
    ],
)
suspend fun occlusion(device: GPUDevice) = withValidationScope(device) {
    val format = GPUTextureFormat.Depth32Float
    device.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Occlusion, count = 2u)).use { queries ->
        device.createBuffer(
            BufferDescriptor(16uL, org.graphiks.webgpu.GPUBufferUsage.QueryResolve or org.graphiks.webgpu.GPUBufferUsage.CopySrc),
        ).use { resolve ->
            device.createBuffer(
                BufferDescriptor(16uL, org.graphiks.webgpu.GPUBufferUsage.CopyDst or org.graphiks.webgpu.GPUBufferUsage.MapRead),
            ).use { staging ->
                createRenderPipeline(
                    device,
                    RED_NEAR_SHADER,
                    depthStencil = DepthStencilState(format, depthWriteEnabled = true, depthCompare = GPUCompareFunction.Less),
                ).use { red ->
                    createRenderPipeline(
                        device,
                        GREEN_FAR_SHADER,
                        depthStencil = DepthStencilState(format, depthWriteEnabled = true, depthCompare = GPUCompareFunction.Less),
                    ).use { green ->
                        createColorTarget(device, 16, 16).use { target ->
                            createDepthStencilTarget(device, 16, 16, format).use { depth ->
                                target.createView().use { colorView ->
                                    depth.createView().use { depthView ->
                                        device.createCommandEncoder().use { encoder ->
                                            val pass = encoder.beginRenderPass(
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
                                                    occlusionQuerySet = queries,
                                                ),
                                            )
                                            pass.beginOcclusionQuery(0u)
                                            pass.setPipeline(red)
                                            pass.draw(3u)
                                            pass.endOcclusionQuery()

                                            pass.beginOcclusionQuery(1u)
                                            pass.setPipeline(green)
                                            pass.draw(3u)
                                            pass.endOcclusionQuery()
                                            pass.end()

                                            encoder.resolveQuerySet(queries, 0u, 2u, resolve, 0uL)
                                            encoder.copyBufferToBuffer(resolve, 0uL, staging, 0uL, 16uL)
                                            encoder.finish().use { device.queue.submit(listOf(it)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                val words = try {
                    staging.getMappedRange().toUIntArray()
                } finally {
                    staging.unmap()
                }
                assertTrue(words[0] != 0u || words[1] != 0u, "The visible draw must contribute samples")
                assertTrue(words[2] == 0u && words[3] == 0u, "The depth-rejected draw must contribute none")
            }
        }
    }
}
