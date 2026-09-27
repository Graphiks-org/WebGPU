@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUBuffer
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals

private fun fakeBuffer(): WGPUBuffer = js("({ label: 'before', size: 16, usage: 8, mapState: 'unmapped' })")

class WrapperSmokeTest {
    @Test fun bufferDelegatesLabelAndSize() {
        val raw = fakeBuffer()
        val wrapped = Buffer(raw)
        assertEquals(16uL, wrapped.size)
        wrapped.label = "after"
        assertEquals("after", raw.label)
    }
}
