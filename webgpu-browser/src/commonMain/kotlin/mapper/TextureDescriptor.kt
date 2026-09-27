@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUTextureDescriptor
import org.graphiks.webgpu.bindings.WGPUTextureDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

internal fun map(input: GPUTextureDescriptor): WGPUTextureDescriptor = createJsObject<WGPUTextureDescriptor>().apply {
    label = input.label
    size = map(input.size)
    mipLevelCount = input.mipLevelCount.asJsNumber()
    sampleCount = input.sampleCount.asJsNumber()
    dimension = input.dimension.value
    format = input.format.value
    usage = input.usage.value.asJsNumber()
    viewFormats = input.viewFormats.mapJsArray { it.value.toJsString() }
}
