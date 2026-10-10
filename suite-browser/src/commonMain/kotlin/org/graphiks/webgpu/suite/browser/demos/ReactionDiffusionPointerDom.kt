@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.JsAny
import kotlin.js.js

internal external interface ReactionPointerEvent : JsAny {
    val pointerId: Int
    val clientX: Double
    val clientY: Double
    val button: Int
}

internal external interface ReactionRect : JsAny {
    val left: Double
    val top: Double
    val width: Double
    val height: Double
}

internal external interface ReactionPointerCanvas : JsAny {
    fun getBoundingClientRect(): ReactionRect
    fun setPointerCapture(pointerId: Int)
    fun hasPointerCapture(pointerId: Int): Boolean
    fun releasePointerCapture(pointerId: Int)
}

internal fun reloadReactionPage(): Unit = js("globalThis.location.reload()")
