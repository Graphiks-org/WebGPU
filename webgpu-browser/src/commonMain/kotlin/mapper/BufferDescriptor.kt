@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.bindings.WGPUBufferDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUBufferDescriptor): WGPUBufferDescriptor = createJsObject<WGPUBufferDescriptor>().apply {
    label = input.label
    size = input.size.asJsNumber()
    usage = input.usage.value.asJsNumber()
    mappedAtCreation = input.mappedAtCreation
}
