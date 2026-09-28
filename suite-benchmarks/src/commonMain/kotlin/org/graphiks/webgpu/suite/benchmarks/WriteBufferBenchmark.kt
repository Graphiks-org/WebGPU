package org.graphiks.webgpu.suite.benchmarks

import kotlin.time.TimeSource
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor

/** The multiplier that makes each word depend on both its index and its write. */
private const val XorConstant = 0x9e3779b9u

/** The u32 written at word [index] by write [writeIndex] of a batch. */
private fun transferWord(index: Int, writeIndex: Int): UInt =
    index.toUInt() xor (XorConstant + writeIndex.toUInt())

/**
 * Measures `queue.writeBuffer` for the foundations-v1 protocol.
 *
 * The target buffer and the CPU-side payloads are prepared once, outside every measurement. Each
 * sample drains the queue, then times the synchronous `writeBuffer` calls of one batch
 * (`cpuIssueMs`) and the wait for completion (`completionMs`). Warm-ups run the same code and are
 * discarded. The full buffer is read back and checked once before the warm-ups and once after the
 * last sample: a mismatch fails the scenario, so no duration is ever returned for a transfer that
 * produced the wrong memory. The function owns and closes its buffers; it never closes [device].
 * All writes target the same buffer on purpose, so the last write of a batch is the verified value.
 */
suspend fun benchmarkWriteBuffer(
    device: GPUDevice,
    sizeBytes: Int,
    writes: Int,
    profile: BenchmarkProfile,
): BenchmarkResult {
    require(sizeBytes in TransferSizeBytes) {
        "The transfer size must be one of $TransferSizeBytes bytes, was $sizeBytes."
    }
    require(writes in TransferBatches) {
        "The transfer batch must be one of $TransferBatches writes, was $writes."
    }
    require(sizeBytes.toULong() <= device.limits.maxBufferSize) {
        "The transfer size $sizeBytes exceeds the device limit ${device.limits.maxBufferSize}."
    }

    val input = List(writes) { writeIndex ->
        ArrayBuffer.of(UIntArray(sizeBytes / 4) { index -> transferWord(index, writeIndex) })
    }

    val target = device.createBuffer(
        BufferDescriptor(
            size = sizeBytes.toULong(),
            usage = GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc,
        ),
    )
    try {
        val staging = device.createBuffer(
            BufferDescriptor(
                size = sizeBytes.toULong(),
                usage = GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst,
            ),
        )
        try {
            writeAll(device, target, input)
            verifyTransfer(device, target, staging, sizeBytes, writes)

            val samples = mutableListOf<TimingSample>()
            for (run in 0 until profile.warmups + profile.samples) {
                device.queue.onSubmittedWorkDone().getOrThrow()
                val start = TimeSource.Monotonic.markNow()
                writeAll(device, target, input)
                val cpuIssueMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
                device.queue.onSubmittedWorkDone().getOrThrow()
                val completionMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
                if (run >= profile.warmups) {
                    samples += TimingSample(cpuIssueMs, completionMs)
                }
            }

            verifyTransfer(device, target, staging, sizeBytes, writes)

            return BenchmarkResult(
                workload = "transfer.write-buffer",
                size = sizeBytes,
                operationsPerSample = writes,
                profile = profile,
                samples = samples,
            )
        } finally {
            staging.close()
        }
    } finally {
        target.close()
    }
}

/** Writes every payload of a batch into the same target buffer; the last write wins. */
private fun writeAll(device: GPUDevice, target: GPUBuffer, input: List<ArrayBuffer>) {
    for (data in input) {
        device.queue.writeBuffer(target, 0uL, data)
    }
}

/**
 * Reads the whole target back and checks every word against the last write of a batch.
 *
 * The expected value is computed from the index and the write, never read from a previous result.
 * A difference invalidates the whole scenario, and the diagnostic names the word and both values.
 */
private suspend fun verifyTransfer(
    device: GPUDevice,
    target: GPUBuffer,
    staging: GPUBuffer,
    sizeBytes: Int,
    writes: Int,
) {
    val expectedXor = XorConstant + (writes - 1).toUInt()
    val actual = readUints(device, target, staging, sizeBytes.toULong())
    for (index in actual.indices) {
        val expected = index.toUInt() xor expectedXor
        check(actual[index] == expected) {
            "Transfer readback mismatch at word $index: expected ${expected.toString(16)}, " +
                "observed ${actual[index].toString(16)}."
        }
    }
}
