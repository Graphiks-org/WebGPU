@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.bindings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.JsArray
import kotlin.js.get
import kotlin.js.length
import kotlin.js.js
import kotlin.js.toInt
import kotlin.js.toJsBigInt
import kotlin.js.toJsNumber
import kotlin.js.toJsReference

class JsArrayInteropTest {

    @Test
    fun jsArray_vararg_creation_and_indexing() {
        val arr: JsArray<JsNumber> = jsArray(1.asJsNumber(), 2.asJsNumber(), 3.asJsNumber())
        assertEquals(3, arr.length, "JsArray length should match number of elements")
        assertEquals(1L, arr.bridge_get(0).toLong())
        assertEquals(2L, arr.bridge_get(1).toLong())
        assertEquals(3L, arr.bridge_get(2).toLong())
    }

    @Test
    fun jsArray_extension_map_to_kotlin_list() {
        val arr = jsArray(10.asJsNumber(), 20.asJsNumber(), 30.asJsNumber())
        val mapped: List<Long> = arr.map { it.toLong() }
        assertEquals(listOf(10L, 20L, 30L), mapped)
    }

    @Test
    fun collection_mapJsArray_to_jsarray() {
        val src = listOf(7, 8, 9, Int.MAX_VALUE, Int.MIN_VALUE)
        val jsArr = src.mapJsArray { it.asJsNumber() }
        assertEquals(src.size, jsArr.length)
        assertEquals(7L, jsArr.bridge_get(0).toLong())
        assertEquals(7, jsArr.bridge_get(0).toInt())
        assertEquals(8L, jsArr.bridge_get(1).toLong())
        assertEquals(8, jsArr.bridge_get(1).toInt())
        assertEquals(9L, jsArr.bridge_get(2).toLong())
        assertEquals(9, jsArr.bridge_get(2).toInt())
        assertEquals(Int.MAX_VALUE.toLong(), jsArr.bridge_get(3).toLong())
        assertEquals(Int.MAX_VALUE, jsArr.bridge_get(3).toInt())
        assertEquals(Int.MIN_VALUE.toLong(), jsArr.bridge_get(4).toLong())
        assertEquals(Int.MIN_VALUE, jsArr.bridge_get(4).toInt())
    }

    @Test
    fun createJsObject_is_mutable_and_holds_properties() {
        val o = createJsObject<TestJs>()
        assertNotNull(o)
        // assign a property via JS and read it back
        o.x = 42.toJsNumber()
        assertEquals(42L, o.x.toLong())
    }

}

// js and wasmjs footprint mismatch
expect fun<A: JsAny> JsArray<A>.bridge_get(index: Int): A

external interface TestJs : JsAny {
    var x: JsNumber
}