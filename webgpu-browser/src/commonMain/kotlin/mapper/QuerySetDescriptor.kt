@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.bindings.WGPUQuerySetDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUQuerySetDescriptor): WGPUQuerySetDescriptor = createJsObject<WGPUQuerySetDescriptor>().apply {
    label = input.label
    type = input.type.value
    count = input.count.asJsNumber()
}
