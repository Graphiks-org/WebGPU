package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.ArrayBuffer
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
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

private const val COPY_VALUE_SHADER = """
struct Input { value: u32, pad0: u32, pad1: u32, pad2: u32 }

@group(0) @binding(0) var<uniform> input: Input;
@group(0) @binding(1) var<storage, read_write> output: array<u32>;

@compute @workgroup_size(1)
fn main() {
    output[0] = input.value;
}
"""

/**
 * A bind group entry smaller than its layout's `minBindingSize` is rejected at creation with a
 * validation error, while an entry of exactly that size is accepted and executes a known write. The
 * rejection is creation-only: a dispatch error would not distinguish a dropped minimum-size
 * requirement.
 */
@AcidTest(
    id = AcidCaseId.ErrorsMinBindingSize,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_createBindGroupLayout,
        ApiSymbols.GPUDevice_createPipelineLayout,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUBufferBindingLayout_minBindingSize,
        ApiSymbols.GPUBufferBinding_size,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUValidationError,
    ],
)
suspend fun minBindingSize(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst),
    ).use { uniform ->
        device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { output ->
            device.queue.writeBuffer(uniform, 0uL, ArrayBuffer.of(uintArrayOf(0xABCDu)))
            device.createShaderModule(ShaderModuleDescriptor(code = COPY_VALUE_SHADER)).use { shader ->
                device.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform, minBindingSize = 16uL),
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
                            // Probe: a 12-byte binding against minBindingSize 16 is a creation-time
                            // validation error. The returned object, if any, is closed outside the
                            // probe so a cleanup failure cannot masquerade as the rejection.
                            device.pushErrorScope(GPUErrorFilter.Validation)
                            var invalid: org.graphiks.webgpu.GPUBindGroup? = null
                            try {
                                invalid = device.createBindGroup(
                                    BindGroupDescriptor(
                                        layout = layout,
                                        entries = listOf(
                                            BindGroupEntry(0u, BufferBinding(uniform, size = 12uL)),
                                            BindGroupEntry(1u, BufferBinding(output)),
                                        ),
                                    ),
                                )
                            } finally {
                                invalid?.close()
                                assertIs<GPUValidationError>(
                                    device.popErrorScope().getOrThrow(),
                                    "A binding smaller than minBindingSize must be a validation error at creation",
                                )
                            }

                            // Control: the exact minimum size is accepted and the kernel writes the value.
                            device.createBindGroup(
                                BindGroupDescriptor(
                                    layout = layout,
                                    entries = listOf(
                                        BindGroupEntry(0u, BufferBinding(uniform, size = 16uL)),
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
                            }
                        }
                    }
                }
            }
            assertContentEquals(
                uintArrayOf(0xABCDu),
                ArrayBuffer.of(readBufferBytes(device, output, 4uL)).toUIntArray(),
                "The adequately sized binding must execute the known write",
            )
        }
    }
}
