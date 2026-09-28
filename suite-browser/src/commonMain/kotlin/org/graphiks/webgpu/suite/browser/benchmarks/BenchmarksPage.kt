@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser.benchmarks

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.suite.benchmarks.BenchmarkProfile
import org.graphiks.webgpu.suite.browser.demos.AbortController
import org.graphiks.webgpu.suite.browser.demos.DomElement
import org.graphiks.webgpu.suite.browser.demos.createElement
import org.graphiks.webgpu.suite.browser.demos.documentRef
import org.graphiks.webgpu.suite.browser.demos.elementById
import org.graphiks.webgpu.suite.browser.demos.fetchText
import org.graphiks.webgpu.suite.browser.demos.listen
import org.graphiks.webgpu.suite.browser.demos.newAbortController
import org.graphiks.webgpu.suite.browser.demos.setClass
import org.graphiks.webgpu.suite.browser.demos.setText
import org.graphiks.webgpu.suite.browser.demos.windowRef

/** The localized texts of the benchmarks page, loaded from `benchmarks/texts.<locale>.json`. */
@Serializable
internal data class BenchmarkTexts(
    val title: String,
    val description: String,
    val protocol: String,
    val timing: String,
    val profile: String,
    val start: String,
    val cancel: String,
    val running: String,
    val statuses: Map<String, String>,
    val columnScenario: String,
    val columnStatus: String,
    val columnSize: String,
    val columnBatch: String,
    val columnWarmups: String,
    val columnSamples: String,
    val columnCpu: String,
    val columnCompletion: String,
    val columnVerified: String,
    val adapter: String,
    val fallback: String,
    val features: String,
    val limits: String,
    val lowResolution: String,
    val zeroSamples: String,
    val download: String,
    val unavailable: String,
    val cancelled: String,
    val notPublished: String,
    val yes: String,
    val no: String,
)

private const val LocalReportFile = "graphiks-benchmarks-local.json"

/** The report serializes its defaults too: the published JSON names its protocol and clock. */
private val reportJson = Json { encodeDefaults = true }

/**
 * Entry point of the `?benchmark=foundations` route.
 *
 * Loads the localized texts, then hands the page over to [BenchmarkPage]. The campaign only starts
 * on the button, unless [autorun] is set by the collector. A missing text resource or an
 * unavailable WebGPU environment produces a visible diagnostic and never a blank page or a
 * successful-looking empty report.
 */
internal suspend fun showBenchmarksPage(
    locale: String,
    profile: BenchmarkProfile = BenchmarkProfile.Standard,
    autorun: Boolean = false,
) {
    val texts = try {
        Json.decodeFromString<BenchmarkTexts>(fetchText("benchmarks/texts.$locale.json"))
    } catch (failure: Throwable) {
        showBenchmarkRouteError("Cannot load the benchmark texts for '$locale': ${failure.message}")
        return
    }
    BenchmarkPage(texts, profile, autorun).start()
}

/** Shows a plain diagnostic in the benchmark container, before or instead of the page. */
internal fun showBenchmarkRouteError(message: String) {
    elementById("validation")?.hidden = true
    elementById("demo-root")?.hidden = true
    val root = elementById("benchmark-root") ?: return
    root.hidden = false
    root.setText("")
    val paragraph = createElement("p")
    paragraph.setClass("benchmark-status")
    paragraph.setAttribute("data-error", "true")
    paragraph.setText(message)
    root.appendChild(paragraph)
}

/** Whether this browser exposes WebGPU at all; the campaign refuses to start without it. */
private fun webGpuAvailable(): Boolean = js("!!(globalThis.navigator && globalThis.navigator.gpu)")

/** Whether the page is currently hidden; a hidden campaign is invalidated. */
private fun documentHidden(): Boolean = js("globalThis.document.visibilityState !== 'visible'")

private fun createDownloadUrl(json: String): String =
    js("URL.createObjectURL(new Blob([json], { type: 'application/json' }))")

private fun revokeDownloadUrl(url: String) {
    js("URL.revokeObjectURL(url)")
}

private fun publishReport(value: String) {
    js("globalThis.graphiksBenchmarkReport = value")
}

/**
 * Owns the controls, the progress and the rendered results of the benchmarks page.
 *
 * A campaign always runs in the page's own scope. The button and the tab visibility are the only
 * ways it stops; there is no pause or resume. When a campaign is cancelled by the user or by a
 * hidden tab, the previously displayed result and its date stay in place: the incomplete report is
 * still published for tooling, but it is not presented as the new result.
 */
