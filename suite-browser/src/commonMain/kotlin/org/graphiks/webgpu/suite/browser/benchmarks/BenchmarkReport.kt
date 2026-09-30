package org.graphiks.webgpu.suite.browser.benchmarks

import kotlinx.serialization.Serializable
import org.graphiks.webgpu.suite.benchmarks.BenchmarkProfile
import org.graphiks.webgpu.suite.benchmarks.ComputeBatches
import org.graphiks.webgpu.suite.benchmarks.ComputeElementCounts
import org.graphiks.webgpu.suite.benchmarks.TransferBatches
import org.graphiks.webgpu.suite.benchmarks.TransferSizeBytes

/** The size unit of a workload: bytes for the transfer, u32 elements for compute. */
internal enum class SizeUnit(val id: String) {
    Bytes("bytes"),
    Elements("elements"),
}

/** One deterministic scenario of the foundations-v1 protocol. */
internal data class BenchmarkScenario(
    val id: String,
    val workload: String,
    val sizeUnit: SizeUnit,
    val size: Int,
    val operationsPerSample: Int,
)

/**
 * The ten scenarios in their published order: transfer sizes ascending, then compute sizes
 * ascending, and for each size the batch of 1 before the batch of 16. The sizes and batches come
 * from the same lists the portable functions validate against, so no scenario can drift out of the
 * protocol they enforce.
 */
internal fun benchmarkScenarios(): List<BenchmarkScenario> = buildList {
    for (size in TransferSizeBytes) {
        for (batch in TransferBatches) {
            add(
                BenchmarkScenario(
                    id = "transfer.write-buffer.bytes-$size.batch-$batch",
                    workload = "transfer.write-buffer",
                    sizeUnit = SizeUnit.Bytes,
                    size = size,
                    operationsPerSample = batch,
                ),
            )
        }
    }
    for (size in ComputeElementCounts) {
        for (batch in ComputeBatches) {
            add(
                BenchmarkScenario(
                    id = "compute.encode-submit.elements-$size.batch-$batch",
                    workload = "compute.encode-submit",
                    sizeUnit = SizeUnit.Elements,
                    size = size,
                    operationsPerSample = batch,
                ),
            )
        }
    }
}

/** The lowercase profile name used by the route, the report and the collector. */
internal fun BenchmarkProfile.id(): String = when (this) {
    BenchmarkProfile.Standard -> "standard"
    BenchmarkProfile.Ci -> "ci"
}

/** One retained sample, in milliseconds, as published in the raw report. */
@Serializable
internal data class BenchmarkSample(
    val cpuIssueMs: Double,
    val completionMs: Double,
)

/**
 * The outcome of one scenario.
 *
 * `status` is one of `completed`, `failed`, `interrupted` or `not-run`. Only a `completed`
 * scenario has `outputVerified = true` and a non-empty `samples` list that may be aggregated; the
 * other statuses carry a diagnostic. The zeros of the clock are kept: [zeroCpuSamples] counts the
 * samples whose `cpuIssueMs` was exactly zero, which the page reports as a low-resolution limit of
 * the environment rather than hiding the sample.
 */
@Serializable
internal data class BenchmarkScenarioResult(
    val id: String,
    val workload: String,
    val sizeUnit: String,
    val size: Int,
    val operationsPerSample: Int,
    val profile: String,
    val warmups: Int,
    val plannedSamples: Int,
    val samples: List<BenchmarkSample>,
    val zeroCpuSamples: Int,
    val status: String,
    val diagnostic: String? = null,
    val outputVerified: Boolean,
    val adapterDescription: String? = null,
    val isFallbackAdapter: Boolean? = null,
    val features: List<String> = emptyList(),
    val limitsUsed: Map<String, String> = emptyMap(),
)

/** The report the browser publishes in `globalThis.graphiksBenchmarkReport`. */
@Serializable
internal data class BenchmarkReport(
    val schemaVersion: Int = 1,
    val buildCommit: String,
    val buildVersion: String,
    val protocol: String = "foundations-v1",
    val profile: String,
    val clock: String = "kotlin.time.TimeSource.Monotonic",
    val scenarios: List<BenchmarkScenarioResult>,
    val fatalError: String? = null,
)
