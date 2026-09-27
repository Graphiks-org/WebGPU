@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.bindings

import kotlin.js.JsAny
import kotlin.js.js

external interface WebGpuRecord : JsAny

fun createWebGpuRecord(): WebGpuRecord = js("Object.create(null)")

fun setRecordValue(record: WebGpuRecord, key: String, value: JsAny): Unit =
    js("{ record[key] = value; }")
