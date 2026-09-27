@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.bindings

actual fun<A: JsAny> JsArray<A>.bridge_get(index: Int): A = get(index)!!