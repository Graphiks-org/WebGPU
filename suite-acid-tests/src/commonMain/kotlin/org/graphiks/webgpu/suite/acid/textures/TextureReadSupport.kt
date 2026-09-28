package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUSampler
import org.graphiks.webgpu.GPUSamplerBindingType
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferBindingLayout
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.SamplerBindingLayout
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.suite.acid.readBufferBytes

/**
 * Runs a compute shader that reads [textureView] at `@binding(0)` into a storage output at
 * `@binding(1)` (and an optional sampler at `@binding(2)`) and returns the raw output bytes.
 *
 * The case owns the texture, the view and the shader; this only wires the read so each case shows
 * the texture resource it binds and asserts on the read values. The output starts zeroed.
 */
internal suspend fun computeTextureRead(
    device: GPUDevice,
    shaderCode: String,
    textureView: GPUTextureView,
    textureLayout: TextureBindingLayout,
    outputBytes: ULong,
    sampler: GPUSampler? = null,
    workgroupsX: UInt = 1u,
    workgroupsY: UInt = 1u,
): ByteArray {
    device.createBuffer(
        BufferDescriptor(outputBytes, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
    ).use { output ->
        device.createShaderModule(ShaderModuleDescriptor(code = shaderCode)).use { shader ->
            val layoutEntries = buildList {
                add(BindGroupLayoutEntry(binding = 0u, visibility = GPUShaderStage.Compute, texture = textureLayout))
                add(
                    BindGroupLayoutEntry(
                        binding = 1u,
                        visibility = GPUShaderStage.Compute,
                        buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, minBindingSize = outputBytes),
                    ),
                )
                if (sampler != null) {
                    add(
                        BindGroupLayoutEntry(
                            binding = 2u,
                            visibility = GPUShaderStage.Compute,
                            sampler = SamplerBindingLayout(type = GPUSamplerBindingType.Filtering),
                        ),
                    )
                }
            }
            device.createBindGroupLayout(BindGroupLayoutDescriptor(entries = layoutEntries)).use { bindGroupLayout ->
                device.createPipelineLayout(PipelineLayoutDescriptor(listOf(bindGroupLayout))).use { pipelineLayout ->
                    device.createComputePipeline(
                        ComputePipelineDescriptor(compute = ProgrammableStage(shader), layout = pipelineLayout),
                    ).use { pipeline ->
                        val entries = buildList {
                            add(BindGroupEntry(0u, textureView))
                            add(BindGroupEntry(1u, BufferBinding(output)))
                            if (sampler != null) add(BindGroupEntry(2u, sampler))
                        }
                        device.createBindGroup(
                            BindGroupDescriptor(layout = bindGroupLayout, entries = entries),
                        ).use { group ->
                            device.createCommandEncoder().use { encoder ->
                                val pass = encoder.beginComputePass()
                                pass.setPipeline(pipeline)
                                pass.setBindGroup(0u, group)
                                pass.dispatchWorkgroups(workgroupsX, workgroupsY)
                                pass.end()
                                encoder.finish().use { device.queue.submit(listOf(it)) }
                            }
                        }
                    }
                }
            }
        }
        return readBufferBytes(device, output, outputBytes)
    }
}
