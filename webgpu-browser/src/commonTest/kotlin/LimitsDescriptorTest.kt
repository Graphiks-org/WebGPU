@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.bindings.WGPUSupportedLimits
import org.graphiks.webgpu.browser.mapper.map
import kotlin.test.Test
import kotlin.test.assertEquals

private fun proxyLimits(): WGPUSupportedLimits = js(
    "new Proxy({}, { get: (target, key) => key === 'maxBufferSize' ? 4294967296 : 64 })",
)

class LimitsDescriptorTest {

    @Test
    fun limitsRoundTripPreservesNewFieldsAnd64BitValues() {
        val contract = map(proxyLimits())
        val record: GPUSupportedLimits = contract
        val out = map(record)
        assertEquals(64.0, propertyNumber(out, "maxImmediateSize"))
        assertEquals(64.0, propertyNumber(out, "maxStorageBuffersInVertexStage"))
        assertEquals(64.0, propertyNumber(out, "maxStorageBuffersInFragmentStage"))
        assertEquals(64.0, propertyNumber(out, "maxStorageTexturesInVertexStage"))
        assertEquals(64.0, propertyNumber(out, "maxStorageTexturesInFragmentStage"))
        assertEquals(4294967296.0, propertyNumber(out, "maxBufferSize"))
    }
}
