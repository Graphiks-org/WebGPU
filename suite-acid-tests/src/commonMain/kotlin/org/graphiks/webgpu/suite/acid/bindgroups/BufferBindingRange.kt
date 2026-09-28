package org.graphiks.webgpu.suite.acid.bindgroups

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUShaderStage
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
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

private const val UNIFORM_RANGE_SHADER = """
struct Input { value: u32, pad0: u32, pad1: u32, pad2: u32 }

@group(0) @binding(0) var<uniform> input: Input;
@group(0) @binding(1) var<storage, read_write> output: array<u32>;

@compute @workgroup_size(1)
fn main() {
    output[0] = input.value;
}
"""

/**
 * A buffer binding exposes the range named by its explicit offset and size, not the buffer start:
 * a uniform buffer holding 7 at byte 0 and 29 at the aligned offset reads back 29 when the binding
 * starts at that alignment.
 */
@AcidTest(
    id = AcidCaseId.BindingsBufferRange,
    family = AcidFamily.BindGroupsLayouts,
    contract = [
        ApiSymbols.GPUDevice_createBindGroupLayout,
        ApiSymbols.GPUDevice_createPipelineLayout,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUBufferBindingLayout_minBindingSize,
        ApiSymbols.GPUBufferBinding_offset,
        ApiSymbols.GPUBufferBinding_size,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
    ],
)
suspend fun bufferBindingRange(device: GPUDevice) = withValidationScope(device) {
    val alignment = device.limits.minUniformBufferOffsetAlignment.toULong()

    device.createBuffer(
        BufferDescriptor(alignment + 16uL, GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst),
    ).use { uniform ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
        ).use { output ->
            device.queue.writeBuffer(uniform, 0uL, ArrayBuffer.of(uintArrayOf(7u)))
            device.queue.writeBuffer(uniform, alignment, ArrayBuffer.of(uintArrayOf(29u)))

            device.createShaderModule(ShaderModuleDescriptor(code = UNIFORM_RANGE_SHADER)).use { shader ->
                device.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(
                                    type = GPUBufferBindingType.Uniform,
                                    minBindingSize = 16uL,
                                ),
                            ),
                            BindGroupLayoutEntry(
                                binding = 1u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(
                                    type = GPUBufferBindingType.Storage,
                                    minBindingSize = 4uL,
                                ),
                            ),
                        ),
                    ),
                ).use { layout ->
                    device.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                        device.createComputePipeline(
                            ComputePipelineDescriptor(
                                compute = ProgrammableStage(shader),
                                layout = pipelineLayout,
                            ),
                        ).use { pipeline ->
                            device.createBindGroup(
                                BindGroupDescriptor(
                                    layout = layout,
                                    entries = listOf(
                                        BindGroupEntry(0u, BufferBinding(uniform, offset = alignment, size = 16uL)),
                                        BindGroupEntry(1u, BufferBinding(output)),
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
                                val words = ArrayBuffer.of(readBufferBytes(device, output, 16uL)).toUIntArray()
                                assertContentEquals(
                                    uintArrayOf(29u, 0u, 0u, 0u),
                                    words,
                                    "The bound range starts at the aligned offset, so it reads 29 and not 7",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
