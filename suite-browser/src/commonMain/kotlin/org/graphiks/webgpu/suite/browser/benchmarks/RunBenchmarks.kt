package org.graphiks.webgpu.suite.browser.benchmarks

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.graphiks.webgpu.GPUAdapterInfo
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.benchmarks.BenchmarkProfile
import org.graphiks.webgpu.suite.benchmarks.BenchmarkResult
import org.graphiks.webgpu.suite.benchmarks.TimingSample
import org.graphiks.webgpu.suite.benchmarks.benchmarkCompute
import org.graphiks.webgpu.suite.benchmarks.benchmarkWriteBuffer

/** Real wall-clock budget of one scenario, including adapter and device acquisition. */
internal const val ScenarioTimeoutSeconds = 90L

/** Real wall-clock budget of a whole campaign. */
internal const val CampaignTimeoutMinutes = 16L

/**
 * Runs the ten foundations-v1 scenarios and returns their report.
 *
 * Scenarios run in their published order, each on its own adapter and device, sequentially. Each
 * scenario is bounded by [ScenarioTimeoutSeconds] and the campaign by [CampaignTimeoutMinutes];
 * both are real time, never a virtual clock. A failing or timed-out scenario is recorded and the
 * next one runs; a user cancellation or a hidden tab stops the campaign, marks the in-flight
 * scenario `interrupted` and leaves the rest `not-run`. [onProgress] is called between scenarios
 * only, never inside a measurement window.
 */
internal suspend fun runBenchmarks(
    profile: BenchmarkProfile,
    onProgress: (completed: Int, total: Int, scenarioId: String) -> Unit = { _, _, _ -> },
): BenchmarkReport {
    val scenarios = benchmarkScenarios()
    val results = arrayOfNulls<BenchmarkScenarioResult>(scenarios.size)
    var currentIndex = 0
    var campaignTimedOut = false
    try {
        withTimeout(CampaignTimeoutMinutes.minutes) {
            for ((index, scenario) in scenarios.withIndex()) {
                currentIndex = index
                onProgress(index, scenarios.size, scenario.id)
                results[index] = runScenarioSafely(profile, scenario)
            }
        }
    } catch (timeout: TimeoutCancellationException) {
        campaignTimedOut = true
    } catch (cancelled: CancellationException) {
        // The user cancelled or the tab was hidden: keep the outcome that exists and stop.
    }

    val reported = scenarios.mapIndexed { index, scenario ->
        results[index] ?: when {
            index == currentIndex && campaignTimedOut ->
                scenarioResult(
                    scenario,
                    profile,
                    status = "failed",
                    diagnostic = "The campaign timed out after $CampaignTimeoutMinutes minutes.",
                )
            index == currentIndex ->
                scenarioResult(
                    scenario,
                    profile,
                    status = "interrupted",
                    diagnostic = "The campaign was cancelled or the tab was hidden.",
                )
            else -> scenarioResult(scenario, profile, status = "not-run")
        }
    }
    return BenchmarkReport(profile = profile.id(), scenarios = reported)
}

/** Bounds one scenario by the real timeout; records the failure and never rethrows it. */
private suspend fun runScenarioSafely(
    profile: BenchmarkProfile,
    scenario: BenchmarkScenario,
): BenchmarkScenarioResult = try {
    // withTimeoutOrNull returns null only for its own timeout; a campaign cancellation is a
    // foreign CancellationException and is rethrown, so the two cases stay distinct.
    val outcome = withTimeoutOrNull(ScenarioTimeoutSeconds.seconds) { runScenario(profile, scenario) }
    outcome ?: scenarioResult(
        scenario,
        profile,
        status = "failed",
        diagnostic = "Timed out after $ScenarioTimeoutSeconds seconds.",
    )
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    scenarioResult(scenario, profile, status = "failed", diagnostic = failure.stackTraceToString())
}

/**
 * Runs one scenario on a fresh adapter and device.
 *
 * A Validation scope and an uncaptured-error callback cover preparation, warm-ups, measurements
 * and readbacks, not only the measured code. The queue is drained and the callback is given a
 * moment to deliver before the scenario is accepted. Any diagnostic, scope error or readback
 * failure makes the scenario `failed`; the caller continues with the next one.
 */
