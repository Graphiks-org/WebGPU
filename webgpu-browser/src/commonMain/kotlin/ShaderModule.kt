@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import js.promise.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.unsafeCast

class ShaderModule(val handler: WGPUShaderModule) : GPUShaderModule {
    override var label: String
        get() = handler.label
        set(value) {
            handler.label = value
        }

    override suspend fun getCompilationInfo(): Result<GPUCompilationInfo> = runCatching {
        handler
            .getCompilationInfo()
            .await()
            .unsafeCast<WGPUCompilationInfo>()
            .let { map(it) }
    }

    override fun close() {
        // Nothing to do on JS
    }
}
