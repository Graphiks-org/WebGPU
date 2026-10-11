@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.browser.BindGroupLayout
import org.graphiks.webgpu.GPUPipelineLayoutDescriptor
import org.graphiks.webgpu.bindings.WGPUPipelineLayoutDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUPipelineLayoutDescriptor): WGPUPipelineLayoutDescriptor =
    createJsObject<WGPUPipelineLayoutDescriptor>().apply {
        label = input.label
        bindGroupLayouts = input.bindGroupLayouts
            .mapJsArray { layout -> (layout as BindGroupLayout?)?.handler }
        immediateSize = input.immediateSize.asJsNumber()
    }
