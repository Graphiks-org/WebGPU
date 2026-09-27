@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPUProgrammableStage
import org.graphiks.webgpu.browser.PipelineLayout
import org.graphiks.webgpu.browser.ShaderModule
import org.graphiks.webgpu.bindings.WGPUComputePipelineDescriptor
import org.graphiks.webgpu.bindings.WGPUProgrammableStage
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

internal fun map(input: GPUComputePipelineDescriptor): WGPUComputePipelineDescriptor =
    createJsObject<WGPUComputePipelineDescriptor>().apply {
        label = input.label
        compute = map(input.compute)
        layout = (input.layout as PipelineLayout?)?.handler ?: "auto".toJsString()
    }

private fun map(input: GPUProgrammableStage): WGPUProgrammableStage =
    createJsObject<WGPUProgrammableStage>().apply {
        module = (input.module as ShaderModule).handler
        input.entryPoint?.let { entryPoint = it }
        constants = mapConstants(input.constants)
    }
