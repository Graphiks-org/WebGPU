@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import kotlin.js.ExperimentalWasmJsInterop

class Buffer(val handler: WGPUBuffer) : GPUBuffer {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }
    override val size: GPUSize64
        get() = handler.size.toULong()
    override val usage: Set<GPUBufferUsage>
        get() = GPUBufferUsage.entries.filter { it.value and handler.usage.toULong() != 0uL }.toSet()
    override val mapState: GPUBufferMapState
        get() = GPUBufferMapState.of(handler.mapState) ?: error("fail to get MapState")

    override fun getMappedRange(
        offset: GPUSize64,
        size: GPUSize64?
    ): ArrayBuffer = when (size) {
        null -> ArrayBuffer.wrap(handler.getMappedRange(offset.asJsNumber()))
        else -> ArrayBuffer.wrap(handler.getMappedRange(offset.asJsNumber(), size.asJsNumber()))
    }

    override suspend fun mapAsync(
        mode: GPUMapMode,
        offset: GPUSize64,
        size: GPUSize64?
    ): Result<Unit> = browserResult {
        when (size) {
            null -> handler.mapAsync(mode.value.asJsNumber(), offset.asJsNumber())
            else -> handler.mapAsync(mode.value.asJsNumber(), offset.asJsNumber(), size.asJsNumber())
        }.await()
        return@browserResult Unit
    }

    override fun unmap() {
        handler.unmap()
    }

    override fun close() {
        handler.destroy()
    }
}
