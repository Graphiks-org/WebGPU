@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import kotlin.OptIn
import kotlin.String
import kotlin.error
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toInt
import kotlin.toUInt

class QuerySet(val handler: WGPUQuerySet): GPUQuerySet {
    override val count: GPUSize32Out
        get() = handler.count.toInt().toUInt()
    override val type: GPUQueryType
        get() = GPUQueryType.of(handler.type) ?: error("Unknown query type ${handler.type}")
    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override fun close() {
        handler.destroy()
    }
}
