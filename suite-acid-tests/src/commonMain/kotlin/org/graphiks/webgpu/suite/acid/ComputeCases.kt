package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
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
import kotlin.test.assertContentEquals

/**
 * A deterministic compute shader: each invocation writes one word scaled by the
 * pipeline-overridable constant `scale`.
 */
private const val SCALE_SHADER = """
override scale: u32 = 1u;
@group(0) @binding(0) var<storage, read_write> output: array<u32>;
@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    output[id.x] = (id.x + 1u) * scale;
}
"""

/**
 * Creates the pipeline from the shader alone (`layout = null`), dispatches four workgroups and
 * reads the storage buffer back through a staging copy. The constant comes from the stage.
 */
suspend fun computeAutoLayout(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
    ).use { storage ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
        ).use { staging ->
            device.createShaderModule(ShaderModuleDescriptor(code = SCALE_SHADER)).use { shader ->
                device.createComputePipeline(
                    ComputePipelineDescriptor(
                        compute = ProgrammableStage(shader, entryPoint = "main", constants = mapOf("scale" to 7.0)),
                    ),
                ).use { pipeline ->
                    pipeline.getBindGroupLayout(0u).use { layout ->
                        device.createBindGroup(
                            BindGroupDescriptor(
                                layout = layout,
                                entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
                            ),
                        ).use { group ->
                            encodeScaleDispatch(device, pipeline, group, storage, staging)
                            readBackStaging(device, staging, uintArrayOf(7u, 14u, 21u, 28u))
                        }
                    }
                }
            }
        }
    }
}

/**
 * The same compute dispatch with an explicit bind group layout and pipeline layout. The stage
 * omits the entry point: the shader has a single compute entry point.
 */
suspend fun computeExplicitLayout(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
    ).use { storage ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
        ).use { staging ->
            device.createShaderModule(ShaderModuleDescriptor(code = SCALE_SHADER)).use { shader ->
                device.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(
                                    type = GPUBufferBindingType.Storage,
                                    minBindingSize = 16uL,
                                ),
                            ),
                        ),
                    ),
                ).use { bindGroupLayout ->
                    device.createPipelineLayout(
                        PipelineLayoutDescriptor(listOf(bindGroupLayout)),
                    ).use { pipelineLayout ->
                        device.createComputePipeline(
                            ComputePipelineDescriptor(
                                compute = ProgrammableStage(shader, constants = mapOf("scale" to 3.0)),
                                layout = pipelineLayout,
                            ),
                        ).use { pipeline ->
                            device.createBindGroup(
                                BindGroupDescriptor(
                                    layout = bindGroupLayout,
                                    entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
                                ),
                            ).use { group ->
                                encodeScaleDispatch(device, pipeline, group, storage, staging)
                                readBackStaging(device, staging, uintArrayOf(3u, 6u, 9u, 12u))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun encodeScaleDispatch(
    device: GPUDevice,
    pipeline: GPUComputePipeline,
    group: GPUBindGroup,
    storage: GPUBuffer,
    staging: GPUBuffer,
) {
    device.createCommandEncoder().use { encoder ->
        val pass = encoder.beginComputePass()
        pass.setPipeline(pipeline)
        pass.setBindGroup(0u, group)
        pass.dispatchWorkgroups(4u)
        pass.end()
        encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
        encoder.finish().use { device.queue.submit(listOf(it)) }
    }
}

private suspend fun readBackStaging(device: GPUDevice, staging: GPUBuffer, expected: UIntArray) {
    staging.mapAsync(GPUMapMode.Read).getOrThrow()
    try {
        assertContentEquals(expected, staging.getMappedRange().toUIntArray())
    } finally {
        staging.unmap()
    }
}
