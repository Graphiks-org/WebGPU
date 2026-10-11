package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.suite.CompatibilityModeUnavailable
import kotlin.test.assertTrue

internal fun requireCompatibilityFeatures(features: Set<GPUFeatureName>) {
    assertTrue((features - GPUFeatureName.CoreFeaturesAndLimits).isEmpty(),
        "Optional features must not be enabled without an explicit request: $features")
    if (GPUFeatureName.CoreFeaturesAndLimits in features) {
        throw CompatibilityModeUnavailable()
    }
    assertTrue(features.isEmpty(), "No feature should be enabled without an explicit request")
}
