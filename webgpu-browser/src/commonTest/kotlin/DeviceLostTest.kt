@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import js.promise.Promise
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.bindings.WGPUDevice
import org.graphiks.webgpu.bindings.WGPUDeviceLostInfo
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun fakeDevice(lost: Promise<JsAny>): Device {
    val handler = createJsObject<WGPUDevice>()
    handler.lost = lost
    return Device(handler)
}

private fun lostInfo(reason: String, message: String): WGPUDeviceLostInfo =
    createJsObject<WGPUDeviceLostInfo>().apply {
        this.reason = reason
        this.message = message
    }

private fun rejectedPromise(): Promise<JsAny> = js("Promise.reject(new Error('boom'))")

class DeviceLostTest {

    @Test
    fun twoObserversObserveTheSameLoss() = runTest {
        var resolveLost: ((JsAny) -> Unit)? = null
        val lost = Promise<JsAny> { resolve, _ -> resolveLost = { value -> resolve(value) } }
        val device = fakeDevice(lost)

        val first = async { device.awaitLost() }
        val second = async { device.awaitLost() }
        yield()

        resolveLost?.invoke(lostInfo("destroyed", "device destroyed"))

        val firstInfo = first.await().getOrThrow()
        val secondInfo = second.await().getOrThrow()
        assertEquals(GPUDeviceLostReason.Destroyed, firstInfo.reason)
        assertEquals("device destroyed", firstInfo.message)
        assertEquals(GPUDeviceLostReason.Destroyed, secondInfo.reason)
        assertEquals("device destroyed", secondInfo.message)
    }

    @Test
    fun cancelledObserverKeepsOtherAndLateObserversWorking() = runTest {
        var resolveLost: ((JsAny) -> Unit)? = null
        val lost = Promise<JsAny> { resolve, _ -> resolveLost = { value -> resolve(value) } }
        val device = fakeDevice(lost)

        val cancelled = async { device.awaitLost() }
        yield()
        cancelled.cancel()
        assertTrue(cancelled.isCancelled)

        val survivor = async { device.awaitLost() }
        yield()
        resolveLost?.invoke(lostInfo("unknown", "gpu gone"))

        assertEquals(GPUDeviceLostReason.Unknown, survivor.await().getOrThrow().reason)

        // An observer that starts after the loss resolves immediately with the same information.
        val late = device.awaitLost().getOrThrow()
        assertEquals(GPUDeviceLostReason.Unknown, late.reason)
        assertEquals("gpu gone", late.message)
    }

    @Test
    fun unknownBackendReasonMapsToUnknownNotDestroyed() = runTest {
        var resolveLost: ((JsAny) -> Unit)? = null
        val lost = Promise<JsAny> { resolve, _ -> resolveLost = { value -> resolve(value) } }
        val device = fakeDevice(lost)

        val waiting = async { device.awaitLost() }
        yield()
        resolveLost?.invoke(lostInfo("some-future-reason", "mystery"))

        val info = waiting.await().getOrThrow()
        assertEquals(GPUDeviceLostReason.Unknown, info.reason)
        assertEquals("mystery", info.message)
    }

    @Test
    fun rejectedLossPromiseIsAResultFailure() = runTest {
        val device = fakeDevice(rejectedPromise())
        val result = device.awaitLost()
        assertTrue(result.isFailure, "An interop failure must surface as a Result failure")
    }
}
