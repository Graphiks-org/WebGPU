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

private const val INDEX_TIMES_THREE_SHADER = """
@group(0) @binding(0) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(4)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    out[id.x] = id.x * 3u + 7u;
}
"""

/**
 * `createComputePipelineAsync` resolves a pipeline that is not just non-null: dispatching it over
 * four invocations writes `[7, 10, 13, 16]`, so a resolved-but-unusable pipeline is caught.
 */
@AcidTest(
    id = AcidCaseId.ComputePipelineAsync,
    family = AcidFamily.Compute,
    contract = [
        ApiSymbols.GPUDevice_createComputePipelineAsync,
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPUComputePipelineDescriptor,
        ApiSymbols.GPUComputePipelineDescriptor_compute,
        ApiSymbols.GPUProgrammableStage,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_setPipeline,
        ApiSymbols.GPUComputePassEncoder_setBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUBufferUsage_Storage,
        ApiSymbols.GPUBufferUsage_CopySrc,
    ],
)
suspend fun asyncPipeline(device: GPUDevice) = withValidationScope(device) {
    device.createShaderModule(ShaderModuleDescriptor(code = INDEX_TIMES_THREE_SHADER)).use { shader ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
        ).use { storage ->
            device.createComputePipelineAsync(
                ComputePipelineDescriptor(compute = ProgrammableStage(shader)),
            ).getOrThrow().use { pipeline ->
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
                            encoder.finish().use { device.queue.submit(listOf(it)) }
                        }
                    }
                }
            }

            val bytes = readBufferBytes(device, storage, 16uL)
            assertContentEquals(
                uintArrayOf(7u, 10u, 13u, 16u),
                ArrayBuffer.of(bytes).toUIntArray(),
                "The asynchronously created pipeline must run and write [7, 10, 13, 16]",
            )
        }
    }
}
