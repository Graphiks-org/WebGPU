package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.bindings.WGPUError

actual fun isGPUValidationError(error: WGPUError): Boolean = js("error instanceof GPUValidationError")
actual fun isGPUInternalError(error: WGPUError): Boolean = js("error instanceof GPUInternalError")
actual fun isGPUOutOfMemoryError(error: WGPUError): Boolean = js("error instanceof GPUOutOfMemoryError")
