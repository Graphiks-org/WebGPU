@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import kotlin.js.ExperimentalWasmJsInterop

class Buffer(val handler: WGPUBuffer) : GPUBuffer {

    /**
     * Identity of the latest `mapAsync` call. A cancelled mapping only unmaps when no newer
     * mapping has started in the meantime, so a late settlement of the cancelled request cannot
     * invalidate a mapping that is already active.
     */
    private var mapGeneration = 0L

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }
    override val size: GPUSize64
        get() = handler.size.toULong()
    override val usage: GPUBufferUsage
        get() = GPUBufferUsage.fromBits(handler.usage.toULong())
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
        val generation = ++mapGeneration
        when (size) {
            null -> handler.mapAsync(mode.value.asJsNumber(), offset.asJsNumber())
            else -> handler.mapAsync(mode.value.asJsNumber(), offset.asJsNumber(), size.asJsNumber())
        }.await {
            // This mapping request was cancelled: if the backend still completed it, unmap it,
            // unless a newer mapping has already started on this buffer.
            if (generation == mapGeneration) handler.unmap()
        }
        return@browserResult Unit
    }

    override fun unmap() {
        handler.unmap()
    }

    private var closed = false

    override fun close() {
        // A repeated close must not release the same owned reference twice.
        if (closed) return
        closed = true
        handler.destroy()
    }
}