private class BenchmarkPage(
    private val texts: BenchmarkTexts,
    private val profile: BenchmarkProfile,
    private val autorun: Boolean,
) {

    private val scope: CoroutineScope = MainScope()
    private val controller: AbortController = newAbortController()

    private val startButton = createElement("button")
    private val cancelButton = createElement("button")
    private val progress = createElement("p")
    private val status = createElement("p")
    private val details = createElement("div")
    private val download = createElement("a")

    private var campaign: Job? = null
    private var cancelRequested = false
    private var downloadUrl: String? = null
    private var closed = false

    suspend fun start() {
        if (!buildDom()) {
            showBenchmarkRouteError("The benchmark container is missing from the page.")
            return
        }
        registerControls()
        updateButtons()
        if (!webGpuAvailable()) {
            status.setText(texts.unavailable)
            status.setAttribute("data-error", "true")
            startButton.disabled = true
            return
        }
        if (autorun) startCampaign()
    }

    private fun buildDom(): Boolean {
        val root = elementById("benchmark-root") ?: return false
        elementById("validation")?.hidden = true
        elementById("demo-root")?.hidden = true
        root.hidden = false
        root.setText("")

        val section = createElement("section")
        section.setClass("benchmark")

        val heading = createElement("h1")
        heading.setText(texts.title)
        section.appendChild(heading)

        section.appendChild(paragraph(texts.description, "benchmark-description"))
        section.appendChild(paragraph(texts.protocol, "benchmark-note"))
        section.appendChild(paragraph(texts.timing, "benchmark-note"))
        section.appendChild(paragraph("${texts.profile}: ${profile.id()}", "benchmark-profile"))

        val controls = createElement("div")
        controls.setClass("benchmark-controls")
        startButton.setAttribute("type", "button")
        startButton.setText(texts.start)
        controls.appendChild(startButton)
        cancelButton.setAttribute("type", "button")
        cancelButton.setText(texts.cancel)
        controls.appendChild(cancelButton)
        section.appendChild(controls)

        progress.setClass("benchmark-status")
        progress.setAttribute("id", "benchmark-progress")
        progress.setAttribute("role", "status")
        section.appendChild(progress)

        status.setClass("benchmark-status")
        status.setAttribute("id", "benchmark-status")
        status.setAttribute("role", "status")
        section.appendChild(status)

        download.setAttribute("download", LocalReportFile)
        download.setClass("benchmark-download")
        download.setText(texts.download)
        download.hidden = true
        section.appendChild(download)

        section.appendChild(paragraph(texts.notPublished, "benchmark-note"))
        section.appendChild(details)

        root.appendChild(section)
        return true
    }

    private fun registerControls() {
        startButton.listen("click", controller) { startCampaign() }
        cancelButton.listen("click", controller) { cancelCampaign() }
        documentRef().listen("visibilitychange", controller) { onVisibilityChange() }
        windowRef().listen("pagehide", controller) { close() }
    }

    private fun startCampaign() {
        if (closed || campaign != null) return
        if (documentHidden()) {
            status.setText(texts.cancelled)
            return
        }
        cancelRequested = false
        campaign = scope.launch { runCampaign() }
        updateButtons()
    }

    private suspend fun runCampaign() {
        var report: BenchmarkReport? = null
        var failure: Throwable? = null
        try {
            report = runBenchmarks(profile) { completed, total, id ->
                progress.setText("${texts.running} $completed/$total — $id")
            }
        } catch (cancelled: CancellationException) {
            cancelRequested = true
        } catch (thrown: Throwable) {
            failure = thrown
        } finally {
            campaign = null
            progress.setText("")
            updateButtons()
        }

        when {
            cancelRequested -> {
                // Tooling needs an incomplete report to fail fast; the displayed result and the
                // download link keep the previous campaign, so the interrupted one is not shown
                // as the new result.
                report?.let { publishReport(reportJson.encodeToString(it)) }
                status.setText(texts.cancelled)
            }
            failure != null -> {
                status.setAttribute("data-error", "true")
                status.setText(failure.stackTraceToString())
            }
            report != null -> {
                status.setText("")
                publishCampaign(report)
            }
        }
    }

    /** Publishes the local report, replaces the displayed result and refreshes the download link. */
    private fun publishCampaign(report: BenchmarkReport) {
        val json = reportJson.encodeToString(report)
        publishReport(json)
        downloadUrl?.let { revokeDownloadUrl(it) }
        val url = createDownloadUrl(json)
        downloadUrl = url
        download.setAttribute("href", url)
        download.hidden = false
        renderResults(report)
    }

    private fun cancelCampaign() {
        if (campaign == null) return
        cancelRequested = true
        campaign?.cancel()
    }

    private fun onVisibilityChange() {
        if (documentHidden()) cancelCampaign()
    }

    private fun updateButtons() {
        val running = campaign != null
        startButton.disabled = running || !webGpuAvailable()
        cancelButton.disabled = !running
    }

    private fun renderResults(report: BenchmarkReport) {
        details.setText("")

        val firstAdapter = report.scenarios.firstOrNull { it.status == "completed" }
        if (firstAdapter != null) {
            val summary = buildString {
                append("${texts.adapter}: ${firstAdapter.adapterDescription ?: "—"}")
                append(" · ${texts.fallback}: ${if (firstAdapter.isFallbackAdapter == true) texts.yes else texts.no}")
                if (firstAdapter.features.isNotEmpty()) {
                    append(" · ${texts.features}: ${firstAdapter.features.joinToString(", ")}")
                }
                if (firstAdapter.limitsUsed.isNotEmpty()) {
                    append(" · ${texts.limits}: " +
                        firstAdapter.limitsUsed.entries.joinToString(", ") { "${it.key}=${it.value}" })
                }
            }
            details.appendChild(paragraph(summary, "benchmark-note"))
        }

        val table = createElement("table")
        val head = createElement("thead")
        val headRow = createElement("tr")
        val headers = listOf(
            texts.columnScenario,
            texts.columnStatus,
            texts.columnSize,
            texts.columnBatch,
            texts.columnWarmups,
            texts.columnSamples,
            texts.columnCpu,
            texts.columnCompletion,
            texts.columnVerified,
        )
        for (header in headers) {
            val cell = createElement("th")
            cell.setText(header)
            headRow.appendChild(cell)
        }
        head.appendChild(headRow)
        table.appendChild(head)

        val body = createElement("tbody")
        for (scenario in report.scenarios) {
            body.appendChild(resultRow(scenario))
        }
        table.appendChild(body)
        details.appendChild(table)

        val lowResolution = report.scenarios.filter { it.zeroCpuSamples > 0 }
        if (lowResolution.isNotEmpty()) {
            details.appendChild(
                paragraph(
                    "${texts.lowResolution} (${texts.zeroSamples}: " +
                        lowResolution.joinToString(", ") { "${it.id}=${it.zeroCpuSamples}" } + ")",
                    "benchmark-note",
                ),
            )
        }
    }

    private fun resultRow(scenario: BenchmarkScenarioResult): DomElement {
        val row = createElement("tr")
        val cpuValues = scenario.samples.map { it.cpuIssueMs }
        val completionValues = scenario.samples.map { it.completionMs }
        val cells = listOf(
            scenario.id,
            texts.statuses[scenario.status] ?: scenario.status,
            "${scenario.size} ${scenario.sizeUnit}",
            scenario.operationsPerSample.toString(),
            scenario.warmups.toString(),
            "${scenario.samples.size}/${scenario.plannedSamples}",
            interval(cpuValues),
            interval(completionValues),
            if (scenario.outputVerified) texts.yes else texts.no,
        )
        for (value in cells) {
            val cell = createElement("td")
            cell.setText(value)
            row.appendChild(cell)
        }
        return row
    }

    /** `median [min..max] ms`, or an em dash for a scenario with no retained sample. */
    private fun interval(values: List<Double>): String {
        if (values.isEmpty()) return "—"
        val sorted = values.sorted()
        val middle = sorted.size / 2
        val median = if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
        return "${format(median)} [${format(sorted.first())}..${format(sorted.last())}]"
    }

    private fun format(value: Double): String = ((value * 1000.0).toInt() / 1000.0).toString()

    private fun paragraph(text: String, className: String): DomElement {
        val element = createElement("p")
        element.setClass(className)
        element.setText(text)
        return element
    }

    private fun close() {
        if (closed) return
        closed = true
        cancelRequested = true
        campaign?.cancel()
        scope.cancel()
        controller.abort()
    }
}
