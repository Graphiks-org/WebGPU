@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUOrigin3D
import org.graphiks.webgpu.bindings.WGPUOrigin3D
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUOrigin3D): WGPUOrigin3D = createJsObject<WGPUOrigin3D>().apply {
    x = input.x.asJsNumber()
    y = input.y.asJsNumber()
    z = input.z.asJsNumber()
}
