@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import kotlin.js.ExperimentalWasmJsInterop

class Buffer(val handler: WGPUBuffer) : GPUBuffer {

    /**
     * Monotonic identity of every `mapAsync` request. Tokens are never reused, so a late
     * settlement of an old request can never be confused with a newer one.
     */
    private var nextRequestId = 0L

    /**
     * Identity of the request that currently owns the buffer's mapping (its request was accepted
     * by the backend and is pending or mapped). `0L` means no request owns the mapping. Only the
     * owner may release the buffer: a rejected request never becomes the owner, so cancelling it
     * can never unmap a mapping it does not own.
     */
    private var ownerRequestId = 0L

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
        val requestId = ++nextRequestId
        // The backend accepts or rejects a mapping request synchronously: an accepted request
        // moves the buffer from 'unmapped' to 'pending' before the promise settles. A request
        // that finds the buffer already 'pending' or 'mapped' is rejected by the backend (the
        // rejection is delivered through the promise, not thrown synchronously).
        val stateBefore = handler.mapState
        val promise = when (size) {
            null -> handler.mapAsync(mode.value.asJsNumber(), offset.asJsNumber())
            else -> handler.mapAsync(mode.value.asJsNumber(), offset.asJsNumber(), size.asJsNumber())
        }
        val accepted = stateBefore == "unmapped" && handler.mapState == "pending"
        if (accepted) ownerRequestId = requestId
        try {
            promise.await()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            // The caller is gone while the request was pending, or after the backend resolved it
            // but before the value was delivered. Release the buffer only if this request is the
            // real owner: a rejected request never owned the mapping, so cancelling it must not
            // unmap a mapping it does not own. Releasing at cancellation time (not at the late
            // settlement) lets a mapping started right after the cancellation succeed.
            if (accepted && ownerRequestId == requestId) {
                ownerRequestId = 0L
                handler.unmap()
            }
            throw cancelled
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
