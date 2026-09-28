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

/**
 * Creates the pipeline from the shader alone (`layout = null`), dispatches four workgroups and
 * reads the storage buffer back through a staging copy. The constant comes from the stage.
 */
@AcidTest(
    id = AcidCaseId.ComputeAutoLayoutConstants,
    family = AcidFamily.Compute,
    contract = [
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_setPipeline,
        ApiSymbols.GPUComputePassEncoder_setBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUComputePassEncoder_end,
        ApiSymbols.GPUProgrammableStage_constants,
    ],
)
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
