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
import kotlin.test.assertEquals

private const val DYNAMIC_UNIFORM_SHADER = """
struct Input { value: u32, pad0: u32, pad1: u32, pad2: u32 }

@group(0) @binding(0) var<uniform> input: Input;
@group(0) @binding(1) var<storage, read_write> output: array<u32>;

@compute @workgroup_size(1)
fn main() {
    output[0] = input.value;
}
"""

/**
 * With `hasDynamicOffset = true`, the dynamic offset selects which aligned block of the uniform
 * buffer the binding sees: two passes read 3 (offset 0) and 9 (offset = alignment) from the same
 * binding.
 */
@AcidTest(
    id = AcidCaseId.BindingsDynamicUniformOffsets,
    family = AcidFamily.BindGroupsLayouts,
    contract = [
        ApiSymbols.GPUDevice_createBindGroupLayout,
        ApiSymbols.GPUDevice_createPipelineLayout,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUBufferBindingLayout_hasDynamicOffset,
        ApiSymbols.GPUBindGroupLayoutEntry_buffer,
        ApiSymbols.GPUComputePassEncoder_setBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
    ],
)
suspend fun dynamicUniformOffsets(device: GPUDevice) = withValidationScope(device) {
    val alignment = device.limits.minUniformBufferOffsetAlignment
    val alignmentBytes = alignment.toULong()

    device.createBuffer(
        BufferDescriptor(alignmentBytes + 16uL, GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst),
    ).use { uniform ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
        ).use { outputA ->
            device.createBuffer(
                BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
            ).use { outputB ->
                device.queue.writeBuffer(uniform, 0uL, ArrayBuffer.of(uintArrayOf(3u)))
                device.queue.writeBuffer(uniform, alignmentBytes, ArrayBuffer.of(uintArrayOf(9u)))

                device.createShaderModule(ShaderModuleDescriptor(code = DYNAMIC_UNIFORM_SHADER)).use { shader ->
                    device.createBindGroupLayout(
                        BindGroupLayoutDescriptor(
                            entries = listOf(
                                BindGroupLayoutEntry(
                                    binding = 0u,
                                    visibility = GPUShaderStage.Compute,
                                    buffer = BufferBindingLayout(
                                        type = GPUBufferBindingType.Uniform,
                                        hasDynamicOffset = true,
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
                                            BindGroupEntry(0u, BufferBinding(uniform, size = 16uL)),
                                            BindGroupEntry(1u, BufferBinding(outputA)),
                                        ),
                                    ),
                                ).use { groupA ->
                                    device.createBindGroup(
                                        BindGroupDescriptor(
                                            layout = layout,
                                            entries = listOf(
                                                BindGroupEntry(0u, BufferBinding(uniform, size = 16uL)),
                                                BindGroupEntry(1u, BufferBinding(outputB)),
                                            ),
                                        ),
                                    ).use { groupB ->
                                        dispatchWithDynamicOffset(device, pipeline, groupA, listOf(0u))
                                        dispatchWithDynamicOffset(device, pipeline, groupB, listOf(alignment))

                                        val wordsA = ArrayBuffer.of(readBufferBytes(device, outputA, 16uL)).toUIntArray()
                                        val wordsB = ArrayBuffer.of(readBufferBytes(device, outputB, 16uL)).toUIntArray()
                                        assertEquals(3u, wordsA[0], "The zero dynamic offset selects the value 3")
                                        assertEquals(9u, wordsB[0], "The aligned dynamic offset selects the value 9")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Runs one dispatch whose uniform binding is moved by [dynamicOffsets]. */
private fun dispatchWithDynamicOffset(
    device: GPUDevice,
    pipeline: org.graphiks.webgpu.GPUComputePipeline,
    group: org.graphiks.webgpu.GPUBindGroup,
    dynamicOffsets: List<UInt>,
) {
    device.createCommandEncoder().use { encoder ->
        val pass = encoder.beginComputePass()
        pass.setPipeline(pipeline)
        pass.setBindGroup(0u, group, dynamicOffsets)
        pass.dispatchWorkgroups(1u)
        pass.end()
        encoder.finish().use { device.queue.submit(listOf(it)) }
    }
}
