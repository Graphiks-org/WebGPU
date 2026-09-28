package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs

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
