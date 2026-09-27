package org.graphiks.webgpu.suite.browser

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.AcidCase
import org.graphiks.webgpu.suite.acid.foundationCases

/**
 * Runs every foundation case, each on a fresh adapter and device.
 *
 * Cases run sequentially: `requestDevice` consumes its adapter, so the runner never asks one
 * adapter for two devices. A case that times out or throws is reported as `failed`; a missing
 * optional feature is reported as `unsupported` without running the case.
 */
suspend fun runFoundations(): BrowserReport {
    val results = mutableListOf<CaseResult>()
    for (case in foundationCases()) {
        val result = try {
            withTimeout(30.seconds) { runCase(case) }
        } catch (timeout: TimeoutCancellationException) {
            CaseResult(case.id, "failed", "Timed out after 30 seconds")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            CaseResult(case.id, "failed", failure.stackTraceToString())
        }
        results += result
    }
    return BrowserReport(cases = results)
}

private suspend fun runCase(case: AcidCase): CaseResult {
    val adapter = requestAdapter().getOrThrow()
    try {
        val missing = case.requiredFeatures - adapter.features
        if (missing.isNotEmpty()) {
            return CaseResult(case.id, "unsupported", "Missing optional features: $missing")
        }

        val uncapturedErrors = mutableListOf<String>()
        val device = adapter.requestDevice(
            DeviceDescriptor(
                requiredFeatures = case.requiredFeatures.toList(),
                onUncapturedError = GPUUncapturedErrorCallback { uncapturedErrors.add(it.message) },
            ),
        ).getOrThrow()
        try {
            case.run(device)
            device.queue.onSubmittedWorkDone().getOrThrow()
            delay(50) // let the browser deliver pending uncaptured-error callbacks
            check(uncapturedErrors.isEmpty()) { uncapturedErrors.joinToString("\n") }
            return CaseResult(case.id, "passed", adapterDescription = adapter.info.description)
        } finally {
            device.close()
        }
    } finally {
        adapter.close()
    }
}
