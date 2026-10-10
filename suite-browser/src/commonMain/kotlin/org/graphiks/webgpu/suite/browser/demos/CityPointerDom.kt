@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.JsAny
import kotlin.js.js

internal external interface CityWheelEvent : JsAny {
    val deltaY: Double
    val deltaMode: Int
    fun preventDefault()
}
internal external interface CityCheckbox : JsAny { var checked: Boolean }

internal fun cityWheelOptions(controller: AbortController): JsAny = js("({signal: controller.signal, passive: false})")
internal fun cityReducedMotion(): Boolean = js("globalThis.matchMedia('(prefers-reduced-motion: reduce)').matches")
internal fun cityCanvasVisible(canvas: DomElement): Boolean = js(
    "canvas.isConnected && document.visibilityState !== 'hidden' && " +
        "canvas.checkVisibility({visibilityProperty:true}) && " +
        "canvas.getBoundingClientRect().width > 0 && canvas.getBoundingClientRect().height > 0",
)
internal fun reloadCityPage(): Unit = js("globalThis.location.reload()")
