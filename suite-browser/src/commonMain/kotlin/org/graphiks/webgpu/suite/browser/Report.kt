package org.graphiks.webgpu.suite.browser

import kotlinx.serialization.Serializable

/**
 * The observed outcome of one case.
 *
 * `status` is one of `passed`, `failed`, `unsupported` or `not-run`. A failure is never
 * reported as `unsupported`: optional capabilities are named explicitly and everything else
 * stays a real failure.
 */
@Serializable
data class CaseResult(
    val id: String,
    val status: String,
    val diagnostic: String? = null,
    val adapterDescription: String? = null,
    val missingFeatures: List<String> = emptyList(),
)

/**
 * The report the browser runner publishes in `globalThis.graphiksSuiteReport`.
 */
@Serializable
data class BrowserReport(
    val schemaVersion: Int = 1,
    val buildCommit: String,
    val buildVersion: String,
    val cases: List<CaseResult>,
    val fatalError: String? = null,
)
