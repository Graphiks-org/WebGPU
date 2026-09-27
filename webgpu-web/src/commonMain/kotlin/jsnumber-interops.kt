@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsNumber

expect inline fun Float.asJsNumber(): JsNumber
expect inline fun Double.asJsNumber(): JsNumber
expect inline fun Byte.asJsNumber(): JsNumber
expect inline fun Short.asJsNumber(): JsNumber
expect inline fun Int.asJsNumber(): JsNumber
expect inline fun Long.asJsNumber(): JsNumber

@Suppress("NOTHING_TO_INLINE")
expect inline fun UByte.asJsNumber(): JsNumber
@Suppress("NOTHING_TO_INLINE")
expect inline fun UShort.asJsNumber(): JsNumber
@Suppress("NOTHING_TO_INLINE")
expect inline fun UInt.asJsNumber(): JsNumber
@Suppress("NOTHING_TO_INLINE")
expect inline fun ULong.asJsNumber(): JsNumber