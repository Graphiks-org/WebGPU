@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.graphiks.webgpu.browser

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.bindings.GPU
import org.graphiks.webgpu.bindings.WGPUBuffer
import org.graphiks.webgpu.bindings.WGPUDevice
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun emptyScopeDevice(): WGPUDevice = js("({ popErrorScope: () => Promise.resolve(null) })")
private fun rejectedScopeDevice(): WGPUDevice = js("({ popErrorScope: () => Promise.reject(new Error('scope rejected')) })")
private fun rejectingBuffer(): WGPUBuffer = js("({ mapAsync: () => Promise.reject(new Error('map rejected')) })")
private fun fulfilledBuffer(): WGPUBuffer = js("({ mapAsync: () => Promise.resolve() })")
private fun pendingBuffer(): WGPUBuffer = js("({ mapAsync: () => new Promise(() => {}) })")
private fun noAdapterGpu(): GPU = js("({ requestAdapter: () => Promise.resolve(null) })")
private fun rejectingGpu(): GPU = js("({ requestAdapter: () => Promise.reject(new Error('adapter rejected')) })")

class AsyncContractTest {

    @Test
    fun emptyErrorScopeIsSuccessfulNull() = runTest {
        val result = Device(emptyScopeDevice()).popErrorScope()
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun rejectedPromisesBecomeFailures() = runTest {
        assertTrue(Device(rejectedScopeDevice()).popErrorScope().isFailure)
        assertTrue(Buffer(rejectingBuffer()).mapAsync(GPUMapMode.Read).isFailure)
        assertTrue(Buffer(fulfilledBuffer()).mapAsync(GPUMapMode.Read).isSuccess)
        assertTrue(requestAdapter(null, null).isFailure)
        assertTrue(requestAdapter(noAdapterGpu(), null).isFailure)
        assertTrue(requestAdapter(rejectingGpu(), null).isFailure)
    }

    @Test
    fun cancellationDoesNotReturnAResult() = runTest {
        var returned = false
        val job = launch {
            Buffer(pendingBuffer()).mapAsync(GPUMapMode.Read)
            returned = true
        }
        runCurrent()
        job.cancelAndJoin()
        assertFalse(returned)
    }
}
