@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPURenderBundleEncoderDescriptor
import org.graphiks.webgpu.bindings.WGPURenderBundleEncoderDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

internal fun map(input: GPURenderBundleEncoderDescriptor): WGPURenderBundleEncoderDescriptor =
    createJsObject<WGPURenderBundleEncoderDescriptor>().apply {
        label = input.label
        depthReadOnly = input.depthReadOnly
        stencilReadOnly = input.stencilReadOnly
        colorFormats = input.colorFormats.mapJsArray { format -> format?.value?.toJsString() }
        input.depthStencilFormat?.let { depthStencilFormat = it.value }
        sampleCount = input.sampleCount.asJsNumber()
    }
