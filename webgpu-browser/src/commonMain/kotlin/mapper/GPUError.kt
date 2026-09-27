package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.browser.InternalError
import org.graphiks.webgpu.browser.OutOfMemoryError
import org.graphiks.webgpu.browser.ValidationError
import org.graphiks.webgpu.bindings.WGPUError

internal fun errorOf(value: WGPUError): GPUError = when {
    isGPUValidationError(value) -> ValidationError(value.message)
    isGPUInternalError(value) -> InternalError(value.message)
    isGPUOutOfMemoryError(value) -> OutOfMemoryError(value.message)
    else -> InternalError(value.message)
}

internal expect fun isGPUValidationError(error: WGPUError): Boolean
internal expect fun isGPUInternalError(error: WGPUError): Boolean
internal expect fun isGPUOutOfMemoryError(error: WGPUError): Boolean
