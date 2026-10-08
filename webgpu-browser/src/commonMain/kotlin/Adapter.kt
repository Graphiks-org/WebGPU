@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString
import kotlin.js.unsafeCast


suspend fun requestAdapter(options: GPURequestAdapterOptions? = null): Result<Adapter> =
    requestAdapter(browserGpu(), options)

private fun browserGpu(): GPU? = js("globalThis.navigator && globalThis.navigator.gpu")

internal suspend fun requestAdapter(gpu: GPU?, options: GPURequestAdapterOptions?): Result<Adapter> = browserResult {
    checkNotNull(gpu) { "WebGPU is not available in this environment." }

    val raw = when (options) {
        null -> gpu.requestAdapter()
        else -> gpu.requestAdapter(map(options))
    }.await()
    Adapter(checkNotNull(raw) { "No WebGPU adapter is available." }.unsafeCast<WGPUAdapter>())
}

class Adapter(val handler: WGPUAdapter) : GPUAdapter {

    override val features: Set<GPUFeatureName> by lazy {
        GPUFeatureName.entries
            .filter { handler.features.has(it.value.toJsString()) }
            .toSet()
    }

    override val limits: GPUSupportedLimits by lazy { map(handler.limits) }

    override val info: GPUAdapterInfo
        get() = map(handler.info)

    override suspend fun requestDevice(descriptor: GPUDeviceDescriptor?): Result<GPUDevice> {
        return browserResult {
            when (descriptor) {
                null -> handler.requestDevice()
                else -> handler.requestDevice(map(descriptor))
            }.await { raw ->
                // The device was created but never delivered to the caller: destroy it exactly once.
                raw.unsafeCast<WGPUDevice>().destroy()
            }
                .unsafeCast<WGPUDevice>()
                .let { Device(it, descriptor?.onUncapturedError)}
        }
    }

    override fun close() {
        // Nothing to do on JS
    }
}


private fun map(input: GPURequestAdapterOptions) = createJsObject<WGPURequestAdapterOptions>().apply {
    featureLevel = input.featureLevel
    input.powerPreference?.let { powerPreference = it.value }
    forceFallbackAdapter = input.forceFallbackAdapter
    xrCompatible = input.xrCompatible
}
