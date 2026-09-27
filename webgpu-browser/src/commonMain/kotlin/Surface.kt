@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUCanvasContext
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.unsafeCast
import web.html.HTMLCanvasElement

private external interface WebGpuCanvasContextProvider : JsAny {
    fun getContext(contextId: String): WGPUCanvasContext?
}

/** Returns the WebGPU canvas surface of this canvas, or fails when the browser has none. */
fun HTMLCanvasElement.getCanvasSurface(): CanvasSurface {
    val context = unsafeCast<WebGpuCanvasContextProvider>().getContext("webgpu")
        ?: error("The canvas does not expose a WebGPU context.")
    return CanvasSurface(context)
}
