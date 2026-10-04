package org.graphiks.webgpu.suite.browser

/**
 * The pure summary logic behind the human-readable validation page.
 *
 * A status outside the four documented ones is never silently swallowed: it is counted and
 * displayed separately, so a runner change can never hide behind a familiar sum.
 */

/** The counted statuses of a report; [unknown] holds every status outside the four known ones. */
internal data class ValidationCounts(
    val passed: Int,
    val failed: Int,
    val unsupported: Int,
    val notRun: Int,
    val unknown: Int,
    val total: Int,
)

/** The localized words of the summary line; [total] is a template with one `{total}` placeholder. */
internal data class SummaryLabels(
    val passed: String,
    val failed: String,
    val unsupported: String,
    val notRun: String,
    val unknown: String,
    val total: String,
)

/** Normalizes a raw status for counting, styling and labelling; anything else becomes `unknown`. */
internal fun normalizedStatus(status: String): String = when (status) {
    "passed", "failed", "unsupported", "not-run" -> status
    else -> "unknown"
}

/** Counts the statuses of a report; [total] is the number of cases seen, whatever their status. */
internal fun validationCounts(cases: List<CaseResult>): ValidationCounts {
    var passed = 0
    var failed = 0
    var unsupported = 0
    var notRun = 0
    var unknown = 0
    for (case in cases) {
        when (normalizedStatus(case.status)) {
            "passed" -> passed++
            "failed" -> failed++
            "unsupported" -> unsupported++
            "not-run" -> notRun++
            else -> unknown++
        }
    }
    return ValidationCounts(passed, failed, unsupported, notRun, unknown, cases.size)
}

/**
 * Builds the human summary line: the three headline counts are always shown — the zeros are part of
 * the answer — while `not run` and `unknown` appear only when they actually happened.
 */
internal fun summaryLine(counts: ValidationCounts, labels: SummaryLabels): String {
    val segments = mutableListOf(
        "${counts.passed} ${labels.passed}",
        "${counts.failed} ${labels.failed}",
        "${counts.unsupported} ${labels.unsupported}",
    )
    if (counts.notRun > 0) segments += "${counts.notRun} ${labels.notRun}"
    if (counts.unknown > 0) segments += "${counts.unknown} ${labels.unknown}"
    val total = labels.total.replace("{total}", counts.total.toString())
    return segments.joinToString(" · ") + " — " + total
}

/**
 * Builds the progress line shown while the catalogue runs, e.g. `Running case 12/132 — the-id`.
 * [current] is the 1-based number of the case now running out of [total].
 */
internal fun progressLine(label: String, current: Int, total: Int, id: String): String =
    "$label $current/$total — $id"
