@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUShaderModuleCompilationHint
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.bindings.WGPUShaderModuleCompilationHint
import org.graphiks.webgpu.bindings.WGPUShaderModuleDescriptor
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop

fun map(input: GPUShaderModuleDescriptor): WGPUShaderModuleDescriptor = createJsObject<WGPUShaderModuleDescriptor>().apply {
    code = input.code
    compilationHints = input.compilationHints.mapJsArray { map(it) }
    label = input.label
}

private fun map(input: GPUShaderModuleCompilationHint) =
    createJsObject<WGPUShaderModuleCompilationHint>().apply {
        entryPoint = input.entryPoint
        layout = TODO("no yet implemented")//input.layout ?: undefined
    }
