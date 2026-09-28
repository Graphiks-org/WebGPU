package org.graphiks.webgpu.suite.acid.compute

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
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * The same compute dispatch with an explicit bind group layout and pipeline layout. The stage
 * omits the entry point: the shader has a single compute entry point.
 */
@AcidTest(
    id = AcidCaseId.ComputeExplicitLayoutEntrypoint,
    family = AcidFamily.Compute,
    contract = [
        ApiSymbols.GPUDevice_createBindGroupLayout,
        ApiSymbols.GPUDevice_createPipelineLayout,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUBufferBindingLayout_minBindingSize,
    ],
)
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
