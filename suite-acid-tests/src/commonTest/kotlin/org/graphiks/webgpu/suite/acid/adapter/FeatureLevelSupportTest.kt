package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.suite.CompatibilityModeUnavailable
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FeatureLevelSupportTest {
    @Test
    fun compatibilityEnablesNoFeaturesImplicitly() {
        requireCompatibilityFeatures(emptySet())
    }

    @Test
    fun onlyImplicitCoreIsAnUnsupportedFallback() {
        assertFailsWith<CompatibilityModeUnavailable> {
            requireCompatibilityFeatures(setOf(GPUFeatureName.CoreFeaturesAndLimits))
        }
    }

    @Test
    fun unrequestedOptionalFeaturesFailEvenWhenCoreFallbackIsPresent() {
        for (features in listOf(
            setOf(GPUFeatureName.ShaderF16),
            setOf(GPUFeatureName.CoreFeaturesAndLimits, GPUFeatureName.ShaderF16),
        )) {
            assertFailsWith<AssertionError> { requireCompatibilityFeatures(features) }
        }
    }
}
