package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
