@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUCanvasContext
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

/** The minimal HTML canvas element used to obtain a WebGPU canvas surface. */
external interface HTMLCanvasElement : JsAny {
    var width: Int
    var height: Int
    fun getContext(contextId: String): WGPUCanvasContext?
}

/** Returns the WebGPU canvas surface of this canvas, or fails when the browser has none. */
fun HTMLCanvasElement.getCanvasSurface(): CanvasSurface {
    val context = getContext("webgpu")
        ?: error("The canvas does not expose a WebGPU context.")
    return CanvasSurface(context)
}
