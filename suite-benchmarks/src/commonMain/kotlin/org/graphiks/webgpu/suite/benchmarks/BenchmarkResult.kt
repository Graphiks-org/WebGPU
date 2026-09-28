package org.graphiks.webgpu.suite.benchmarks

/**
 * The two measurement profiles of the foundations-v1 protocol.
 *
 * Both profiles run the same workloads at the same sizes and batch sizes; only the number of
 * discarded warm-ups and of retained samples changes. [Ci] is a working check with a few
 * observations, not a representative statistical campaign.
 */
enum class BenchmarkProfile(val warmups: Int, val samples: Int) {
    Standard(5, 30),
    Ci(3, 5),
}

/**
 * One measured batch, in milliseconds, as two durations read from the same monotonic clock.
 *
 * `cpuIssueMs` stops when the GPU calls have returned synchronously (submission included for
 * compute); `completionMs` stops when `queue.onSubmittedWorkDone()` resolves. Both are durations
 * for the whole batch, never individual latencies and never pure GPU time.
 */
data class TimingSample(val cpuIssueMs: Double, val completionMs: Double)

/**
 * The retained measurements of one completed scenario.
 *
 * [size] is in bytes for the transfer workload and in u32 elements for the compute workload,
 * following the workload's unit. [operationsPerSample] is the number of writes or dispatches
 * encoded per measured batch. A returned result means the scenario finished and its GPU
 * readbacks matched its expected values; a failure throws and is reported by the runner instead.
 */
data class BenchmarkResult(
    val workload: String,
    val size: Int,
    val operationsPerSample: Int,
    val profile: BenchmarkProfile,
    val samples: List<TimingSample>,
)

/** The transfer sizes of the foundations-v1 protocol, in bytes. */
val TransferSizeBytes = listOf(4 * 1024, 64 * 1024, 1024 * 1024)

/** The allowed number of writes encoded per measured transfer batch. */
val TransferBatches = listOf(1, 16)

/** The compute sizes of the foundations-v1 protocol, in u32 elements. */
val ComputeElementCounts = listOf(1024, 64 * 1024)

/** The allowed number of dispatches encoded per measured compute batch. */
val ComputeBatches = listOf(1, 16)
