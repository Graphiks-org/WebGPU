package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUPrimitiveTopology
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
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.PrimitiveState
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.SamplerBindingLayout
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

private const val SPLIT_DEPTH_SHADER = """
@vertex fn splitMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    // A triangle whose left edge stops short of the first texel centre: it covers the right
    // column of a 2x2 depth target and writes depth 0.5 there; the clear wrote 0.25 everywhere.
    let points = array<vec2f,3>(vec2f(-0.4, -1.0), vec2f(3.0, -1.0), vec2f(-0.4, 3.0));
    return vec4f(points[i], 0.5, 1.0);
}
"""

private const val PCF_SHADER = """
@group(0) @binding(0) var image: texture_depth_2d;
@group(0) @binding(1) var lessLinear: sampler_comparison;
@group(0) @binding(2) var greaterLinear: sampler_comparison;
@group(0) @binding(3) var lessNearest: sampler_comparison;
@group(0) @binding(4) var<storage, read_write> result: array<f32>;

@compute @workgroup_size(1)
fn main() {
    // References beyond both depths are filter-independent: every tap passes or fails the same
    // way however the implementation filters comparison results.
    result[0] = textureSampleCompareLevel(image, lessLinear, vec2f(0.5), 0.125);
    result[1] = textureSampleCompareLevel(image, lessLinear, vec2f(0.5), 0.75);
    result[2] = textureSampleCompareLevel(image, greaterLinear, vec2f(0.5), 0.75);
    result[3] = textureSampleCompareLevel(image, greaterLinear, vec2f(0.5), 0.125);
    // With nearest filtering the single tap's comparison is binary: the between-depths
    // reference 0.375 fails on the 0.25 tap and passes on the 0.5 tap.
    result[4] = textureSampleCompareLevel(image, lessNearest, vec2f(0.25), 0.375);
    result[5] = textureSampleCompareLevel(image, lessNearest, vec2f(0.75), 0.375);
}
"""

/**
 * A Depth32Float 2x2 target holds depth 0.25 in its left column and 0.5 in its right column — a
 * clear writes the first value and a depth-only draw writes the second. Comparison samplers then
 * observe percentage-closer comparison semantics over a split depth. The filtered result of a
 * comparison sampler is implementation-dependent, so the case asserts the filter-independent
 * envelope: with linear comparison samplers, a reference below both depths returns 1 for `Less`
 * and 0 for `Greater`, and a reference above both depths returns the opposite, so the two
 * comparators are told apart whatever the implementation's filtering. A nearest comparison at a
 * between-depths reference returns each single tap's binary result — 0 on the 0.25 tap, 1 on the
 * 0.5 tap — so the split depth is observed, not assumed.
 */
@AcidTest(
    id = AcidCaseId.SamplerComparisonPcf,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureFormat_Depth32Float,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_TextureBinding,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderPassDepthStencilAttachment,
        ApiSymbols.GPUDepthStencilState_depthCompare,
        ApiSymbols.GPUDepthStencilState_depthWriteEnabled,
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_compare,
        ApiSymbols.GPUSamplerDescriptor_magFilter,
        ApiSymbols.GPUSamplerDescriptor_minFilter,
        ApiSymbols.GPUSamplerDescriptor_mipmapFilter,
        ApiSymbols.GPUCompareFunction_Less,
        ApiSymbols.GPUCompareFunction_Greater,
        ApiSymbols.GPUSamplerBindingType_Comparison,
        ApiSymbols.GPUTextureSampleType_Depth,
        ApiSymbols.GPUTextureViewDimension_TwoD,
    ],
)
suspend fun comparisonPcf(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.Depth32Float,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        texture.createView().use { view ->
            device.createCommandEncoder().use { encoder ->
                val clear = encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = emptyList(),
                        depthStencilAttachment = RenderPassDepthStencilAttachment(
                            view = view,
                            depthClearValue = 0.25f,
                            depthLoadOp = GPULoadOp.Clear,
                            depthStoreOp = GPUStoreOp.Store,
                        ),
                    ),
                )
                clear.end()

                device.createShaderModule(ShaderModuleDescriptor(code = SPLIT_DEPTH_SHADER)).use { shader ->
                    device.createRenderPipeline(
                        RenderPipelineDescriptor(
                            vertex = VertexState(module = shader, entryPoint = "splitMain"),
                            primitive = PrimitiveState(topology = GPUPrimitiveTopology.TriangleList),
                            depthStencil = DepthStencilState(
                                format = GPUTextureFormat.Depth32Float,
                                depthWriteEnabled = true,
                                depthCompare = GPUCompareFunction.Always,
                            ),
                        ),
                    ).use { pipeline ->
                        val split = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = emptyList(),
                                depthStencilAttachment = RenderPassDepthStencilAttachment(
                                    view = view,
                                    depthLoadOp = GPULoadOp.Load,
                                    depthStoreOp = GPUStoreOp.Store,
                                ),
                            ),
                        )
                        split.setPipeline(pipeline)
                        split.draw(3u)
                        split.end()
                    }
                }

                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        device.createSampler(
            SamplerDescriptor(
                compare = GPUCompareFunction.Less,
                magFilter = GPUFilterMode.Linear,
                minFilter = GPUFilterMode.Linear,
                mipmapFilter = GPUMipmapFilterMode.Linear,
            ),
        ).use { lessLinear ->
            device.createSampler(
                SamplerDescriptor(
                    compare = GPUCompareFunction.Greater,
                    magFilter = GPUFilterMode.Linear,
                    minFilter = GPUFilterMode.Linear,
                    mipmapFilter = GPUMipmapFilterMode.Linear,
                ),
            ).use { greaterLinear ->
                device.createSampler(
                    SamplerDescriptor(compare = GPUCompareFunction.Less),
                ).use { lessNearest ->
                    device.createBuffer(
                        BufferDescriptor(24uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
                    ).use { output ->
                        device.createShaderModule(ShaderModuleDescriptor(code = PCF_SHADER)).use { shader ->
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
                                            sampler = SamplerBindingLayout(type = GPUSamplerBindingType.Comparison),
                                        ),
                                        BindGroupLayoutEntry(
                                            binding = 3u,
                                            visibility = GPUShaderStage.Compute,
                                            sampler = SamplerBindingLayout(type = GPUSamplerBindingType.Comparison),
                                        ),
                                        BindGroupLayoutEntry(
                                            binding = 4u,
                                            visibility = GPUShaderStage.Compute,
                                            buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, minBindingSize = 24uL),
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
                                                        BindGroupEntry(1u, lessLinear),
                                                        BindGroupEntry(2u, greaterLinear),
                                                        BindGroupEntry(3u, lessNearest),
                                                        BindGroupEntry(4u, BufferBinding(output)),
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

                        val results = ArrayBuffer.of(readBufferBytes(device, output, 24uL)).toFloatArray()

                        assertContentEquals(
                            floatArrayOf(1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f),
                            results,
                            "Linear Less and Greater over the split depth must answer 1 and 0 for references beyond " +
                                "both depths in opposite directions, and nearest taps at 0.375 must answer the " +
                                "0.25 tap with 0 and the 0.5 tap with 1",
                        )
                    }
                }
            }
        }
    }
}
