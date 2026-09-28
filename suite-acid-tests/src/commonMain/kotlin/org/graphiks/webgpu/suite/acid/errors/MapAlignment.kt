package org.graphiks.webgpu.suite.acid.errors

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
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
