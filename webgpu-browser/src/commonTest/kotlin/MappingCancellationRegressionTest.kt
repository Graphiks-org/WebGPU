@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.graphiks.webgpu.browser

import js.promise.Promise
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.bindings.WGPUBuffer
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A stateful double of a WebGPU buffer that reproduces the backend's mapping state machine:
 * a second `mapAsync` is rejected while the buffer is `pending` or `mapped`, and `unmap`
 * rejects the still-pending request. This is the constraint the previous fake handle did not
 * model, which let two mappings overlap in the older cancellation tests.
 */
private external interface StatefulMappingBuffer : WGPUBuffer {
    var unmapCount: Double
    var mapAsyncCount: Double
    fun finishMapping()
    fun failMapping()
}

private fun statefulMappingBuffer(): StatefulMappingBuffer = js(
    """({
    mapState: 'unmapped',
    resolvePending: null,
    rejectPending: null,
    unmapCount: 0,
    mapAsyncCount: 0,
    mapAsync: function() {
        this.mapAsyncCount++;
        if (this.mapState !== 'unmapped') return Promise.reject(new Error('mapping already pending; state=' + this.mapState));
        this.mapState = 'pending';
        var self = this;
        return new Promise(function(resolve, reject) {
            self.resolvePending = resolve;
            self.rejectPending = reject;
        });
    },
    unmap: function() {
        this.unmapCount++;
        var reject = this.rejectPending;
        this.resolvePending = null;
        this.rejectPending = null;
        this.mapState = 'unmapped';
        if (reject) reject(new Error('mapping aborted'));
    },
    finishMapping: function() {
        var resolve = this.resolvePending;
        if (resolve) {
            this.mapState = 'mapped';
            this.resolvePending = null;
            this.rejectPending = null;
            resolve();
        }
    },
    failMapping: function() {
        var reject = this.rejectPending;
        if (reject) {
            this.resolvePending = null;
            this.rejectPending = null;
            reject(new Error('mapping failed'));
        }
    }
})""",
)

private fun eventLoopTurn(): Promise<JsAny?> =
    js("new Promise(function(resolve) { setTimeout(resolve, 0); })")

private suspend fun pumpEventLoop() {
    eventLoopTurn().await()
}

/**
 * Regression for audit defect F1: cancelling a mapping request that has really started must
 * release the buffer immediately, so a mapping started right after the cancellation is not
 * rejected by the backend and is not invalidated by the late settlement of the first request.
 */
class MappingCancellationRegressionTest {

    @Test
    fun cancellationOfStartedMappingAllowsImmediateRemap() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        val first = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        first.cancelAndJoin()
        assertEquals(
            GPUBufferMapState.Unmapped,
            buffer.mapState,
            "Cancelling a started mapping must release the buffer immediately",
        )

        val second = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        // The backend completes the second request: the mapping is delivered to its caller.
        raw.finishMapping()
        pumpEventLoop()

