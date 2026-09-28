package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A validation scope with no operation inside resolves to `null` and does not fail.
 */
@AcidTest(
    id = AcidCaseId.ErrorsEmptyScope,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun emptyErrorScope(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    val result = device.popErrorScope()
    assertTrue(result.isSuccess)
    assertNull(result.getOrThrow())
}

/**
 * A buffer with no usage fails validation; the error scope captures a [GPUValidationError].
 */
@AcidTest(
    id = AcidCaseId.ErrorsInvalidBufferUsage,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
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
@AcidTest(
    id = AcidCaseId.ErrorsMapAlignment,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_mapState,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
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
@AcidTest(
    id = AcidCaseId.BuffersMapDestroyed,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_close,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
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
