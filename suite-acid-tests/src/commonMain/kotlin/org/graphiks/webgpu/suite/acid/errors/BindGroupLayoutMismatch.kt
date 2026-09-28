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

private const val STORAGE_SHADER = """
@group(0) @binding(0) var<storage, read_write> data: array<u32>;

@compute @workgroup_size(1)
fn main() {
    data[0] = 1u;
}
"""

/**
 * A bind group built from a layout that declares binding 0 as a uniform buffer is set on a pipeline
 * whose layout declares binding 0 as storage: `setBindGroup` fails validation.
 */
@AcidTest(
    id = AcidCaseId.ErrorsBindGroupLayoutMismatch,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_createBindGroupLayout,
        ApiSymbols.GPUBufferBindingLayout_type,
        ApiSymbols.GPUComputePassEncoder_setBindGroup,
        ApiSymbols.GPUDevice_pushErrorScope,
    ],
)
suspend fun bindGroupLayoutMismatch(device: GPUDevice) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst),
    ).use { uniform ->
        device.createShaderModule(ShaderModuleDescriptor(code = STORAGE_SHADER)).use { shader ->
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    entries = listOf(
                        BindGroupLayoutEntry(
                            binding = 0u,
                            visibility = GPUShaderStage.Compute,
                            buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform, minBindingSize = 16uL),
                        ),
                    ),
                ),
            ).use { uniformLayout ->
                device.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, minBindingSize = 4uL),
                            ),
                        ),
                    ),
                ).use { storageLayout ->
                    device.createPipelineLayout(PipelineLayoutDescriptor(listOf(storageLayout))).use { pipelineLayout ->
                        device.createComputePipeline(
                            ComputePipelineDescriptor(compute = ProgrammableStage(shader), layout = pipelineLayout),
                        ).use { pipeline ->
                            device.createBindGroup(
                                BindGroupDescriptor(
                                    layout = uniformLayout,
                                    entries = listOf(BindGroupEntry(0u, BufferBinding(uniform))),
                                ),
                            ).use { group ->
                                device.pushErrorScope(GPUErrorFilter.Validation)
                                try {
                                    device.createCommandEncoder().use { encoder ->
                                        val pass = encoder.beginComputePass()
                                        pass.setPipeline(pipeline)
                                        pass.setBindGroup(0u, group)
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