        val result = second.await()
        assertTrue(result.isSuccess, "Immediate remap after cancellation failed: ${result.exceptionOrNull()}; state=${buffer.mapState}")
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)
        assertEquals(1.0, raw.unmapCount, "Only the cancellation may unmap; the late settlement must not unmap again")
        buffer.unmap()
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
    }

    @Test
    fun rejectedSecondMappingDoesNotStealThePendingRequest() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        val first = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        // A second request while the first is pending is rejected by the backend.
        val rejected = buffer.mapAsync(GPUMapMode.Write)
        assertTrue(rejected.isFailure, "A second mapping while one is pending must be rejected")
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        // The rejected request must not take over the pending one: the first request is still
        // the owner, so cancelling it releases the buffer.
        first.cancelAndJoin()
        assertEquals(
            GPUBufferMapState.Unmapped,
            buffer.mapState,
            "Cancelling the pending request after a rejected second request must release the buffer",
        )
        assertEquals(1.0, raw.unmapCount, "The rejected request must not have unmapped the pending mapping")

        // The buffer is free again: a new mapping succeeds.
        val third = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        raw.finishMapping()
        pumpEventLoop()
        assertTrue(third.await().isSuccess, "A mapping after the rejected one must succeed")
        buffer.unmap()
    }

    @Test
    fun deliveredMappingIsNotUnmappedByLaterCancellation() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        val mapping = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        raw.finishMapping()
        pumpEventLoop()
        val result = mapping.await()
        assertTrue(result.isSuccess, "The mapping must succeed: ${result.exceptionOrNull()}")
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)

        mapping.cancelAndJoin()
        pumpEventLoop()
        assertEquals(
            GPUBufferMapState.Mapped,
            buffer.mapState,
            "A delivered mapping belongs to the caller; a later cancellation must not unmap it",
        )
        assertEquals(0.0, raw.unmapCount)
        buffer.unmap()
    }

    @Test
    fun lateRejectionAfterCancellationIsSwallowed() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        val mapping = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        mapping.cancelAndJoin()
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)

        // The backend rejects the cancelled request afterwards: no unhandled rejection, no unmap.
        // Note: after the cancellation unmap, the fake has no pending callback, so failMapping is
        // a no-op here; the real coverage of a late rejection is the race test below.
        raw.failMapping()
        pumpEventLoop()
        assertEquals(1.0, raw.unmapCount, "The cancellation already released the buffer")
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
    }

    // -----------------------------------------------------------------------------------------
    // Courses identifiées par relecture : une CancellationException ne doit pas unmap quand la
    // demande n'a jamais été acceptée par le backend (rejetée via promesse, pas throw synchrone).
    // -----------------------------------------------------------------------------------------

    @Test
    fun cancellationOfRejectedRequestDoesNotUnmapTheDeliveredMapping() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        // A first mapping is delivered and stays mapped: the caller owns it.
        val owned = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        raw.finishMapping()
        pumpEventLoop()
        assertTrue(owned.await().isSuccess)
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)

        // A second request is rejected by the backend because the buffer is mapped. The
        // rejection is delivered through the promise, not synchronously: cancelling the request
        // immediately (before the rejection is processed) must not unmap the delivered mapping.
        val rejected = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        // No pumpEventLoop, no await: the rejection microtask has not run yet.
        rejected.cancelAndJoin()

        assertEquals(
            GPUBufferMapState.Mapped,
            buffer.mapState,
            "Cancelling a rejected request must not unmap the mapping the first caller owns",
        )
        assertEquals(0.0, raw.unmapCount, "No unmap may happen: the rejected request never mapped")

        // Let the rejection settle: it must be swallowed without side effects.
        pumpEventLoop()
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)
        assertEquals(0.0, raw.unmapCount)
        buffer.unmap()
    }

    @Test
    fun cancellationOfRejectedRequestDoesNotUnmapThePendingMapping() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        // A first request is pending: the backend accepted it.
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        // A second request is rejected (buffer pending). Cancel it immediately, before the
        // rejection microtask runs: it must not unmap the pending mapping of the first request.
        val rejected = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        rejected.cancelAndJoin()

        assertEquals(
            GPUBufferMapState.Pending,
            buffer.mapState,
            "Cancelling a rejected request must not unmap the pending mapping",
        )
        assertEquals(0.0, raw.unmapCount, "No unmap may happen: the rejected request never mapped")

        // The first request is still the owner: completing it maps the buffer for its caller.
        raw.finishMapping()
        pumpEventLoop()
        assertTrue(first.await().isSuccess, "The pending mapping must succeed")
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)
        buffer.unmap()
    }

    @Test
    fun cancellationOfPendingRequestIsNotIgnoredByLaterRejections() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        // A is pending (accepted by the backend).
        val a = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        // B and C are rejected (buffer pending). Their rejections are microtasks, not yet run.
        val b = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        val c = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }

        // Cancel A immediately, before the rejections of B and C are delivered. A is the owner:
        // its cancellation must release the buffer even though B and C have claimed identities.
        a.cancelAndJoin()
        assertEquals(
            GPUBufferMapState.Unmapped,
            buffer.mapState,
            "Cancelling the pending owner must release the buffer despite later rejected requests",
        )
        assertEquals(1.0, raw.unmapCount)

        // The rejections of B and C settle afterwards: swallowed, no further unmap.
        pumpEventLoop()
        assertTrue(b.isCancelled || b.await().isFailure)
        assertTrue(c.isCancelled || c.await().isFailure)
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
        assertEquals(1.0, raw.unmapCount)

        // The buffer is free: a new mapping succeeds.
        val d = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        raw.finishMapping()
        pumpEventLoop()
        assertTrue(d.await().isSuccess, "A mapping after the cancelled owner must succeed")
        buffer.unmap()
    }

    @Test
    fun resolutionBeforeDeliveryThenCancellationUnmaps() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        // The request is pending.
        val mapping = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        assertEquals(GPUBufferMapState.Pending, buffer.mapState)

        // The backend resolves the request (state becomes mapped), but the coroutine has not
        // resumed yet: the value is produced but not delivered.
        raw.finishMapping()
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)

        // Cancel before the delivery: the produced mapping is undelivered and must be released.
        mapping.cancelAndJoin()
        pumpEventLoop()

        assertEquals(
            GPUBufferMapState.Unmapped,
            buffer.mapState,
            "A mapping resolved but never delivered must be unmapped",
        )
        assertEquals(1.0, raw.unmapCount, "The undelivered mapping must be unmapped exactly once")
    }

    @Test
    fun explicitUnmapThenNewMappingIsNotBrokenByLateCancellationOfTheOldRequest() = runTest {
        val raw = statefulMappingBuffer()
        val buffer = Buffer(raw)

        // A first mapping is delivered and explicitly unmapped by its caller.
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        raw.finishMapping()
        pumpEventLoop()
        assertTrue(first.await().isSuccess)
        buffer.unmap()
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
        assertEquals(1.0, raw.unmapCount)

        // A second mapping starts and is delivered.
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            buffer.mapAsync(GPUMapMode.Write)
        }
        raw.finishMapping()
        pumpEventLoop()
        assertTrue(second.await().isSuccess)
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)

        // A late cancellation of the first (already completed) coroutine must not unmap the
        // second mapping.
        first.cancelAndJoin()
        pumpEventLoop()
        assertEquals(
            GPUBufferMapState.Mapped,
            buffer.mapState,
            "A late cancellation of a completed request must not unmap the current mapping",
        )
        assertEquals(1.0, raw.unmapCount, "No additional unmap may happen")
        buffer.unmap()
    }
}
