@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUColor
import org.graphiks.webgpu.bindings.WGPUColor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUColor): WGPUColor = createJsObject<WGPUColor>().apply {
    r = input.r.asJsNumber()
    g = input.g.asJsNumber()
    b = input.b.asJsNumber()
    a = input.a.asJsNumber()
}
