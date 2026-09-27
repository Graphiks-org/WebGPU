@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.bindings.WGPUCanvasConfiguration
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import org.graphiks.webgpu.browser.Device
import org.graphiks.webgpu.browser.SurfaceConfiguration
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

internal fun map(input: SurfaceConfiguration): WGPUCanvasConfiguration =
    createJsObject<WGPUCanvasConfiguration>().apply {
        device = (input.device as Device).handler
        format = input.format.value
        usage = input.usage.value.asJsNumber()
        viewFormats = input.viewFormats.mapJsArray { it.value.toJsString() }
        colorSpace = input.colorSpace.value.toJsString()
        alphaMode = input.alphaMode.value.toJsString()
    }
