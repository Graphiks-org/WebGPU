@file:OptIn(ExperimentalWasmJsInterop::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.graphiks.webgpu.browser

import js.promise.Promise
import kotlinx.coroutines.async
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

private fun countedBuffer(): CountedBuffer =
    js(
        "({ unmapCount: 0, currentPromise: null, " +
            "unmap: function() { this.unmapCount++; }, " +
            "mapAsync: function() { return this.currentPromise; } })",
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
        raw.currentPromise = promiseB.promise
        val mappingB = async { buffer.mapAsync(GPUMapMode.Write, 0uL, null) }
        yield()
        mappingA.cancel()
        promiseA.resolve(jsUndefined())
        pumpEventLoop()
        promiseB.resolve(jsUndefined())

        val resultB = mappingB.await()
        assertTrue(resultB.isSuccess, "The newer mapping must succeed")
        assertEquals(
            0,
            raw.unmapCount,
            "A late settlement of the cancelled mapping must not unmap the newer mapping",
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
        assertEquals(0, raw.unmapCount, "A rejected mapping never mapped, so there is nothing to unmap")
    }
}
