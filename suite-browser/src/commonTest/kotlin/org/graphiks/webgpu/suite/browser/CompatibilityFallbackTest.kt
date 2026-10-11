package org.graphiks.webgpu.suite.browser

import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.CompatibilityModeUnavailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompatibilityFallbackTest {
    @Test
    fun onlyTheCompatibilityCaseCanReportACleanFallbackAsUnsupported() {
        assertEquals("unsupported", compatibilityFallbackResult(
            AcidCaseId.AdapterCompatibilityFeatureLevel, CompatibilityModeUnavailable(),
        ).status)
        assertEquals("failed", compatibilityFallbackResult(
            AcidCaseId.AdapterCoreFeatureLevel, CompatibilityModeUnavailable(),
        ).status)
    }

    @Test
    fun cleanupFailuresCannotBeHiddenByAnUnsupportedFallback() {
        val fallback = CompatibilityModeUnavailable().apply {
            addSuppressed(IllegalStateException("Device or adapter cleanup failed"))
        }
        val result = compatibilityFallbackResult(AcidCaseId.AdapterCompatibilityFeatureLevel, fallback)
        assertEquals("failed", result.status)
        assertNull(result.unsupportedReason)
    }
}
