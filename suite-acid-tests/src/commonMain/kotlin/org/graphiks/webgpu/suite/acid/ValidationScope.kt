package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import kotlin.test.assertNull

/**
 * Pushes a validation error scope around [block] and fails when the device reports an
 * unexpected validation error.
 *
 * The helper does not replace the GPU commands of a case: it only makes the case's own
 * validation failures visible. If the body already failed, the scope failure is attached
 * as a suppressed exception so the original failure stays the reported one.
 */
internal suspend fun withValidationScope(device: GPUDevice, block: suspend () -> Unit) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    var bodyFailure: Throwable? = null
    try {
        block()
    } catch (failure: Throwable) {
        bodyFailure = failure
        throw failure
    } finally {
        try {
            assertNull(device.popErrorScope().getOrThrow(), "Unexpected validation error")
        } catch (scopeFailure: Throwable) {
            val original = bodyFailure
            if (original == null) throw scopeFailure
            original.addSuppressed(scopeFailure)
        }
    }
}
