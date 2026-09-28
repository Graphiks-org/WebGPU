package org.graphiks.webgpu.suite.acid.compute

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val WRITE_SEVEN_SHADER = """
@group(0) @binding(0) var<storage, read_write> value: array<u32>;

@compute @workgroup_size(1)
fn main() {
    value[0] = 7u;
}
"""

private const val MULTIPLY_SHADER = """
@group(0) @binding(0) var<storage, read_write> value: array<u32>;

@compute @workgroup_size(1)
fn main() {
    value[0] = value[0] * 3u + 1u;
}
"""

/**
 * Two compute passes encoded in one submission are ordered: the first writes 7, the second reads it
 * back and writes `7 * 3 + 1 = 22`, with no CPU synchronisation between them.
 */
@AcidTest(
    id = AcidCaseId.ComputeOrderedPasses,
    family = AcidFamily.Compute,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_setPipeline,
        ApiSymbols.GPUComputePassEncoder_setBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUComputePassEncoder_end,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
    ],
)
suspend fun orderedPasses(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
    ).use { storage ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
        ).use { staging ->
            device.createShaderModule(ShaderModuleDescriptor(code = WRITE_SEVEN_SHADER)).use { writer ->
                device.createShaderModule(ShaderModuleDescriptor(code = MULTIPLY_SHADER)).use { multiplier ->
                    device.createComputePipeline(
                        ComputePipelineDescriptor(compute = ProgrammableStage(writer)),
                    ).use { writePipeline ->
                        writePipeline.getBindGroupLayout(0u).use { writeLayout ->
                            device.createComputePipeline(
                                ComputePipelineDescriptor(compute = ProgrammableStage(multiplier)),
                            ).use { multiplyPipeline ->
                                multiplyPipeline.getBindGroupLayout(0u).use { multiplyLayout ->
                                    device.createBindGroup(
                                        BindGroupDescriptor(
                                            layout = writeLayout,
                                            entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
                                        ),
                                    ).use { writeGroup ->
                                        device.createBindGroup(
                                            BindGroupDescriptor(
                                                layout = multiplyLayout,
                                                entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
                                            ),
                                        ).use { multiplyGroup ->
                                            device.createCommandEncoder().use { encoder ->
                                                val first = encoder.beginComputePass()
                                                first.setPipeline(writePipeline)
                                                first.setBindGroup(0u, writeGroup)
                                                first.dispatchWorkgroups(1u)
                                                first.end()

                                                val second = encoder.beginComputePass()
                                                second.setPipeline(multiplyPipeline)
                                                second.setBindGroup(0u, multiplyGroup)
                                                second.dispatchWorkgroups(1u)
                                                second.end()

                                                encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
                                                encoder.finish().use { device.queue.submit(listOf(it)) }
                                            }
                                            readBackStaging(device, staging, uintArrayOf(22u, 0u, 0u, 0u))
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
}
