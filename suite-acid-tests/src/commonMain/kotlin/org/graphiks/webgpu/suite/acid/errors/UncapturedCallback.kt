package org.graphiks.webgpu.suite.acid.errors

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidInput
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs

/**
 * A dedicated device has an uncaptured-error callback and no scope around a single invalid
 * `createBuffer` (usage None), so the error cannot be captured and must reach the callback. The
 * callback is awaited under a real timeout rather than a sleep; the message is kept for diagnosis
 * and is not used as an oracle.
 *
 * The borrowed device's own error guard is never disabled to make this event observable.
 */
@AcidTest(
    id = AcidCaseId.ErrorsUncapturedError,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUUncapturedErrorCallback,
        ApiSymbols.GPUUncapturedErrorCallback_onUncapturedError,
        ApiSymbols.GPUError,
        ApiSymbols.GPUError_message,
        ApiSymbols.GPUDeviceDescriptor,
        ApiSymbols.GPUDeviceDescriptor_onUncapturedError,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUValidationError,
    ],
    input = AcidInput.Context,
)
suspend fun uncapturedCallback(context: AcidContext) {
    val adapter = context.requestAdapter(null).getOrThrow()
    try {
        val received = CompletableDeferred<GPUError>()
        adapter.requestDevice(
            DeviceDescriptor(
                label = "acid-uncaptured-device-λ",
                onUncapturedError = GPUUncapturedErrorCallback { received.complete(it) },
            ),
        ).getOrThrow().use { device ->
            val invalid = device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None))
            invalid.close()

            // Chromium delivers the pending uncaptured-error event once the queue work is drained;
            // the subsequent await is bounded by a real timeout rather than a sleep.
            device.queue.onSubmittedWorkDone().getOrThrow()
            val error = withTimeout(5.seconds) { received.await() }
            assertIs<GPUValidationError>(
                error,
                "An invalid buffer outside any scope must deliver a validation error to the callback",
            )
        }
    } finally {
        adapter.close()
    }
}
