package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A validation scope with no operation inside resolves to `null` and does not fail.
 */
suspend fun emptyErrorScope(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    val result = device.popErrorScope()
    assertTrue(result.isSuccess)
    assertNull(result.getOrThrow())
}

/**
 * A buffer with no usage fails validation; the error scope captures a [GPUValidationError].
 */
suspend fun invalidBufferUsage(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }
}

/**
 * A mapping offset that is not 8-byte aligned fails validation and leaves the buffer unmapped.
 */
suspend fun invalidMapAlignment(device: GPUDevice) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
    ).use { buffer ->
        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            val result = buffer.mapAsync(GPUMapMode.Read, 4uL, 4uL)
            assertTrue(result.isFailure, "Mapping offset must be aligned to 8 bytes")
            assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
        } finally {
            assertIs<GPUValidationError>(device.popErrorScope().getOrThrow())
        }
    }
}

/**
 * Mapping a destroyed buffer fails and is reported through the validation error scope.
 */
suspend fun mappingDestroyedBuffer(device: GPUDevice) {
    val buffer = device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
    )
    buffer.close()

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        assertTrue(buffer.mapAsync(GPUMapMode.Read).isFailure)
    } finally {
        assertIs<GPUValidationError>(device.popErrorScope().getOrThrow())
    }
}
