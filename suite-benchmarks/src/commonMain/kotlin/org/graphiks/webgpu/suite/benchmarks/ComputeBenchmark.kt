package org.graphiks.webgpu.suite.benchmarks

import kotlin.time.TimeSource
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUCommandBuffer
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor

/** One workgroup covers 64 invocations, matching the shader's `@workgroup_size`. */
private const val WorkgroupSize = 64

/**
 * The compute load: each invocation writes one deterministic u32. The bounds check keeps a
 * partial final workgroup from writing past the buffer, and the values depend only on the index,
 * so the readback can be checked without trusting a previous run.
 */
private const val ComputeShader = """
@group(0) @binding(0) var<storage, read_write> output: array<u32>;

@compute @workgroup_size(64)
fn main(@builtin(global_invocation_id) id: vec3u) {
    if (id.x >= arrayLength(&output)) {
        return;
    }
    output[id.x] = id.x * 3u + 7u;
}
"""

/** The u32 the compute shader writes at element [index]. */
private fun computeWord(index: Int): UInt = index.toUInt() * 3u + 7u

/**
 * Measures the encoding and submission of a compute load for the foundations-v1 protocol.
 *
 * The shader, pipeline, bind group, output buffer and staging buffer are prepared once, outside
 * every measurement. Each sample drains the queue, then times one encoder that records
 * [dispatches] dispatches over the whole element count, submits it and waits for completion
 * (`cpuIssueMs`), then waits for `queue.onSubmittedWorkDone()` (`completionMs`). Warm-ups run the
 * same code and are discarded. The output is read back and checked once before the warm-ups and
 * once after the last sample, so a duration is only returned for a load that produced the expected
 * memory. The function owns and closes its resources; it never closes [device]. The dispatches all
 * write the same values: this is a simple load, not a model of a general GPU application.
 */
suspend fun benchmarkCompute(
    device: GPUDevice,
    elements: Int,
    dispatches: Int,
    profile: BenchmarkProfile,
): BenchmarkResult {
    require(elements in ComputeElementCounts) {
        "The compute size must be one of $ComputeElementCounts elements, was $elements."
    }
    require(dispatches in ComputeBatches) {
        "The compute batch must be one of $ComputeBatches dispatches, was $dispatches."
    }

    val byteSize = elements.toULong() * 4uL
    val workgroups = ((elements + WorkgroupSize - 1) / WorkgroupSize).toUInt()
    val limits = device.limits
    require(byteSize <= limits.maxBufferSize) {
        "The compute size $byteSize bytes exceeds the device limit ${limits.maxBufferSize}."
    }
    require(byteSize <= limits.maxStorageBufferBindingSize) {
        "The compute size $byteSize bytes exceeds the storage binding limit ${limits.maxStorageBufferBindingSize}."
    }
    require(workgroups <= limits.maxComputeWorkgroupsPerDimension) {
        "The compute dispatch of $workgroups workgroups exceeds the device limit ${limits.maxComputeWorkgroupsPerDimension}."
    }
    require(WorkgroupSize.toUInt() <= limits.maxComputeWorkgroupSizeX) {
        "The workgroup size $WorkgroupSize exceeds the device limit ${limits.maxComputeWorkgroupSizeX}."
    }
    require(WorkgroupSize.toUInt() <= limits.maxComputeInvocationsPerWorkgroup) {
        "The workgroup size $WorkgroupSize exceeds the device limit ${limits.maxComputeInvocationsPerWorkgroup}."
    }

    val shader = device.createShaderModule(ShaderModuleDescriptor(code = ComputeShader))
    try {
        val pipeline = device.createComputePipeline(
            ComputePipelineDescriptor(compute = ProgrammableStage(shader, entryPoint = "main")),
        )
        try {
            val layout = pipeline.getBindGroupLayout(0u)
            try {
                val output = device.createBuffer(
                    BufferDescriptor(
                        size = byteSize,
                        usage = GPUBufferUsage.Storage or GPUBufferUsage.CopySrc,
                    ),
                )
                try {
                    val staging = device.createBuffer(
                        BufferDescriptor(
                            size = byteSize,
                            usage = GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst,
                        ),
                    )
                    try {
                        val group = device.createBindGroup(
                            BindGroupDescriptor(
                                layout = layout,
                                entries = listOf(BindGroupEntry(0u, BufferBinding(output))),
                            ),
                        )
                        try {
                            submitCompute(device, pipeline, group, workgroups, dispatches)
                            verifyCompute(device, output, staging, elements)

                            val samples = mutableListOf<TimingSample>()
                            for (run in 0 until profile.warmups + profile.samples) {
                                device.queue.onSubmittedWorkDone().getOrThrow()
                                var cpuIssueMs = 0.0
                                var completionMs = 0.0
                                val start = TimeSource.Monotonic.markNow()
                                val encoder = device.createCommandEncoder()
                                var commands: GPUCommandBuffer? = null
                                try {
                                    val pass = encoder.beginComputePass()
                                    pass.setPipeline(pipeline)
                                    pass.setBindGroup(0u, group)
                                    repeat(dispatches) { pass.dispatchWorkgroups(workgroups) }
                                    pass.end()
                                    val finished = encoder.finish()
                                    commands = finished
                                    device.queue.submit(listOf(finished))
                                    cpuIssueMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
                                    device.queue.onSubmittedWorkDone().getOrThrow()
                                    completionMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
                                } finally {
                                    commands?.close()
                                    encoder.close()
                                }
                                if (run >= profile.warmups) {
                                    samples += TimingSample(cpuIssueMs, completionMs)
                                }
                            }

                            verifyCompute(device, output, staging, elements)

                            return BenchmarkResult(
                                workload = "compute.encode-submit",
                                size = elements,
                                operationsPerSample = dispatches,
                                profile = profile,
                                samples = samples,
                            )
                        } finally {
                            group.close()
                        }
                    } finally {
                        staging.close()
                    }
                } finally {
                    output.close()
                }
            } finally {
                layout.close()
            }
        } finally {
            pipeline.close()
        }
    } finally {
        shader.close()
    }
}

/** Encodes and submits [dispatches] compute dispatches; used for the untimed control run. */
private fun submitCompute(
    device: GPUDevice,
    pipeline: GPUComputePipeline,
    group: GPUBindGroup,
    workgroups: UInt,
    dispatches: Int,
) {
    val encoder = device.createCommandEncoder()
    var commands: GPUCommandBuffer? = null
    try {
        val pass = encoder.beginComputePass()
        pass.setPipeline(pipeline)
        pass.setBindGroup(0u, group)
        repeat(dispatches) { pass.dispatchWorkgroups(workgroups) }
        pass.end()
        val finished = encoder.finish()
        commands = finished
        device.queue.submit(listOf(finished))
    } finally {
        commands?.close()
        encoder.close()
    }
}

/**
 * Reads the whole output back and checks every element against `index * 3 + 7`.
 *
 * The expected value is computed from the index, never read from a previous result. A difference
 * invalidates the whole scenario, and the diagnostic names the element and both values.
 */
private suspend fun verifyCompute(
    device: GPUDevice,
    output: GPUBuffer,
    staging: GPUBuffer,
    elements: Int,
) {
    val actual = readUints(device, output, staging, elements.toULong() * 4uL)
    for (index in actual.indices) {
        val expected = computeWord(index)
        check(actual[index] == expected) {
            "Compute readback mismatch at element $index: expected $expected, observed ${actual[index]}."
        }
    }
}
