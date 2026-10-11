package org.graphiks.webgpu.suite.browser

import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.CompatibilityModeUnavailable

internal fun compatibilityFallbackResult(id: AcidCaseId, fallback: CompatibilityModeUnavailable): CaseResult =
    if (id == AcidCaseId.AdapterCompatibilityFeatureLevel && fallback.suppressedExceptions.isEmpty()) {
        CaseResult(id.id, "unsupported", fallback.message,
            unsupportedReason = "compatibility-mode-core-fallback")
    } else {
        CaseResult(id.id, "failed", fallback.stackTraceToString())
    }
