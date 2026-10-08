@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.bindings

expect fun <A: JsAny, B> JsArray<A>.map(converter: (A) -> B): List<B>

fun <A: JsAny> jsArray(vararg values: A): JsArray<A> = js("Array.from(values)")

fun <A, B : JsAny?> Collection<A>.mapJsArray(converter: (A) -> B): JsArray<B> {
    val output = JsArray<B>()
    forEachIndexed { index, value ->
        output[index] = converter(value)
    }
    return output
}

fun <T : JsAny> createJsObject(): T = js("({ })")

