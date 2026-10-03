package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePassDescriptor
import org.graphiks.webgpu.descriptors.ComputePassTimestampWrites
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertIs

private const val WRITE_37_SHADER = """
@group(0) @binding(0) var<storage, read_write> value: array<u32>;

@compute @workgroup_size(1)
fn main() {
    value[0] = 37u;
}
"""

/**
 * A compute pass timestamp descriptor is validated against its query set: indices 0 and 1 of a
 * count-2 set are accepted and the pass runs, while an end index of 2 — outside the set — is a
 * validation error. Dropping the descriptor removes the error, so the case fails rather than passing
 * vacuously.
 *
 * Requires the optional `TimestampQuery` feature; the runner reports it as `unsupported` otherwise.
 */
@AcidTest(
    id = AcidCaseId.ErrorsComputeTimestampIndices,
    family = AcidFamily.ErrorsAsync,
    requiredFeatures = [GPUFeatureName.TimestampQuery],
    contract = [
        ApiSymbols.GPUDevice_createQuerySet,
        ApiSymbols.GPUQueryType_Timestamp,
        ApiSymbols.GPUComputePassDescriptor_timestampWrites,
        ApiSymbols.GPUComputePassTimestampWrites_beginningOfPassWriteIndex,
        ApiSymbols.GPUComputePassTimestampWrites_endOfPassWriteIndex,
    ],
)
suspend fun computeTimestampIndices(device: GPUDevice) = withValidationScope(device) {
    device.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Timestamp, count = 2u)).use { querySet ->
        // Control: the valid descriptor is accepted and the dispatched kernel runs.
        device.createBuffer(
            BufferDescriptor(4uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
        ).use { output ->
            device.createShaderModule(ShaderModuleDescriptor(code = WRITE_37_SHADER)).use { shader ->
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
                                val pass = encoder.beginComputePass(
                                    ComputePassDescriptor(
                                        timestampWrites = ComputePassTimestampWrites(
                                            querySet = querySet,
                                            beginningOfPassWriteIndex = 0u,
                                            endOfPassWriteIndex = 1u,
                                        ),
                                    ),
                                )
                                pass.setPipeline(pipeline)
                                pass.setBindGroup(0u, group)
                                pass.dispatchWorkgroups(1u)
                                pass.end()
                                encoder.finish().use { device.queue.submit(listOf(it)) }
                            }
                        }
                    }
                    val produced = readBufferBytes(device, output, 4uL)
                    require(produced[0].toInt() == 37) { "The valid timestamped dispatch must run" }
                }
            }
        }

        // Probe: an out-of-range end index is a validation error. The invalid encoder is finalized
        // inside the scope (without submitting) so the error is observed.
        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginComputePass(
                    ComputePassDescriptor(
                        timestampWrites = ComputePassTimestampWrites(
                            querySet = querySet,
                            beginningOfPassWriteIndex = 0u,
                            endOfPassWriteIndex = 2u,
                        ),
                    ),
                )
                pass.end()
                encoder.finish().close()
            }
        } finally {
            assertIs<GPUValidationError>(
                device.popErrorScope().getOrThrow(),
                "A timestamp write index outside the query set must be a validation error",
            )
        }
    }
}
