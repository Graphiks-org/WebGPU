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
internal external interface CityLayoutWatch : JsAny { fun disconnect() }

/** Wake a paused renderer for style/ancestor visibility changes and canvas reconnection. */
internal fun watchCityLayout(canvas: DomElement, wake: () -> Unit): CityLayoutWatch = js("""{
    const signature = () => {
        const rect = canvas.getBoundingClientRect();
        return [canvas.isConnected, canvas.checkVisibility({visibilityProperty:true}),
            canvas.clientWidth, canvas.clientHeight, rect.width, rect.height].join(':');
    };
    let previous = signature();
    const changed = () => {
        const current = signature();
        if (current !== previous) { previous = current; wake(); }
    };
    const resize = new ResizeObserver(changed);
    resize.observe(canvas);
    const mutation = new MutationObserver(changed);
    mutation.observe(document.documentElement, {subtree:true, childList:true,
        attributes:true, attributeFilter:['style','class','hidden']});
    return {disconnect() { resize.disconnect(); mutation.disconnect(); }};
}""")

internal fun cityWheelOptions(controller: AbortController): JsAny = js("({signal: controller.signal, passive: false})")
internal fun cityReducedMotion(): Boolean = js("globalThis.matchMedia('(prefers-reduced-motion: reduce)').matches")
internal fun cityCanvasVisible(canvas: DomElement): Boolean = js(
    "canvas.isConnected && document.visibilityState !== 'hidden' && " +
        "canvas.checkVisibility({visibilityProperty:true}) && " +
        "canvas.getBoundingClientRect().width > 0 && canvas.getBoundingClientRect().height > 0",
)
internal fun reloadCityPage(): Unit = js("globalThis.location.reload()")
