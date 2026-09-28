@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser.demos

import js.coroutines.resumeWithError
import js.promise.Promise
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.js
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The small browser boundary shared by the JS and Wasm builds of the demo.
 *
 * Everything the page needs from the surrounding document lives here: element creation, reading and
 * writing, query parameters, localization storage, animation frames and text loading. The rest of the
 * demo is ordinary Kotlin, and no `dynamic` is used so the same code compiles for Wasm.
 */

/** A DOM event handed to a listener. The demo never inspects it. */
internal external class DomEvent : JsAny

/** Anything that can receive DOM events. */
internal external interface DomEventTarget : JsAny {
    fun addEventListener(type: String, listener: (DomEvent) -> Unit, options: JsAny)
}

/** A DOM node the demo can read, write and append to. */
internal external interface DomElement : DomEventTarget {
    var textContent: String?
    var hidden: Boolean
    var disabled: Boolean
    var value: String
    var className: String
    fun setAttribute(name: String, value: String)
    fun appendChild(child: JsAny): JsAny
}

/** The subset of `document` the demo needs. */
internal external interface DomDocument : DomEventTarget {
    fun getElementById(id: String): DomElement?
    fun createElement(tagName: String): DomElement
}

/** The subset of `window` the demo needs. */
internal external interface DomWindow : DomEventTarget

/** A canvas element: its drawing buffer (`width`/`height`) and its CSS layout size (`client*`). */
internal external interface DomCanvas : JsAny {
    var width: Int
    var height: Int
    val clientWidth: Int
    val clientHeight: Int
}

/** An abort controller whose signal removes every listener registered with it in one call. */
internal external class AbortController : JsAny {
    val signal: JsAny
    fun abort()
}

internal fun documentRef(): DomDocument = js("document")

internal fun windowRef(): DomWindow = js("window")

internal fun newAbortController(): AbortController = js("new AbortController()")

internal fun elementById(id: String): DomElement? = documentRef().getElementById(id)

internal fun createElement(tagName: String): DomElement = documentRef().createElement(tagName)

private fun abortOptions(controller: AbortController): JsAny = js("({ signal: controller.signal })")

/** Registers [listener] for [type]; [controller].abort() removes it, with no per-listener bookkeeping. */
internal fun DomEventTarget.listen(type: String, controller: AbortController, listener: (DomEvent) -> Unit) {
    addEventListener(type, listener, abortOptions(controller))
}

internal fun DomElement.setText(value: String) {
    textContent = value
}

internal fun DomElement.setClass(value: String) {
    className = value
}

/** Reads a query parameter from the current URL. */
internal fun queryParameter(name: String): String? =
    js("new URL(globalThis.location.href).searchParams.get(name)")

/** The locale remembered by the site, if any. */
internal fun storedLocale(): String? = js("globalThis.localStorage.getItem('graphiks-suite-locale')")

/** Resolves the locale from `?lang=`, then from the site's stored choice, then defaults to English. */
internal fun selectedLocale(): String {
    val fromUrl = queryParameter("lang")
    if (fromUrl == "en" || fromUrl == "fr") return fromUrl
    val stored = storedLocale()
    return if (stored == "en" || stored == "fr") stored else "en"
}

/** The device pixel ratio, defaulting to 1 when the browser does not report one. */
internal fun pixelRatio(): Double = js("globalThis.devicePixelRatio || 1")

internal external fun requestAnimationFrame(callback: (Double) -> Unit): JsNumber

internal external fun cancelAnimationFrame(handle: JsNumber)

/** A JS object used to hand a JS string back to Kotlin through the compiler's string conversion. */
private external interface JsText : JsAny {
    val text: String
}

private fun asJsText(value: JsAny): JsText = js("({ text: value })")

private fun fetchPromise(path: String): Promise<JsAny?> =
    js(
        "fetch(path).then(function (response) {" +
            " if (!response.ok) { throw new Error('Cannot load ' + path + ': ' + response.status); }" +
            " return response.text(); })",
    )

/** Fetches [path] and resolves its body as text, failing with the HTTP status on a bad response. */
internal suspend fun fetchText(path: String): String {
    val value = fetchPromise(path).awaitText() ?: error("Loading $path produced no body.")
    return asJsText(value).text
}

private suspend fun <T : JsAny?> Promise<T>.awaitText(): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value ->
                continuation.resume(value)
                null
            },
            onRejected = { error ->
                continuation.resumeWithError(error)
                null
            },
        )
    }
