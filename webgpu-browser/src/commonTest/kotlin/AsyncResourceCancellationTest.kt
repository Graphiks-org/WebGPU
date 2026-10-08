@file:OptIn(ExperimentalWasmJsInterop::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.graphiks.webgpu.browser

import js.promise.Promise
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.bindings.WGPUAdapter
import org.graphiks.webgpu.bindings.WGPUBuffer
import org.graphiks.webgpu.bindings.WGPUDevice
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class ControlledPromise {
    private var resolveCallback: ((JsAny?) -> Unit)? = null
    val promise: Promise<JsAny?> = Promise { resolve, _ ->
        resolveCallback = { value -> resolve(value) }
    }

    fun resolve(value: JsAny?) {
        resolveCallback?.invoke(value)
    }
}

private class DeliveryCounters {
    var created = 0
    var delivered = 0
    var destroyed = 0

    fun resolve(controlled: ControlledPromise, value: JsAny) {
        created++
        controlled.resolve(value)
    }
}

private fun fakeResource(): JsAny = js("({})")

private fun jsUndefined(): JsAny? = js("undefined")

private fun rejectedPromise(): Promise<JsAny> = js("Promise.reject(new Error('boom'))")

private fun macrotaskPromise(): Promise<JsAny> =
    js("new Promise(function(resolve) { setTimeout(resolve, 0); })")

/**
 * Lets the JavaScript event loop run so promise callbacks (microtasks) settle. A virtual `delay`
 * only advances the test scheduler and never turns the real event loop.
 */
private suspend fun pumpEventLoop() {
    macrotaskPromise().await()
}

private external interface CountedDevice : WGPUDevice {
    var destroyCount: Int
}

private fun countedDevice(): CountedDevice =
    js("({ destroyCount: 0, destroy: function() { this.destroyCount++; } })")

private fun fakeAdapter(devicePromise: Promise<JsAny?>): WGPUAdapter =
    js("({ requestDevice: function() { return devicePromise; } })")

private external interface CountedBuffer : WGPUBuffer {
    var unmapCount: Int
    var currentPromise: Promise<JsAny?>?
}

/**
 * A stateful fake buffer: `mapAsync` moves the buffer from `unmapped` to `pending` (the backend
 * accepts synchronously) and returns the controlled promise; a second request while the buffer
 * is not `unmapped` is rejected, like a real backend. `unmap` returns the buffer to `unmapped`.
 */
private fun countedBuffer(): CountedBuffer =
    js(
        "({ unmapCount: 0, currentPromise: null, mapState: 'unmapped', " +
            "unmap: function() { this.unmapCount++; this.mapState = 'unmapped'; }, " +
            "mapAsync: function() { " +
            "  if (this.mapState !== 'unmapped') return Promise.reject(new Error('mapping already pending')); " +
            "  this.mapState = 'pending'; return this.currentPromise; } })",
    )

class AsyncResourceCancellationTest {

    @Test
    fun cancellationBeforeResolutionCreatesNothing() = runTest {
        val counters = DeliveryCounters()
        val controlled = ControlledPromise()
        val job = launch {
            controlled.promise.await { counters.destroyed++ }
            counters.delivered++
        }
        yield()
        job.cancel()
        pumpEventLoop()
        assertEquals(0, counters.created)
        assertEquals(0, counters.delivered)
        assertEquals(0, counters.destroyed)
    }

    @Test
    fun cancellationAfterResolutionDestroysTheUndeliveredResource() = runTest {
        val counters = DeliveryCounters()
        val controlled = ControlledPromise()
        val job = launch {
            controlled.promise.await { counters.destroyed++ }
            counters.delivered++
        }
        yield()
        counters.resolve(controlled, fakeResource())
        job.cancel()
        pumpEventLoop()
        assertEquals(1, counters.created)
        assertEquals(0, counters.delivered)
        assertEquals(1, counters.destroyed)
    }

    @Test
    fun deliveredResourceIsNotDestroyedByLaterCancellation() = runTest {
        val counters = DeliveryCounters()
        val controlled = ControlledPromise()
        val job = launch {
            controlled.promise.await { counters.destroyed++ }
            counters.delivered++
        }
        yield()
        counters.resolve(controlled, fakeResource())
        pumpEventLoop()
        assertEquals(1, counters.delivered)
        assertEquals(0, counters.destroyed)
        job.cancel()
        pumpEventLoop()
        assertEquals(1, counters.delivered)
        assertEquals(0, counters.destroyed)
    }

    @Test
    fun cancelledRequestDeviceDestroysTheUndeliveredDevice() = runTest {
        val controlled = ControlledPromise()
        val adapter = Adapter(fakeAdapter(controlled.promise))
        val raw = countedDevice()
        val job = launch { adapter.requestDevice(null) }
        yield()
        controlled.resolve(raw)
        job.cancel()
        pumpEventLoop()
        assertEquals(1, raw.destroyCount, "A device created but never delivered must be destroyed")
    }

    @Test
    fun requestDeviceCancelledBeforeResolutionDestroysNothingUntilItResolves() = runTest {
        val controlled = ControlledPromise()
        val adapter = Adapter(fakeAdapter(controlled.promise))
        val raw = countedDevice()
        val job = launch { adapter.requestDevice(null) }
        yield()
        job.cancel()
        pumpEventLoop()
        assertEquals(0, raw.destroyCount)
        // The backend still creates the device after the caller is gone: it must be destroyed once.
        controlled.resolve(raw)
        pumpEventLoop()
        assertEquals(1, raw.destroyCount)
    }

    @Test
    fun deliveredDeviceSurvivesLaterCancellation() = runTest {
        val controlled = ControlledPromise()
        val adapter = Adapter(fakeAdapter(controlled.promise))
        val raw = countedDevice()
        val job = async { adapter.requestDevice(null) }
        yield()
        controlled.resolve(raw)
        val device = job.await().getOrThrow() as Device
        assertSame(raw, device.handler)
        job.cancel()
        pumpEventLoop()
        assertEquals(0, raw.destroyCount, "A delivered device must not be destroyed by a later cancellation")
    }

    @Test
    fun cancelledMappingUnmapsUnlessANewerMappingStarted() = runTest {
        val raw = countedBuffer()
        val promiseA = ControlledPromise()
        val promiseB = ControlledPromise()
        raw.currentPromise = promiseA.promise
        val buffer = Buffer(raw)

        val mappingA = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()
        // The stateful fake rejects a second request while the first is pending, like a real
        // backend: mappingB never starts, so it cannot own the buffer.
        raw.currentPromise = promiseB.promise
        val mappingB = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()
        mappingA.cancel()
        promiseA.resolve(jsUndefined())
        pumpEventLoop()
        promiseB.resolve(jsUndefined())

        val resultB = mappingB.await()
        assertTrue(resultB.isFailure, "The second mapping must be rejected while the first is pending")
        assertEquals(
            1,
            raw.unmapCount,
            "Cancelling the accepted pending mapping must release the buffer exactly once",
        )
    }

    @Test
    fun cancelledMappingWithoutSuccessorIsUnmapped() = runTest {
        val raw = countedBuffer()
        val promiseA = ControlledPromise()
        raw.currentPromise = promiseA.promise
        val buffer = Buffer(raw)

        val mappingA = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()
        mappingA.cancel()
        promiseA.resolve(jsUndefined())
        pumpEventLoop()
        assertEquals(1, raw.unmapCount, "A cancelled mapping completed by the backend must be unmapped")
    }

    @Test
    fun rejectedMappingAfterCancellationIsSwallowed() = runTest {
        val raw = countedBuffer()
        raw.currentPromise = rejectedPromise()
        val buffer = Buffer(raw)

        val mappingA = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()
        mappingA.cancel()
        pumpEventLoop()
        assertTrue(mappingA.isCancelled)
        // The stateful fake accepted the request (mapState became 'pending'), so the wrapper
        // releases the buffer at cancellation time. The late rejection is swallowed.
        assertEquals(1, raw.unmapCount, "Cancelling a started mapping releases the buffer immediately")
    }

    @Test
    fun rejectedMappingWhileAnotherIsPendingDoesNotUnmapIt() = runTest {
        val raw = countedBuffer()
        val promiseA = ControlledPromise()
        raw.currentPromise = promiseA.promise
        val buffer = Buffer(raw)

        val mappingA = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()
        // The stateful fake rejects a second request while the first is pending, like a real
        // backend. The rejected request never becomes the owner.
        val rejected = buffer.mapAsync(GPUMapMode.Write, 0uL, null)
        assertTrue(rejected.isFailure, "The second mapping must be rejected")
        pumpEventLoop()

        // The first request is still the owner: cancelling it releases the buffer exactly once.
        mappingA.cancel()
        pumpEventLoop()
        assertEquals(1, raw.unmapCount, "The rejected request must not have unmapped the pending mapping")
    }

    @Test
    fun rejectedMappingCancelledBeforeRejectionDoesNotUnmapTheOwner() = runTest {
        val raw = countedBuffer()
        val promiseA = ControlledPromise()
        raw.currentPromise = promiseA.promise
        val buffer = Buffer(raw)

        // A is accepted (pending).
        val mappingA = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()

        // B is rejected by the stateful fake (buffer pending). Cancel B immediately, before the
        // rejection microtask runs: B never owned the mapping, so it must not unmap A's.
        val mappingB = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()
        mappingB.cancelAndJoin()

        assertEquals(0, raw.unmapCount, "A rejected request must not unmap the pending owner")

        // A is still the owner: completing it maps the buffer for its caller.
        promiseA.resolve(jsUndefined())
        pumpEventLoop()
        assertTrue(mappingA.await().isSuccess, "The pending mapping must succeed")
        assertEquals(0, raw.unmapCount)
    }
}
