@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu

import kotlin.js.toJsNumber

@Suppress("NOTHING_TO_INLINE")
actual inline fun Float.asJsNumber(): JsNumber = toJsNumber()
@Suppress("NOTHING_TO_INLINE")
actual inline fun Double.asJsNumber(): JsNumber = toJsNumber()
@Suppress(names = ["NOTHING_TO_INLINE"])
actual inline fun Byte.asJsNumber(): JsNumber = toJsNumber()
@Suppress("NOTHING_TO_INLINE")
actual inline fun Short.asJsNumber(): JsNumber = toJsNumber()
@Suppress("NOTHING_TO_INLINE")
actual inline fun Int.asJsNumber(): JsNumber = toJsNumber()
@Suppress("NOTHING_TO_INLINE")
actual inline fun Long.asJsNumber(): JsNumber = toUInt().toJsNumber()
@Suppress(names = ["NOTHING_TO_INLINE"])
actual inline fun UByte.asJsNumber(): JsNumber  = toJsNumber()
@Suppress(names = ["NOTHING_TO_INLINE"])
actual inline fun UShort.asJsNumber(): JsNumber = toJsNumber()
@Suppress(names = ["NOTHING_TO_INLINE"])
actual inline fun UInt.asJsNumber(): JsNumber = toJsNumber()
@Suppress(names = ["NOTHING_TO_INLINE"])
actual inline fun ULong.asJsNumber(): JsNumber = toUInt().toJsNumber()

fun UByte.toJsNumber(): kotlin.js.JsNumber = toJsNumber(this)
private fun toJsNumber(x: UByte): kotlin.js.JsNumber = js("x")

fun UShort.toJsNumber(): kotlin.js.JsNumber = toJsNumber(this)
private fun toJsNumber(x: UShort): kotlin.js.JsNumber = js("x")

fun UInt.toJsNumber(): kotlin.js.JsNumber = toJsNumber(this)
private fun toJsNumber(x: UInt): kotlin.js.JsNumber = js("x")

fun Float.toJsNumber(): kotlin.js.JsNumber = toJsNumber(this)
private fun toJsNumber(x: Float): kotlin.js.JsNumber = js("x")

fun Byte.toJsNumber(): kotlin.js.JsNumber = toJsNumber(this)
private fun toJsNumber(x: Byte): kotlin.js.JsNumber = js("x")

fun Short.toJsNumber(): kotlin.js.JsNumber = toJsNumber(this)
private fun toJsNumber(x: Short): kotlin.js.JsNumber = js("x")

fun Long.toJsNumber(): kotlin.js.JsNumber = toJsNumber(this)
private fun toJsNumber(x: Long): kotlin.js.JsNumber = js("x")
