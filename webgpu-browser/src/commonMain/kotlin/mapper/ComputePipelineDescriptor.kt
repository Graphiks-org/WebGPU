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

internal fun map(input: GPUComputePipelineDescriptor): WGPUComputePipelineDescriptor =
    createJsObject<WGPUComputePipelineDescriptor>().apply {
        label = input.label
        compute = map(input.compute)
        layout = (input.layout as PipelineLayout).handler
    }

private fun map(input: GPUProgrammableStage): WGPUProgrammableStage =
    createJsObject<WGPUProgrammableStage>().apply {
        module = (input.module as ShaderModule).handler
        input.entryPoint?.let { entryPoint = it }
        //TODO map constants
    }
