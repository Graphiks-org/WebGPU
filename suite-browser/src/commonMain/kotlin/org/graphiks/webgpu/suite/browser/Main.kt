package org.graphiks.webgpu.suite.browser

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.suite.acid.foundationCases
import org.graphiks.webgpu.suite.browser.demos.BoundsPauseResetId
import org.graphiks.webgpu.suite.browser.demos.ComputeRenderReadbackId
import org.graphiks.webgpu.suite.browser.demos.DemoReport
import org.graphiks.webgpu.suite.browser.demos.elementById
import org.graphiks.webgpu.suite.browser.demos.particleGpuResults
import org.graphiks.webgpu.suite.browser.demos.queryParameter
import org.graphiks.webgpu.suite.browser.demos.selectedLocale
import org.graphiks.webgpu.suite.browser.demos.showParticlesPage

/**
 * The page has two explicit routes: without a `demo` parameter it runs the foundation validation
 * suite exactly as before and publishes `graphiksSuiteReport`; `?demo=particles` runs the interactive
 * particle demo, and `?demo=particles&verify=1` runs its GPU checks and publishes a separate
 * `graphiksDemoReport`. An unknown demo name fails visibly rather than silently running nothing.
 */
fun main() {
    MainScope().launch {
        when (val demo = queryParameter("demo")) {
            null -> runValidation()
            "particles" -> if (queryParameter("verify") == "1") {
                verifyParticles()
            } else {
                showParticlesPage(selectedLocale())
            }
            else -> showUnknownRoute(demo)
        }
    }
}

private suspend fun runValidation() {
    val report = try {
        runFoundations()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        BrowserReport(
            cases = foundationCases().map {
                CaseResult(it.id.id, "not-run", "Runner could not complete")
            },
            fatalError = failure.stackTraceToString(),
        )
    }
    publishReport(Json.encodeToString(report))
}

/**
 * Runs the two demo GPU checks on their own adapter and device and publishes `graphiksDemoReport`.
 *
 * An initialization failure is fatal and reported, never skipped. The whole run is bounded by a real
 * 60-second timeout; the adapter and device are always closed.
 */
private suspend fun verifyParticles() {
    val report = try {
        withTimeout(60.seconds) {
            val adapter = requestAdapter().getOrThrow()
            try {
                val device = adapter.requestDevice().getOrThrow()
                try {
                    DemoReport(cases = particleGpuResults(device))
                } finally {
                    device.close()
                }
            } finally {
                adapter.close()
            }
        }
    } catch (timeout: TimeoutCancellationException) {
        failedDemoReport("Timed out after 60 seconds")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        failedDemoReport(failure.stackTraceToString())
    }
    publishDemoReport(Json.encodeToString(report))
}

private fun failedDemoReport(diagnostic: String) = DemoReport(
    cases = listOf(
        CaseResult(ComputeRenderReadbackId, "failed", diagnostic),
        CaseResult(BoundsPauseResetId, "failed", diagnostic),
    ),
    fatalError = diagnostic,
)

private fun showUnknownRoute(demo: String) {
    elementById("validation")?.hidden = true
    elementById("demo-root")?.apply {
        hidden = false
        textContent = "Unknown demo '$demo'. Available demos: particles."
    }
}

private fun publishReport(value: String): Unit = js("""{
    globalThis.graphiksSuiteReport = value;
    document.getElementById('result').textContent = value;
}""")

private fun publishDemoReport(value: String): Unit = js("""{
    globalThis.graphiksDemoReport = value;
    var root = document.getElementById('demo-root');
    if (root) { root.hidden = false; root.textContent = value; }
    var validation = document.getElementById('validation');
    if (validation) { validation.hidden = true; }
}""")