private suspend fun runScenario(
    profile: BenchmarkProfile,
    scenario: BenchmarkScenario,
): BenchmarkScenarioResult {
    val adapter = requestAdapter().getOrThrow()
    try {
        val uncaptured = mutableListOf<String>()
        val device = adapter.requestDevice(
            DeviceDescriptor(
                onUncapturedError = GPUUncapturedErrorCallback { uncaptured.add(it.message) },
            ),
        ).getOrThrow()
        try {
            val info = adapter.info
            var outcome: BenchmarkResult? = null
            var failure: Throwable? = null
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                outcome = runWorkload(device, scenario, profile)
                device.queue.onSubmittedWorkDone().getOrThrow()
                delay(50) // let the browser deliver pending uncaptured-error callbacks
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (thrown: Throwable) {
                failure = thrown
            }

            var scopeError: String? = null
            try {
                device.popErrorScope().getOrThrow()?.let { scopeError = it.message }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (thrown: Throwable) {
                if (failure == null) failure = thrown
            }

            val diagnostic = pickDiagnostic(failure, scopeError, uncaptured)
            if (diagnostic != null || outcome == null) {
                return scenarioResult(
                    scenario,
                    profile,
                    status = "failed",
                    diagnostic = diagnostic ?: "No result was produced.",
                    info = info,
                    device = device,
                )
            }
            validateSamples(outcome.samples)
            return scenarioResult(
                scenario,
                profile,
                status = "completed",
                samples = outcome.samples.map { sample -> BenchmarkSample(sample.cpuIssueMs, sample.completionMs) },
                zeroCpuSamples = outcome.samples.count { sample -> sample.cpuIssueMs == 0.0 },
                outputVerified = true,
                info = info,
                device = device,
            )
        } finally {
            device.close()
        }
    } finally {
        adapter.close()
    }
}

/** Runs the workload the scenario names; an unknown name is a programming error, not a skip. */
private suspend fun runWorkload(
    device: GPUDevice,
    scenario: BenchmarkScenario,
    profile: BenchmarkProfile,
): BenchmarkResult = when (scenario.workload) {
    "transfer.write-buffer" ->
        benchmarkWriteBuffer(device, scenario.size, scenario.operationsPerSample, profile)
    "compute.encode-submit" ->
        benchmarkCompute(device, scenario.size, scenario.operationsPerSample, profile)
    else -> error("Unknown benchmark workload '${scenario.workload}'.")
}

/** Prefers the work failure, then the validation-scope error, then an uncaptured error. */
private fun pickDiagnostic(
    failure: Throwable?,
    scopeError: String?,
    uncaptured: List<String>,
): String? = when {
    failure != null -> failure.stackTraceToString()
    scopeError != null -> "Validation error: $scopeError"
    uncaptured.isNotEmpty() -> uncaptured.joinToString("\n")
    else -> null
}

/**
 * Rejects a sample only when a duration is negative or not finite.
 *
 * Zero and repeated values are real observations and are kept; a negative or NaN duration would be
 * a broken clock, not a measurement.
 */
private fun validateSamples(samples: List<TimingSample>) {
    for ((index, sample) in samples.withIndex()) {
        check(sample.cpuIssueMs.isFinite() && sample.cpuIssueMs >= 0.0) {
            "Sample $index has a non-finite or negative cpuIssueMs: ${sample.cpuIssueMs}."
        }
        check(sample.completionMs.isFinite() && sample.completionMs >= 0.0) {
            "Sample $index has a non-finite or negative completionMs: ${sample.completionMs}."
        }
    }
}

/**
 * Builds one scenario result, filling the protocol fields every status shares.
 *
 * Adapter and device details are only present when the device was acquired; a timeout before that
 * leaves them empty rather than inventing a description.
 */
private fun scenarioResult(
    scenario: BenchmarkScenario,
    profile: BenchmarkProfile,
    status: String,
    samples: List<BenchmarkSample> = emptyList(),
    zeroCpuSamples: Int = 0,
    diagnostic: String? = null,
    outputVerified: Boolean = false,
    info: GPUAdapterInfo? = null,
    device: GPUDevice? = null,
): BenchmarkScenarioResult = BenchmarkScenarioResult(
    id = scenario.id,
    workload = scenario.workload,
    sizeUnit = scenario.sizeUnit.id,
    size = scenario.size,
    operationsPerSample = scenario.operationsPerSample,
    profile = profile.id(),
    warmups = profile.warmups,
    plannedSamples = profile.samples,
    samples = samples,
    zeroCpuSamples = zeroCpuSamples,
    status = status,
    diagnostic = diagnostic,
    outputVerified = outputVerified,
    adapterDescription = info?.description?.ifEmpty { null },
    isFallbackAdapter = info?.isFallbackAdapter,
    features = device?.features?.map { feature -> feature.value }?.sorted() ?: emptyList(),
    limitsUsed = device?.let { usedLimits(it, scenario.workload) } ?: emptyMap(),
)

/** The device limits the workload's validation actually consulted, named for the report. */
private fun usedLimits(device: GPUDevice, workload: String): Map<String, String> {
    val limits = device.limits
    return when (workload) {
        "transfer.write-buffer" -> mapOf(
            "maxBufferSize" to limits.maxBufferSize.toString(),
        )
        "compute.encode-submit" -> mapOf(
            "maxBufferSize" to limits.maxBufferSize.toString(),
            "maxStorageBufferBindingSize" to limits.maxStorageBufferBindingSize.toString(),
            "maxComputeWorkgroupsPerDimension" to limits.maxComputeWorkgroupsPerDimension.toString(),
            "maxComputeWorkgroupSizeX" to limits.maxComputeWorkgroupSizeX.toString(),
            "maxComputeInvocationsPerWorkgroup" to limits.maxComputeInvocationsPerWorkgroup.toString(),
        )
        else -> emptyMap()
    }
}
