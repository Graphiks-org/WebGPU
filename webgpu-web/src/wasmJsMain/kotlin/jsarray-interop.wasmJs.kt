@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu

actual fun <A: JsAny, B> JsArray<A>.map(converter: (A) -> B): List<B> = sequence {
    (0 until length).forEach { index ->
        yield(converter(this@map.get(index)!!))
    }
}.toList()