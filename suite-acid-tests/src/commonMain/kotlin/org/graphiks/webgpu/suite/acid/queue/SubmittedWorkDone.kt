package org.graphiks.webgpu.suite.acid.queue

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
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
import kotlin.test.assertContentEquals

private const val ELEVENS_SHADER = """
@group(0) @binding(0) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(4)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    out[id.x] = 11u + id.x * 11u;
}
"""

/**
 * A compute pass and the copy of its result into a staging buffer are encoded in one submission;
 * `onSubmittedWorkDone` then resolves and the mapped staging reads the four values, proving the wait
 * is usable. This does not claim a timing guarantee, only that the awaited work completed.
 */
@AcidTest(
    id = AcidCaseId.QueueSubmittedWorkDone,
    family = AcidFamily.QueueCommands,
    contract = [
        ApiSymbols.GPUQueue_onSubmittedWorkDone,
        ApiSymbols.GPUQueue_submit,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
    ],
)
suspend fun submittedWorkDone(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
    ).use { storage ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
        ).use { staging ->
            device.createShaderModule(ShaderModuleDescriptor(code = ELEVENS_SHADER)).use { shader ->
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
                            device.createCommandEncoder().use { encoder ->
                                val pass = encoder.beginComputePass()
                                pass.setPipeline(pipeline)
                                pass.setBindGroup(0u, group)
                                pass.dispatchWorkgroups(1u)
                                pass.end()
                                encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
                                encoder.finish().use { device.queue.submit(listOf(it)) }
                            }
                        }
                    }
                }
            }

            device.queue.onSubmittedWorkDone().getOrThrow()
            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                assertContentEquals(
                    uintArrayOf(11u, 22u, 33u, 44u),
                    ArrayBuffer.of(staging.getMappedRange().toByteArray()).toUIntArray(),
                    "The awaited submission must have written its result",
                )
            } finally {
                staging.unmap()
            }
        }
    }
}
