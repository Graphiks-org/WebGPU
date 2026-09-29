package org.graphiks.webgpu.suite.acid.queue

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.renderbundles.BUNDLE_SQUARE_SHADER
import org.graphiks.webgpu.suite.acid.renderbundles.BUNDLE_SQUARE_VERTICES
import org.graphiks.webgpu.suite.acid.renderbundles.bundleVertexLayout
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals

private const val WRITE_37_SHADER = """
@group(0) @binding(0) var<storage, read_write> value: array<u32>;

@compute @workgroup_size(1)
fn main() {
    value[0] = 37u;
}
"""

/**
 * Balanced debug groups and `insertDebugMarker` with Unicode labels wrap real work on the command
 * encoder, a compute pass and a render bundle. The compute still writes 37 and the bundle still
 * paints its square, so the markers decorate the commands without replacing them. The check is that
 * the calls are accepted and preserve the work, not that a label appears in an external debugger.
 */
@AcidTest(
    id = AcidCaseId.CommandDebugMarkers,
    family = AcidFamily.QueueCommands,
    contract = [
        ApiSymbols.GPUDebugCommandsMixin_pushDebugGroup,
        ApiSymbols.GPUDebugCommandsMixin_popDebugGroup,
        ApiSymbols.GPUDebugCommandsMixin_insertDebugMarker,
        ApiSymbols.GPUCommandEncoder_pushDebugGroup,
        ApiSymbols.GPUCommandEncoder_popDebugGroup,
        ApiSymbols.GPUCommandEncoder_insertDebugMarker,
        ApiSymbols.GPUComputePassEncoder_pushDebugGroup,
        ApiSymbols.GPUComputePassEncoder_popDebugGroup,
        ApiSymbols.GPUComputePassEncoder_insertDebugMarker,
        ApiSymbols.GPURenderPassEncoder_pushDebugGroup,
        ApiSymbols.GPURenderPassEncoder_popDebugGroup,
        ApiSymbols.GPURenderPassEncoder_insertDebugMarker,
        ApiSymbols.GPURenderBundleEncoder_pushDebugGroup,
        ApiSymbols.GPURenderBundleEncoder_popDebugGroup,
        ApiSymbols.GPURenderBundleEncoder_insertDebugMarker,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUComputePassEncoder_end,
        ApiSymbols.GPUDevice_createRenderBundleEncoder,
        ApiSymbols.GPURenderBundleEncoder_finish,
        ApiSymbols.GPURenderPassEncoder_executeBundles,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPUBufferUsage_Storage,
        ApiSymbols.GPUBufferUsage_CopySrc,
        ApiSymbols.GPUBufferUsage_Vertex,
        ApiSymbols.GPUBufferUsage_CopyDst,
    ],
)
suspend fun debugMarkers(device: GPUDevice) = withValidationScope(device) {
    device.createShaderModule(ShaderModuleDescriptor(code = WRITE_37_SHADER)).use { shader ->
        device.createBuffer(
            BufferDescriptor(4uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
        ).use { storage ->
            device.createComputePipeline(
                ComputePipelineDescriptor(compute = ProgrammableStage(shader)),
            ).use { pipeline ->
                pipeline.getBindGroupLayout(0u).use { layout ->
                    device.createBindGroup(
                        BindGroupDescriptor(
                            layout = layout,
                            entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
                        ),
                    ).use { group ->
                        device.createBuffer(
                            BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst),
                        ).use { vertices ->
                            device.queue.writeBuffer(vertices, 0uL, ArrayBuffer.of(BUNDLE_SQUARE_VERTICES))
                            createRenderPipeline(device, BUNDLE_SQUARE_SHADER, vertexLayouts = bundleVertexLayout()).use { renderPipeline ->
                                val bundle = device.createRenderBundleEncoder(
                                    RenderBundleEncoderDescriptor(colorFormats = listOf(GPUTextureFormat.RGBA8Unorm)),
                                ).use { bundleEncoder ->
                                    bundleEncoder.pushDebugGroup("bundle-λ")
                                    bundleEncoder.insertDebugMarker("repère-bundle-λ")
                                    bundleEncoder.setPipeline(renderPipeline)
                                    bundleEncoder.setVertexBuffer(0u, vertices)
                                    bundleEncoder.draw(6u)
                                    bundleEncoder.popDebugGroup()
                                    bundleEncoder.finish()
                                }

                                createColorTarget(device, 16, 16).use { target ->
                                    target.createView().use { view ->
                                        device.createCommandEncoder().use { encoder ->
                                            encoder.pushDebugGroup("encoder-λ")
                                            encoder.insertDebugMarker("repère-encoder-λ")

                                            val compute = encoder.beginComputePass()
                                            compute.pushDebugGroup("compute-λ")
                                            compute.insertDebugMarker("repère-compute-λ")
                                            compute.setPipeline(pipeline)
                                            compute.setBindGroup(0u, group)
                                            compute.dispatchWorkgroups(1u)
                                            compute.popDebugGroup()
                                            compute.end()

                                            val render = encoder.beginRenderPass(
                                                RenderPassDescriptor(
                                                    colorAttachments = listOf(
                                                        RenderPassColorAttachment(
                                                            view = view,
                                                            loadOp = GPULoadOp.Clear,
                                                            storeOp = GPUStoreOp.Store,
                                                            clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                                        ),
                                                    ),
                                                ),
                                            )
                                            render.pushDebugGroup("render-λ")
                                            render.insertDebugMarker("repère-render-λ")
                                            render.executeBundles(listOf(bundle))
                                            render.popDebugGroup()
                                            render.end()

                                            encoder.popDebugGroup()
                                            encoder.finish().use { device.queue.submit(listOf(it)) }
                                        }
                                    }

                                    val pixels = readRgba8(device, target, 16, 16)
                                    assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                                    assertPixel(pixels, 16, 1, 1, 0, 0, 0, 255)
                                }
                            }
                        }

                        val bytes = readBufferBytes(device, storage, 4uL)
                        assertEquals(37, bytes[0].toInt() and 255, "The compute under the debug groups must still write 37")
                        assertEquals(0, bytes[1].toInt() and 255)
                        assertEquals(0, bytes[2].toInt() and 255)
                        assertEquals(0, bytes[3].toInt() and 255)
                    }
                }
            }
        }
    }
}
