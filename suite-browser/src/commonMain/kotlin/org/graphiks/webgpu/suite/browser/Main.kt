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
import org.graphiks.webgpu.suite.benchmarks.BenchmarkProfile
import org.graphiks.webgpu.suite.browser.benchmarks.showBenchmarkRouteError
import org.graphiks.webgpu.suite.browser.benchmarks.showBenchmarksPage
import org.graphiks.webgpu.suite.browser.demos.BoundsPauseResetId
import org.graphiks.webgpu.suite.browser.demos.ComputeRenderReadbackId
import org.graphiks.webgpu.suite.browser.demos.DemoReport
import org.graphiks.webgpu.suite.browser.demos.elementById
import org.graphiks.webgpu.suite.browser.demos.particleGpuResults
import org.graphiks.webgpu.suite.browser.demos.queryParameter
import org.graphiks.webgpu.suite.browser.demos.selectedLocale
import org.graphiks.webgpu.suite.browser.demos.showParticlesPage

/**
 * The page has explicit routes. Without parameters it runs the foundation validation suite and
 * publishes `graphiksSuiteReport`. `?demo=particles` runs the interactive particle demo, and
 * `?demo=particles&verify=1` runs its GPU checks and publishes `graphiksDemoReport`.
 * `?benchmark=foundations&profile=standard|ci` runs the benchmark campaign and publishes
 * `graphiksBenchmarkReport`; `autorun=1` starts it without the user, for the collector. Asking for
 * a demo and a benchmark at once, or naming an unknown demo, benchmark or profile, fails visibly
 * rather than silently running nothing.
 */
fun main() {
    MainScope().launch {
        val demo = queryParameter("demo")
        val benchmark = queryParameter("benchmark")
        when {
            demo != null && benchmark != null ->
                showBenchmarkRouteError("Use either 'demo' or 'benchmark', not both.")
            benchmark != null -> routeBenchmark(benchmark)
            demo == null -> runValidation()
            demo == "particles" -> if (queryParameter("verify") == "1") {
                verifyParticles()
            } else {
                showParticlesPage(selectedLocale())
            }
            else -> showUnknownRoute(demo)
        }
    }
}

/** Resolves the benchmark route, refusing an unknown benchmark name or profile visibly. */
private suspend fun routeBenchmark(name: String) {
    if (name != "foundations") {
        showBenchmarkRouteError("Unknown benchmark '$name'. Available benchmarks: foundations.")
        return
    }
    val profileName = queryParameter("profile") ?: "standard"
    val profile = when (profileName) {
        "standard" -> BenchmarkProfile.Standard
        "ci" -> BenchmarkProfile.Ci
        else -> {
            showBenchmarkRouteError("Unknown profile '$profileName'. Use 'standard' or 'ci'.")
            return
        }
    }
    showBenchmarksPage(selectedLocale(), profile, autorun = queryParameter("autorun") == "1")
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
