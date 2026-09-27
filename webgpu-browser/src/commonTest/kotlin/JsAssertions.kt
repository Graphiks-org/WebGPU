@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import kotlin.js.JsAny
import kotlin.js.js

internal fun propertyNumber(value: JsAny?, key: String): Double = js("value[key]")
internal fun propertyString(value: JsAny?, key: String): String = js("value[key]")
internal fun propertyValue(value: JsAny?, key: String): JsAny = js("value[key]")
internal fun sameJs(left: JsAny?, right: JsAny?): Boolean = js("left === right")
internal fun hasOwn(value: JsAny?, key: String): Boolean = js("Object.hasOwn(value, key)")
