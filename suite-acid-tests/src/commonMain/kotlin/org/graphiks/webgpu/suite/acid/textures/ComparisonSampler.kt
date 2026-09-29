package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUSamplerBindingType
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferBindingLayout
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.SamplerBindingLayout
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

private const val COMPARE_SHADER = """
@group(0) @binding(0) var image: texture_depth_2d;
@group(0) @binding(1) var compareSampler: sampler_comparison;
@group(0) @binding(2) var<storage, read_write> result: array<f32>;

@compute @workgroup_size(1)
fn main() {
    result[0] = textureSampleCompareLevel(image, compareSampler, vec2f(0.5), 0.25);
    result[1] = textureSampleCompareLevel(image, compareSampler, vec2f(0.5), 0.75);
}
"""

/**
 * A `Less` comparison sampler over a uniform 0.5 depth returns 1 when the reference is below the
 * depth and 0 when it is above: references 0.25 and 0.75 give `[1.0, 0.0]`. The output buffer is
 * prefilled with `0xa5`, so the zero would differ from an unwritten value; sampling is away from any
 * depth boundary, so no PCF is involved.
 */
@AcidTest(
    id = AcidCaseId.SamplerComparison,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_compare,
        ApiSymbols.GPUCompareFunction,
        ApiSymbols.GPUCompareFunction_Less,
        ApiSymbols.GPUSamplerBindingType,
        ApiSymbols.GPUSamplerBindingType_Comparison,
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureSampleType_Depth,
        ApiSymbols.GPUTextureViewDimension_TwoD,
    ],
)
suspend fun comparisonSampler(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.Depth32Float,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        texture.createView().use { view ->
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = emptyList(),
                        depthStencilAttachment = RenderPassDepthStencilAttachment(
                            view = view,
                            depthClearValue = 0.5f,
                            depthLoadOp = GPULoadOp.Clear,
                            depthStoreOp = GPUStoreOp.Store,
                        ),
                    ),
                )
                pass.end()
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        device.createSampler(
            SamplerDescriptor(
                compare = GPUCompareFunction.Less,
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest,
            ),
        ).use { sampler ->
            device.createBuffer(
                BufferDescriptor(
                    size = 8uL,
                    usage = GPUBufferUsage.Storage or GPUBufferUsage.CopySrc,
                    mappedAtCreation = true,
                ),
            ).use { output ->
                output.getMappedRange().setBytes(0uL, ByteArray(8) { 0xA5.toByte() })
                output.unmap()

                device.createShaderModule(ShaderModuleDescriptor(code = COMPARE_SHADER)).use { shader ->
                    device.createBindGroupLayout(
                        BindGroupLayoutDescriptor(
                            entries = listOf(
                                BindGroupLayoutEntry(
                                    binding = 0u,
                                    visibility = GPUShaderStage.Compute,
                                    texture = TextureBindingLayout(
                                        sampleType = GPUTextureSampleType.Depth,
                                        viewDimension = GPUTextureViewDimension.TwoD,
                                    ),
                                ),
                                BindGroupLayoutEntry(
                                    binding = 1u,
                                    visibility = GPUShaderStage.Compute,
                                    sampler = SamplerBindingLayout(type = GPUSamplerBindingType.Comparison),
                                ),
                                BindGroupLayoutEntry(
                                    binding = 2u,
                                    visibility = GPUShaderStage.Compute,
                                    buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, minBindingSize = 8uL),
                                ),
                            ),
                        ),
                    ).use { layout ->
                        device.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                            device.createComputePipeline(
                                ComputePipelineDescriptor(compute = ProgrammableStage(shader), layout = pipelineLayout),
                            ).use { pipeline ->
                                texture.createView().use { sampleView ->
                                    device.createBindGroup(
                                        BindGroupDescriptor(
                                            layout = layout,
                                            entries = listOf(
                                                BindGroupEntry(0u, sampleView),
                                                BindGroupEntry(1u, sampler),
                                                BindGroupEntry(2u, BufferBinding(output)),
                                            ),
                                        ),
                                    ).use { group ->
                                        device.createCommandEncoder().use { encoder ->
                                            val pass = encoder.beginComputePass()
                                            pass.setPipeline(pipeline)
                                            pass.setBindGroup(0u, group)
                                            pass.dispatchWorkgroups(1u)
                                            pass.end()
                                            encoder.finish().use { device.queue.submit(listOf(it)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // readBufferBytes copies into its own 0xff-prefilled staging, so the case never maps
                // the storage buffer; the output's own 0xa5 prefill still catches an unwritten value.
                assertContentEquals(
                    floatArrayOf(1.0f, 0.0f),
                    ArrayBuffer.of(readBufferBytes(device, output, 8uL)).toFloatArray(),
                    "Less comparisons of 0.25 and 0.75 against depth 0.5",
                )
            }
        }
    }
}
