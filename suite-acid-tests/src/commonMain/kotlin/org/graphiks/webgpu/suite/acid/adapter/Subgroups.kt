package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
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
import kotlin.test.assertTrue

private const val SUBGROUP_SHADER = """
enable subgroups;

@group(0) @binding(0) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(64)
fn main(@builtin(global_invocation_id) gid: vec3u, @builtin(subgroup_id) sg: u32) {
    let size = subgroupAdd(1u);
    out[2u * gid.x] = size;
    out[2u * gid.x + 1u] = sg;
}
"""

/**
 * The optional `Subgroups` feature is exercised end to end: the granted device reports it, its
 * adapter identity exposes the subgroup size bounds, and a 64-invocation workgroup runs a
 * `subgroupAdd` whose result — the subgroup's own size — is read back per invocation. Every sum
 * lies within the reported bounds, invocations of the same subgroup agree on it, and the number of
 * distinct subgroups fits the bounds' arithmetic.
 *
 * Requires the optional `Subgroups` feature; the runner reports it as `unsupported` otherwise.
 */
@AcidTest(
    id = AcidCaseId.FeaturesSubgroups,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    requiredFeatures = [GPUFeatureName.Subgroups],
    contract = [
        ApiSymbols.GPUDevice_features,
        ApiSymbols.GPUFeatureName_Subgroups,
        ApiSymbols.GPUDevice_adapterInfo,
        ApiSymbols.GPUAdapterInfo_subgroupMinSize,
        ApiSymbols.GPUAdapterInfo_subgroupMaxSize,
        ApiSymbols.GPUBufferUsage_Storage,
        ApiSymbols.GPUBufferUsage_CopySrc,
    ],
)
suspend fun subgroups(device: GPUDevice) = withValidationScope(device) {
    assertTrue(
        GPUFeatureName.Subgroups in device.features,
        "A device granted for this case must report Subgroups",
    )
    val minSize = device.adapterInfo.subgroupMinSize
    val maxSize = device.adapterInfo.subgroupMaxSize
    assertTrue(minSize >= 1u, "subgroupMinSize must be at least 1, observed $minSize")
    assertTrue(minSize <= maxSize, "subgroupMinSize $minSize must not exceed subgroupMaxSize $maxSize")

    device.createBuffer(
        BufferDescriptor(512uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
    ).use { output ->
        device.createShaderModule(ShaderModuleDescriptor(code = SUBGROUP_SHADER)).use { shader ->
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
                            pass.dispatchWorkgroups(1u)
                            pass.end()
                            encoder.finish().use { device.queue.submit(listOf(it)) }
                        }
                    }
                }
            }
        }

        val words = ArrayBuffer.of(readBufferBytes(device, output, 512uL)).toUIntArray()
        val sumBySubgroup = mutableMapOf<UInt, UInt>()
        for (invocation in 0 until 64) {
            val sum = words[2 * invocation]
            val subgroup = words[2 * invocation + 1]
            assertTrue(
                sum >= minSize && sum <= maxSize,
                "Invocation $invocation: subgroupAdd(1) must land within [$minSize, $maxSize], observed $sum",
            )
            val previous = sumBySubgroup.put(subgroup, sum)
            assertTrue(
                previous == null || previous == sum,
                "Subgroup $subgroup reported the sums $previous and $sum from different invocations",
            )
        }
        val distinct = sumBySubgroup.size.toUInt()
        assertTrue(
            distinct >= (64u + maxSize - 1u) / maxSize,
            "A 64-invocation workgroup needs at least ${(64u + maxSize - 1u) / maxSize} subgroups of at most $maxSize, observed $distinct",
        )
        assertTrue(
            distinct <= (64u + minSize - 1u) / minSize,
            "A 64-invocation workgroup cannot exceed ${(64u + minSize - 1u) / minSize} subgroups of at least $minSize, observed $distinct",
        )
    }
}
