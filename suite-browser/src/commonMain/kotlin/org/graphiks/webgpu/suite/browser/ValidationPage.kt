@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.suite.browser.demos.DomElement
import org.graphiks.webgpu.suite.browser.demos.createElement
import org.graphiks.webgpu.suite.browser.demos.elementById
import org.graphiks.webgpu.suite.browser.demos.fetchText
import org.graphiks.webgpu.suite.browser.demos.setClass
import org.graphiks.webgpu.suite.browser.demos.setText

/**
 * The human-readable rendering of a case report, shared by the validation route and the demo
 * verification route.
 *
 * The runner already publishes the raw JSON in `globalThis.graphiksSuiteReport` and
 * `globalThis.graphiksDemoReport` for tooling; this page turns the same report into a summary, a
 * table and a folded raw block for a person. Everything dynamic is inserted with `textContent`,
 * never as HTML, and the localized texts come from `validation/texts.<locale>.json`.
 */
@Serializable
internal data class ValidationTexts(
    val title: String,
    val description: String,
    val running: String,
    val summaryPassed: String,
    val summaryFailed: String,
    val summaryUnsupported: String,
    val summaryNotRun: String,
    val summaryUnknown: String,
    val summaryTotal: String,
    val fatal: String,
    val columnCase: String,
    val columnStatus: String,
    val columnDiagnostic: String,
    val columnAdapter: String,
    val rawSummary: String,
    val demoTitle: String,
    val demoDescription: String,
    val statuses: Map<String, String>,
) {
    /** The localized words of the summary line, in the shape [summaryLine] expects. */
    fun summaryLabels() = SummaryLabels(
        passed = summaryPassed,
        failed = summaryFailed,
        unsupported = summaryUnsupported,
        notRun = summaryNotRun,
        unknown = summaryUnknown,
        total = summaryTotal,
    )
}

/**
 * Loads the localized texts of the validation pages; `null` when the resource cannot be read, so
 * the caller can fall back to the raw report display instead of failing blank.
 */
internal suspend fun loadValidationTexts(locale: String): ValidationTexts? = try {
    Json.decodeFromString<ValidationTexts>(fetchText("validation/texts.$locale.json"))
} catch (failure: Throwable) {
    null
}

/** Localizes the static headings of the validation section before the run starts. */
internal fun applyValidationTexts(locale: String, texts: ValidationTexts) {
    setDocumentLang(locale)
    elementById("validation-title")?.setText(texts.title)
    elementById("validation-description")?.setText(texts.description)
}

/** Marks the document with the locale the page renders in. */
internal fun setDocumentLang(locale: String): Unit = js("globalThis.document.documentElement.lang = locale")

/** Shows [text] in the validation status line while the catalogue runs. */
internal fun setValidationStatus(text: String) {
    elementById("validation-status")?.setText(text)
}

/**
 * Replaces [rootId]'s content with the readable rendering of one case report: heading, description,
 * a fatal error when there is one, the summary line, the case table and the raw JSON folded in a
 * `details` block. The root is given the `validation` class, so the same styles apply whether it
 * is the validation section or the demo root. Returns `false` when the root is missing from the page.
 */
internal fun renderCaseReport(
    rootId: String,
    title: String,
    description: String,
    cases: List<CaseResult>,
    fatalError: String?,
    texts: ValidationTexts,
    rawJson: String,
): Boolean {
    val root = elementById(rootId) ?: return false
    root.hidden = false
    root.setText("")
    root.setClass("validation")

    val heading = createElement("h1")
    heading.setText(title)
    root.appendChild(heading)

    root.appendChild(paragraph(description, "validation-description"))

    if (fatalError != null) {
        root.appendChild(paragraph("${texts.fatal}: $fatalError", "validation-fatal"))
    }

    val counts = validationCounts(cases)
    root.appendChild(paragraph(summaryLine(counts, texts.summaryLabels()), "validation-summary"))

    root.appendChild(buildCaseTable(cases, texts))
    root.appendChild(buildRawDetails(texts.rawSummary, rawJson))
    return true
}

private fun paragraph(text: String, className: String): DomElement {
    val node = createElement("p")
    node.setClass(className)
    node.setText(text)
    return node
}

private fun cell(tag: String, text: String, className: String?): DomElement {
    val node = createElement(tag)
    if (className != null) node.setClass(className)
    node.setText(text)
    return node
}

private fun buildCaseTable(cases: List<CaseResult>, texts: ValidationTexts): DomElement {
    val table = createElement("table")
    val head = createElement("thead")
    val headRow = createElement("tr")
    for (label in listOf(texts.columnCase, texts.columnStatus, texts.columnDiagnostic, texts.columnAdapter)) {
        headRow.appendChild(cell("th", label, null))
    }
    head.appendChild(headRow)
    table.appendChild(head)

    val body = createElement("tbody")
    for (case in cases) {
        val status = normalizedStatus(case.status)
        val row = createElement("tr")
        row.appendChild(cell("td", case.id, "mono"))
        row.appendChild(cell("td", texts.statuses[status] ?: case.status, "status status-$status"))
        row.appendChild(cell("td", case.diagnostic ?: "", "diagnostic"))
        row.appendChild(cell("td", case.adapterDescription ?: "", null))
        body.appendChild(row)
    }
    table.appendChild(body)
    return table
}

private fun buildRawDetails(label: String, rawJson: String): DomElement {
    val details = createElement("details")
    details.setClass("validation-raw")
    val summary = createElement("summary")
    summary.setText(label)
    details.appendChild(summary)
    val pre = createElement("pre")
    pre.setAttribute("id", "result")
    pre.setText(rawJson)
    details.appendChild(pre)
    return details
}
