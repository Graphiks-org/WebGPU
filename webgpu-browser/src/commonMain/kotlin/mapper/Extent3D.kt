@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUExtent3D
import org.graphiks.webgpu.bindings.WGPUExtent3D
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUExtent3D): WGPUExtent3D = createJsObject<WGPUExtent3D>().apply {
    width = input.width.asJsNumber()
    height = input.height.asJsNumber()
    depthOrArrayLayers = input.depthOrArrayLayers.asJsNumber()
}
