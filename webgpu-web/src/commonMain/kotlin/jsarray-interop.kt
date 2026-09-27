@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.get
import kotlin.js.js
import kotlin.js.length
import kotlin.js.set
import kotlin.js.unsafeCast

expect fun <A: JsAny, B> JsArray<A>.map(converter: (A) -> B): List<B>

fun <A: JsAny> jsArray(vararg values: A): JsArray<A> = js("Array.from(values)")

fun <A, B : JsAny> Collection<A>.mapJsArray(converter: (A) -> B): JsArray<B> {
    val output = JsArray<B>()
    forEachIndexed { index, value ->
        output[index] = converter(value)
    }
    return output.unsafeCast<JsArray<B>>()
}

fun <T : JsAny> createJsObject(): T = js("({ })")

