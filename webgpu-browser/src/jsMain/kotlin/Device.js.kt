package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.errorOf

internal actual fun configureUncapturedError(handler: WGPUDevice, callback: GPUUncapturedErrorCallback) {
    val device: dynamic = handler
    device.addEventListener("uncapturederror", { event: dynamic ->
        callback.onUncapturedError(errorOf(event.error))
    })
}
