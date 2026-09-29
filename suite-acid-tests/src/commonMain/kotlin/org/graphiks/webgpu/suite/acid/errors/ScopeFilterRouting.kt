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
import kotlin.test.assertNull

/**
 * A validation error raised inside an inner OutOfMemory scope and an outer Validation scope is
 * routed by filter: the inner scope sees nothing, the outer Validation scope receives it. No real
 * memory exhaustion is attempted; the error is the ordinary "usage must not be empty" validation.
 */
@AcidTest(
    id = AcidCaseId.ErrorsScopeFilterRouting,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUErrorFilter_OutOfMemory,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUValidationError,
    ],
)
suspend fun scopeFilterRouting(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.pushErrorScope(GPUErrorFilter.OutOfMemory)
        val inner = try {
            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
            device.popErrorScope().getOrThrow()
        } catch (failure: Throwable) {
            // Keep the scope stack balanced before propagating an unexpected failure.
            runCatching { device.popErrorScope() }
            throw failure
        }
        assertNull(inner, "The OutOfMemory scope must not capture a validation error")
    } finally {
        val outer = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(
            outer,
            "The outer Validation scope must receive the validation error",
        )
    }
}
