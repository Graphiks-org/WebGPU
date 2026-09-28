package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.GPUValidationError
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val UNIFORM_SHADER = """
struct Input { value: u32, pad0: u32, pad1: u32, pad2: u32 }

@group(0) @binding(0) var<uniform> input: Input;
@group(0) @binding(1) var<storage, read_write> output: array<u32>;

@compute @workgroup_size(1)
fn main() {
    output[0] = input.value;
}
"""

/**
 * A dynamic uniform offset must be aligned to `minUniformBufferOffsetAlignment`; setting offset 1 and
 * dispatching fails validation.
 */
@AcidTest(
    id = AcidCaseId.ErrorsDynamicOffsetAlignment,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUBufferBindingLayout_hasDynamicOffset,
        ApiSymbols.GPUComputePassEncoder_setBindGroup,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun dynamicOffsetAlignment(device: GPUDevice) {
    val alignment = device.limits.minUniformBufferOffsetAlignment.toULong()
    device.createBuffer(
        BufferDescriptor(alignment + 16uL, GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst),
    ).use { uniform ->
        device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { output ->
            device.createShaderModule(ShaderModuleDescriptor(code = UNIFORM_SHADER)).use { shader ->
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
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, minBindingSize = 4uL),
                            ),
                        ),
                    ),
                ).use { layout ->
                    device.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                        device.createComputePipeline(
                            ComputePipelineDescriptor(compute = ProgrammableStage(shader), layout = pipelineLayout),
                        ).use { pipeline ->
                            device.createBindGroup(
                                BindGroupDescriptor(
                                    layout = layout,
                                    entries = listOf(
                                        BindGroupEntry(0u, BufferBinding(uniform, size = 16uL)),
                                        BindGroupEntry(1u, BufferBinding(output)),
                                    ),
                                ),
                            ).use { group ->
                                device.pushErrorScope(GPUErrorFilter.Validation)
                                try {
                                    device.createCommandEncoder().use { encoder ->
                                        val pass = encoder.beginComputePass()
                                        pass.setPipeline(pipeline)
                                        pass.setBindGroup(0u, group, listOf(1u))
                                        pass.dispatchWorkgroups(1u)
                                        pass.end()
                                        encoder.finish().use { device.queue.submit(listOf(it)) }
                                    }
                                } finally {
                                    val result = device.popErrorScope()
                                    assertTrue(result.isSuccess)
                                    assertIs<GPUValidationError>(result.getOrThrow())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
