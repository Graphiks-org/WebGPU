@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUQueueDescriptor
import org.graphiks.webgpu.bindings.WGPUDeviceDescriptor
import org.graphiks.webgpu.bindings.WGPUQueueDescriptor
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

// TODO: add unit test
internal fun map(input: GPUDeviceDescriptor) = createJsObject<WGPUDeviceDescriptor>().apply {
    requiredFeatures = input.requiredFeatures.mapJsArray { it.value.toJsString() }
    input.requiredLimits?.let { requiredLimits = map(it) }
    defaultQueue = map(input.defaultQueue)
    label = input.label
}

private fun map(input: GPUQueueDescriptor) = createJsObject<WGPUQueueDescriptor>().apply {
    label = input.label
}
