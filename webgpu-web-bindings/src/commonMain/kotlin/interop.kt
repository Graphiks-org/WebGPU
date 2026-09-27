@file:Suppress("unused")
@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.bindings

import js.promise.Promise
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.js

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun JsNumber.toLong(): Long = toLong(this)
fun JsNumber.toULong(): ULong = toULong(this)

private fun toLong(ref: JsNumber): Long = js("BigInt(ref)")
private fun toULong(ref: JsNumber): ULong = js("BigInt(ref)")

external interface EventTarget: JsAny
external interface DOMException: JsAny
external interface Event: JsAny
external interface EventInit: JsAny

external object navigator {
    val gpu: GPU?
}

external object window {
    var devicePixelRatio: JsNumber
}

external interface GPU: JsAny {
    fun getPreferredCanvasFormat(): String
    fun requestAdapter(): Promise<JsAny>
    fun requestAdapter(descriptor: WGPURequestAdapterOptions): Promise<JsAny>
    var wgslLanguageFeatures: JsAny /* WGSLLanguageFeatures */
}

