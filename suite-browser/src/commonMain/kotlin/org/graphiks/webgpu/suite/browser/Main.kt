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
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.suite.acid.SuiteBuildIdentity
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
import org.graphiks.webgpu.suite.browser.demos.ReactionCheckIds
import org.graphiks.webgpu.suite.browser.demos.reactionDiffusionGpuResults
import org.graphiks.webgpu.suite.browser.demos.showDemoFailure

/**
 * The page has explicit routes. Without parameters it runs the foundation validation suite and
 * publishes `graphiksSuiteReport`; `?cases=id1,id2` runs only the named cases. `?demo=particles`
 * runs the interactive particle demo, and `?demo=particles&verify=1` runs its GPU checks and
 * publishes `graphiksDemoReport`. `?benchmark=foundations&profile=standard|ci` runs the benchmark
 * campaign and publishes `graphiksBenchmarkReport`; `autorun=1` starts it without the user, for the
 * collector. Asking for a demo and a benchmark at once, or naming an unknown demo, benchmark or
 * profile, fails visibly rather than silently running nothing.
 *
 * The validation and demo-verification routes render the report for a person — summary, table and
 * the raw JSON folded below — and publish the compact JSON for tooling. When the localized texts
 * cannot be loaded they show the raw JSON instead, never a blank or silent page.
 */

/** The JSON shown to a person is pretty-printed; the JSON published for tooling is compact. */
private val prettyJson = Json { prettyPrint = true }

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
                verifyDemo(listOf(ComputeRenderReadbackId, BoundsPauseResetId), ::particleGpuResults)
            } else {
                showParticlesPage(selectedLocale())
            }
            demo == "reaction-diffusion" -> if (queryParameter("verify") == "1") {
                verifyDemo(ReactionCheckIds, ::reactionDiffusionGpuResults)
            } else {
                showDemoFailure("The interactive reaction-diffusion page is not available yet.")
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
    val locale = selectedLocale()
    val texts = loadValidationTexts(locale)
    if (texts != null) applyValidationTexts(locale, texts)

    val selection = parseCaseSelection(queryParameter("cases"))
    val report = try {
        runFoundations(selection) { current, total, id ->
            if (texts != null) setValidationStatus(progressLine(texts.running, current, total, id))
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        BrowserReport(
            buildCommit = SuiteBuildIdentity.COMMIT,
            buildVersion = SuiteBuildIdentity.VERSION,
            cases = foundationCases().map {
                CaseResult(it.id.id, "not-run", "Runner could not complete")
            },
            fatalError = failure.stackTraceToString(),
        )
    }

    val json = Json.encodeToString(report)
    publishSuiteReport(json)
    if (texts == null) {
        showRawReportFallback(json)
    } else {
        renderCaseReport(
            rootId = "validation",
            title = texts.title,
            description = texts.description,
            cases = report.cases,
            fatalError = report.fatalError,
            texts = texts,
            rawJson = prettyJson.encodeToString(report),
        )
    }
}

/** Reads `?cases=id1,id2`: `null` without the parameter, the trimmed non-empty ids otherwise. */
private fun parseCaseSelection(value: String?): Set<String>? =
    value?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()

/**
 * Runs the selected demo GPU checks on their own adapter and device and publishes `graphiksDemoReport`.
 *
 * An initialization failure is fatal and reported, never skipped. The whole run is bounded by a real
 * 60-second timeout; the adapter and device are always closed.
 */
private suspend fun verifyDemo(ids: List<String>, results: suspend (GPUDevice) -> List<CaseResult>) {
    val locale = selectedLocale()
    val texts = loadValidationTexts(locale)
    if (texts != null) setDocumentLang(locale)
    val report = try {
        withTimeout(60.seconds) {
            val adapter = requestAdapter().getOrThrow()
            try {
                val device = adapter.requestDevice().getOrThrow()
                try {
                    DemoReport(
                        buildCommit = SuiteBuildIdentity.COMMIT,
                        buildVersion = SuiteBuildIdentity.VERSION,
                        cases = results(device),
                    )
                } finally {
                    device.close()
                }
            } finally {
                adapter.close()
            }
        }
    } catch (timeout: TimeoutCancellationException) {
        failedDemoReport(ids, "Timed out after 60 seconds")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        failedDemoReport(ids, failure.stackTraceToString())
    }

    val json = Json.encodeToString(report)
    publishDemoReport(json)
    if (texts == null) {
        showDemoRawReportFallback(json)
    } else {
        elementById("validation")?.hidden = true
        renderCaseReport(
            rootId = "demo-root",
            title = texts.demoTitle,
            description = texts.demoDescription,
            cases = report.cases,
            fatalError = report.fatalError,
            texts = texts,
            rawJson = prettyJson.encodeToString(report),
        )
    }
}

private fun failedDemoReport(ids: List<String>, diagnostic: String) = DemoReport(
    buildCommit = SuiteBuildIdentity.COMMIT,
    buildVersion = SuiteBuildIdentity.VERSION,
    cases = ids.map { CaseResult(it, "failed", diagnostic) },
    fatalError = diagnostic,
)

private fun showUnknownRoute(demo: String) {
    elementById("validation")?.hidden = true
    elementById("demo-root")?.apply {
        hidden = false
        textContent = "Unknown demo '$demo'. Available demos: particles, reaction-diffusion."
    }
}

private fun publishSuiteReport(value: String): Unit = js("""{
    globalThis.graphiksSuiteReport = value;
}""")

/** The display used when the localized texts cannot load: the raw JSON plus an explicit diagnostic. */
private fun showRawReportFallback(value: String): Unit = js("""{
    var status = document.getElementById('validation-status');
    if (status) { status.textContent = 'Cannot load the page texts; the raw report is shown instead.'; }
    var result = document.getElementById('result');
    if (result) { result.textContent = value; }
}""")

private fun publishDemoReport(value: String): Unit = js("""{
    globalThis.graphiksDemoReport = value;
}""")

/** The demo fallback: the raw JSON prefixed with the diagnostic. */
private fun showDemoRawReportFallback(value: String): Unit = js("""{
    var validation = document.getElementById('validation');
    if (validation) { validation.hidden = true; }
    var root = document.getElementById('demo-root');
    if (root) {
        root.hidden = false;
        root.textContent = 'Cannot load the page texts; the raw report is shown instead.\n' + value;
    }
}""")
