package org.graphiks.webgpu.suite.acid.queries

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQueryType
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
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

private const val SENTINEL_WORD = 0xFFFFFFFFu

private const val TIMED_COMPUTE_SHADER = """
@group(0) @binding(0) var<storage, read_write> output: array<u32>;

@compute @workgroup_size(1)
fn main() {
    output[0] = 1u;
}
"""

/**
 * A compute pass writes a begin and an end timestamp, resolved into a 512-byte destination at offset
 * 256 that was prefilled with `0xff`. The 16 written bytes replace their sentinels, every other range
 * stays intact, and the end timestamp is not before the begin one (equality is allowed). This
 * validates query use and resolution, not clock precision.
 *
 * Requires the optional `TimestampQuery` feature; the runner reports it as `unsupported` otherwise.
 */
@AcidTest(
    id = AcidCaseId.QueriesTimestampResolve,
    family = AcidFamily.QueriesTimestamps,
    requiredFeatures = [GPUFeatureName.TimestampQuery],
    contract = [
        ApiSymbols.GPUDevice_createQuerySet,
        ApiSymbols.GPUQueryType_Timestamp,
        ApiSymbols.GPUComputePassDescriptor_timestampWrites,
        ApiSymbols.GPUCommandEncoder_resolveQuerySet,
    ],
)
suspend fun timestamps(device: GPUDevice) = withValidationScope(device) {
    device.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Timestamp, count = 2u)).use { querySet ->
        device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { output ->
            device.createBuffer(
                BufferDescriptor(
                    size = 512uL,
                    usage = GPUBufferUsage.QueryResolve or GPUBufferUsage.CopySrc,
                    mappedAtCreation = true,
                ),
            ).use { destination ->
                destination.getMappedRange().setBytes(0uL, ByteArray(512) { 0xFF.toByte() })
                destination.unmap()

                device.createBuffer(
                    BufferDescriptor(512uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
                ).use { staging ->
                    device.createShaderModule(ShaderModuleDescriptor(code = TIMED_COMPUTE_SHADER)).use { shader ->
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

                                        encoder.resolveQuerySet(querySet, 0u, 2u, destination, 256uL)
                                        encoder.copyBufferToBuffer(destination, 0uL, staging, 0uL, 512uL)
                                        encoder.finish().use { device.queue.submit(listOf(it)) }
                                    }
                                }
                            }
                        }
                    }

                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                    val bytes = try {
                        staging.getMappedRange().toByteArray().copyOf(512)
                    } finally {
                        staging.unmap()
                    }

                    assertContentEquals(
                        ByteArray(256) { 0xFF.toByte() },
                        bytes.copyOfRange(0, 256),
                        "Bytes before the resolve offset must keep the 0xff sentinel",
                    )
                    assertContentEquals(
                        ByteArray(240) { 0xFF.toByte() },
                        bytes.copyOfRange(272, 512),
                        "Bytes after the two resolved queries must keep the 0xff sentinel",
                    )

                    val words = ArrayBuffer.of(bytes).toUIntArray()
                    val beginLow = words[64]
                    val beginHigh = words[65]
                    val endLow = words[66]
                    val endHigh = words[67]
                    assertTrue(
                        !(beginLow == SENTINEL_WORD && beginHigh == SENTINEL_WORD),
                        "The begin timestamp must replace its 0xff sentinel",
                    )
                    assertTrue(
                        !(endLow == SENTINEL_WORD && endHigh == SENTINEL_WORD),
                        "The end timestamp must replace its 0xff sentinel",
                    )
                    assertTrue(
                        endHigh > beginHigh || (endHigh == beginHigh && endLow >= beginLow),
                        "The end timestamp must not be before the begin timestamp",
                    )
                }
            }
        }
    }
}
