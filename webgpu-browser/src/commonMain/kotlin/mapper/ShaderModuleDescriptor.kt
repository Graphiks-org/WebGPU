@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUShaderModuleCompilationHint
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.browser.PipelineLayout
import org.graphiks.webgpu.bindings.WGPUShaderModuleCompilationHint
import org.graphiks.webgpu.bindings.WGPUShaderModuleDescriptor
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

fun map(input: GPUShaderModuleDescriptor): WGPUShaderModuleDescriptor = createJsObject<WGPUShaderModuleDescriptor>().apply {
    code = input.code
    compilationHints = input.compilationHints.mapJsArray { map(it) }
    label = input.label
}

private fun map(input: GPUShaderModuleCompilationHint) =
    createJsObject<WGPUShaderModuleCompilationHint>().apply {
        entryPoint = input.entryPoint
        layout = (input.layout as PipelineLayout?)?.handler ?: "auto".toJsString()
    }
