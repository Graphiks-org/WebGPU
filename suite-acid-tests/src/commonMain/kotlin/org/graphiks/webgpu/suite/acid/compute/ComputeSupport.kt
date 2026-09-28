package org.graphiks.webgpu.suite.acid.compute

import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import kotlin.test.assertContentEquals

/**
 * A deterministic compute shader: each invocation writes one word scaled by the
 * pipeline-overridable constant `scale`.
 */
internal const val SCALE_SHADER = """
override scale: u32 = 1u;
@group(0) @binding(0) var<storage, read_write> output: array<u32>;
@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    output[id.x] = (id.x + 1u) * scale;
}
"""

internal fun encodeScaleDispatch(
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

internal suspend fun readBackStaging(device: GPUDevice, staging: GPUBuffer, expected: UIntArray) {
    staging.mapAsync(GPUMapMode.Read).getOrThrow()
    try {
        assertContentEquals(expected, staging.getMappedRange().toUIntArray())
    } finally {
        staging.unmap()
    }
}
