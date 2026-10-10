@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.js

/** Page-level navigation stays usable even when the demo cannot acquire a GPU device. */
internal fun showDemoNavigation(active: String, locale: String) {
    val navigation = elementById("demo-navigation") ?: return
    val controller = newAbortController()
    val label = createElement("label")
    label.setAttribute("for", "demo-selector")
    label.setText(if (locale == "fr") "Démo" else "Demo")
    val selector = createElement("select")
    selector.setAttribute("id", "demo-selector")
    for ((name, title) in listOf(
        "particles" to if (locale == "fr") "Particules" else "Particles",
        "reaction-diffusion" to if (locale == "fr") "Réaction-diffusion" else "Reaction-diffusion",
    )) {
        selector.appendChild(createElement("option").also {
            it.setAttribute("value", name)
            it.setText(title)
        })
    }
    selector.value = active
    selector.listen("change", controller) {
        if (selector.value != active) navigateToDemo(selector.value, locale)
    }
    windowRef().listen("pagehide", controller) { controller.abort() }
    label.appendChild(selector)
    navigation.setText("")
    navigation.appendChild(label)
    navigation.hidden = false
}

/** A full navigation runs pagehide cleanup; the runner path keeps the current JS/Wasm target. */
private fun navigateToDemo(name: String, locale: String): Unit = js(
    "globalThis.location.assign(globalThis.location.pathname + '?demo=' + " +
        "encodeURIComponent(name) + '&lang=' + encodeURIComponent(locale))",
)
