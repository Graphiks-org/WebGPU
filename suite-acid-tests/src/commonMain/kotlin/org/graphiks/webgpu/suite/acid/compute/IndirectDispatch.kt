package org.graphiks.webgpu.suite.acid.compute

import org.graphiks.webgpu.ArrayBuffer
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
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

private const val INDIRECT_DISPATCH_SHADER = """
@group(0) @binding(0) var<storage, read_write> output: array<u32>;

@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    output[id.x] = id.x + 10u;
}
"""

/**
 * `dispatchWorkgroupsIndirect` takes its workgroup counts from a buffer: the 12-byte `[2, 1, 1]`
 * indirect arguments run two invocations, writing `[10, 11, 0, 0]`.
 */
@AcidTest(
    id = AcidCaseId.ComputeIndirectDispatch,
    family = AcidFamily.Compute,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUQueue_writeBuffer,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroupsIndirect,
        ApiSymbols.GPUBufferUsage_Indirect,
    ],
)
suspend fun indirectDispatch(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(12uL, GPUBufferUsage.Indirect or GPUBufferUsage.CopyDst),
    ).use { indirect ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
        ).use { output ->
            device.queue.writeBuffer(indirect, 0uL, ArrayBuffer.of(uintArrayOf(2u, 1u, 1u)))

            device.createShaderModule(ShaderModuleDescriptor(code = INDIRECT_DISPATCH_SHADER)).use { shader ->
                device.createComputePipeline(
                    ComputePipelineDescriptor(compute = ProgrammableStage(shader)),
                ).use { pipeline ->
                    pipeline.getBindGroupLayout(0u).use { layout ->
                        device.createBindGroup(
                            BindGroupDescriptor(
                                layout = layout,
                                entries = listOf(BindGroupEntry(0u, BufferBinding(output))),
                            ),
                        ).use { group ->
                            device.createCommandEncoder().use { encoder ->
                                val pass = encoder.beginComputePass()
                                pass.setPipeline(pipeline)
                                pass.setBindGroup(0u, group)
                                pass.dispatchWorkgroupsIndirect(indirect, 0uL)
                                pass.end()
                                encoder.finish().use { device.queue.submit(listOf(it)) }
                            }
                            val words = ArrayBuffer.of(readBufferBytes(device, output, 16uL)).toUIntArray()
                            assertContentEquals(
                                uintArrayOf(10u, 11u, 0u, 0u),
                                words,
                                "The indirect workgroup counts come from the buffer",
                            )
                        }
                    }
                }
            }
        }
    }
}
