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
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.acid.SuiteBuildIdentity
import org.graphiks.webgpu.suite.acid.foundationCases

/**
 * Runs the foundation catalogue, each case on a fresh adapter and device.
 *
 * [caseIds] selects an explicit subset for targeted development, keeping the requested order of the
 * catalogue. `null` (the default) runs every case. An explicit selection must be non-empty and name
 * only known case ids; an unknown id or an empty selection fails before any case runs, so a typo is
 * never silently executed as a full catalogue.
 *
 * [onProgress] is called just before each case runs, with the 1-based number of the case now
 * starting, the number of selected cases and the case id, so the page can show a progress line.
 *
 * Cases run sequentially: `requestDevice` consumes its adapter, so the runner never asks one
 * adapter for two devices. A case that times out or throws is reported as `failed`; a missing
 * optional feature is reported as `unsupported` without running the case.
 */
suspend fun runFoundations(
    caseIds: Set<String>? = null,
    onProgress: (current: Int, total: Int, id: String) -> Unit = { _, _, _ -> },
): BrowserReport {
    val catalogue = foundationCases()
    val selected = if (caseIds == null) {
        catalogue
    } else {
        val known = catalogue.mapTo(mutableSetOf()) { it.id.id }
        val unknown = (caseIds - known).sorted()
        require(unknown.isEmpty()) { "Unknown case id(s): ${unknown.joinToString(", ")}" }
        require(caseIds.isNotEmpty()) { "An explicit case selection cannot be empty." }
        catalogue.filter { it.id.id in caseIds }
    }

    val results = mutableListOf<CaseResult>()
    for ((index, case) in selected.withIndex()) {
        onProgress(index + 1, selected.size, case.id.id)
        val result = try {
            withTimeout(30.seconds) { runCase(case) }
        } catch (timeout: TimeoutCancellationException) {
            CaseResult(case.id.id, "failed", "Timed out after 30 seconds")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            CaseResult(case.id.id, "failed", failure.stackTraceToString())
        }
        results += result
    }
    return BrowserReport(
        buildCommit = SuiteBuildIdentity.COMMIT,
        buildVersion = SuiteBuildIdentity.VERSION,
        cases = results,
    )
}

private suspend fun runCase(case: AcidCase): CaseResult {
    val adapter = requestAdapter().getOrThrow()
    try {
        val missing = (case.requiredFeatures - adapter.features).map { it.name }.sorted()
        if (missing.isNotEmpty()) {
            return CaseResult(
                id = case.id.id,
                status = "unsupported",
                diagnostic = "Missing optional features: ${missing.joinToString(", ")}",
                missingFeatures = missing,
            )
        }

        val uncapturedErrors = mutableListOf<String>()
        val device = adapter.requestDevice(
            DeviceDescriptor(
                requiredFeatures = case.requiredFeatures.toList(),
                onUncapturedError = GPUUncapturedErrorCallback { uncapturedErrors.add(it.message) },
            ),
        ).getOrThrow()
        try {
            // The adapter pre-check above already refused a missing requested feature; this guards
            // against a binding that returns a device which silently dropped one of them.
            val missingOnDevice = (case.requiredFeatures - device.features).map { it.name }.sorted()
            check(missingOnDevice.isEmpty()) {
                "Device is missing required features: ${missingOnDevice.joinToString(", ")}"
            }

            // A fresh adapter per call: a context case that needs its own device never has to share
            // the runner's consumed adapter, and the borrowed device is never closed by the case.
            val context = AcidContext(
                device = device,
                requestAdapter = { options -> requestAdapter(options).map { it } },
            )
            case.run(context)
            device.queue.onSubmittedWorkDone().getOrThrow()
            delay(50) // let the browser deliver pending uncaptured-error callbacks
            check(uncapturedErrors.isEmpty()) { uncapturedErrors.joinToString("\n") }
            return CaseResult(case.id.id, "passed", adapterDescription = adapter.info.description)
        } finally {
            device.close()
        }
    } finally {
        adapter.close()
    }
}
