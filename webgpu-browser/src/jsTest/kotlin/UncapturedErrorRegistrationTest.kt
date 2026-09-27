@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.bindings.WGPUDevice
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertTrue

private fun recordingDevice(): JsAny = js(
    "({ listeners: [], addEventListener(type, callback) { this.listeners.push(callback); } })",
)

class UncapturedErrorRegistrationTest {

    @Test
    fun uncapturedErrorIsRegisteredOnce() {
        val raw = recordingDevice()
        Device(raw.unsafeCast<WGPUDevice>(), GPUUncapturedErrorCallback { })
        assertTrue(js("raw.listeners.length === 1"), "the uncaptured error listener must be registered exactly once")
        assertTrue(js("raw.onuncapturederror === undefined"), "the callback must not be registered a second time")
    }
}
