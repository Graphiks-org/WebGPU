@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.errorOf

internal actual fun configureUncapturedError(handler: WGPUDevice, callback: GPUUncapturedErrorCallback) {
    handler.unsafeCast<DeviceInternal>()
        .addEventListener("uncapturederror", { event ->
            callback.onUncapturedError(errorOf(event.error))
        })
}

internal external class DeviceInternal : JsAny {
    fun addEventListener(type: String, callback: (Event) -> Unit)
}

internal external class Event : JsAny {
    val error: WGPUError
}
