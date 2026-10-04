package org.graphiks.webgpu.suite.browser

import kotlin.test.Test
import kotlin.test.assertEquals

/** The pure summary logic behind the human-readable validation page, shared by JS and Wasm. */
class ValidationSummaryTest {
    private fun case(status: String) = CaseResult("case.$status", status)

    @Test
    fun validation_counts_counts_each_known_status() {
        val counts = validationCounts(
            listOf(case("passed"), case("passed"), case("failed"), case("unsupported"), case("not-run")),
        )

        assertEquals(2, counts.passed)
        assertEquals(1, counts.failed)
        assertEquals(1, counts.unsupported)
        assertEquals(1, counts.notRun)
        assertEquals(0, counts.unknown)
        assertEquals(5, counts.total)
    }

    @Test
    fun validation_counts_reports_unknown_statuses_separately() {
        val counts = validationCounts(listOf(case("passed"), case("borked")))

        assertEquals(1, counts.unknown)
        assertEquals(1, counts.passed)
        assertEquals(2, counts.total)
    }

    @Test
    fun validation_counts_empty_report_has_zero_counts() {
        assertEquals(ValidationCounts(0, 0, 0, 0, 0, 0), validationCounts(emptyList()))
    }

    @Test
    fun summary_line_always_lists_the_headline_counts() {
        val counts = validationCounts(List(132) { case("passed") })

        assertEquals(
            "132 passed · 0 failed · 0 unsupported — 132 cases",
            summaryLine(counts, englishLabels()),
        )
    }

    @Test
    fun summary_line_lists_not_run_only_when_present() {
        val counts = validationCounts(listOf(case("passed"), case("not-run")))

        assertEquals(
            "1 passed · 0 failed · 0 unsupported · 1 not run — 2 cases",
            summaryLine(counts, englishLabels()),
        )
    }

    @Test
    fun summary_line_lists_unknown_only_when_present() {
        val counts = validationCounts(listOf(case("passed"), case("borked")))

        assertEquals(
            "1 passed · 0 failed · 0 unsupported · 1 unknown — 2 cases",
            summaryLine(counts, englishLabels()),
        )
    }

    @Test
    fun progress_line_formats_the_running_case() {
        assertEquals(
            "Running case 12/132 — buffers.map-destroy",
            progressLine("Running case", 12, 132, "buffers.map-destroy"),
        )
    }

    @Test
    fun normalized_status_keeps_known_statuses_and_marks_the_rest_unknown() {
        assertEquals("passed", normalizedStatus("passed"))
        assertEquals("failed", normalizedStatus("failed"))
        assertEquals("unsupported", normalizedStatus("unsupported"))
        assertEquals("not-run", normalizedStatus("not-run"))
        assertEquals("unknown", normalizedStatus("borked"))
        assertEquals("unknown", normalizedStatus(""))
    }

    private fun englishLabels() = SummaryLabels(
        passed = "passed",
        failed = "failed",
        unsupported = "unsupported",
        notRun = "not run",
        unknown = "unknown",
        total = "{total} cases",
    )
}
