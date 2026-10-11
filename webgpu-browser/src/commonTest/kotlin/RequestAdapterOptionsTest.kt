package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUFeatureLevel
import org.graphiks.webgpu.GPUPowerPreference
import org.graphiks.webgpu.descriptors.RequestAdapterOptions
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestAdapterOptionsTest {
    @Test
    fun defaultCoreAndCompatibilityReachTheBrowserAsTheirWebGPUStrings() {
        assertEquals("core", mapRequestAdapterOptions(RequestAdapterOptions()).featureLevel)
        assertEquals("core", mapRequestAdapterOptions(RequestAdapterOptions(featureLevel = GPUFeatureLevel.Core)).featureLevel)
        assertEquals("compatibility", mapRequestAdapterOptions(RequestAdapterOptions(featureLevel = GPUFeatureLevel.Compatibility)).featureLevel)
    }

    @Test
    fun featureLevelMappingKeepsTheOtherAdapterOptions() {
        val mapped = mapRequestAdapterOptions(RequestAdapterOptions(
            featureLevel = GPUFeatureLevel.Compatibility,
            powerPreference = GPUPowerPreference.LowPower,
            forceFallbackAdapter = true,
            xrCompatible = true,
        ))
        assertEquals("compatibility", mapped.featureLevel)
        assertEquals("low-power", mapped.powerPreference)
        assertEquals(true, mapped.forceFallbackAdapter)
        assertEquals(true, mapped.xrCompatible)
    }
}
