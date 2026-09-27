@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.integration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BrowserErrorsTest {

    @Test
    fun validationErrorsAreClassified() = runTest(timeout = 60.seconds) {
        val adapter = requestAdapter().getOrThrow()
        val device = adapter.requestDevice().getOrThrow()
        try {
            device.pushErrorScope(GPUErrorFilter.Validation)
            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None))
            val error = device.popErrorScope().getOrThrow()
            assertTrue(error is GPUValidationError)
            assertTrue(error.message.isNotEmpty())
        } finally {
            device.close()
            adapter.close()
        }
    }

    @Test
    fun uncapturedErrorCallbackReceivesErrors() = runTest(timeout = 60.seconds) {
        val adapter = requestAdapter().getOrThrow()
        val deferred = CompletableDeferred<GPUError>()
        val device = adapter.requestDevice(
            DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback { deferred.complete(it) }),
        ).getOrThrow()
        try {
            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None))
            val error = withContext(Dispatchers.Default) {
                withTimeout(10.seconds) { deferred.await() }
            }
            assertTrue(error is GPUValidationError)
        } finally {
            device.close()
            adapter.close()
        }
    }
}
