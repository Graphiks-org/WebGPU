@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.bindings

import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private fun readNumber(record: WebGpuRecord, key: String): Double = js("record[key]")
private fun ownCount(record: WebGpuRecord): Int = js("Object.keys(record).length")
private fun isMap(record: WebGpuRecord): Boolean = js("record instanceof Map")

class WebGpuRecordTest {

    @Test
    fun recordContainsEnumerableOwnProperties() {
        val record = createWebGpuRecord()
        setRecordValue(record, "scale", 7.0.asJsNumber())
        setRecordValue(record, "__proto__", 9.0.asJsNumber())
        assertEquals(7.0, readNumber(record, "scale"))
        assertEquals(9.0, readNumber(record, "__proto__"))
        assertEquals(2, ownCount(record))
        assertFalse(isMap(record))
    }
}
