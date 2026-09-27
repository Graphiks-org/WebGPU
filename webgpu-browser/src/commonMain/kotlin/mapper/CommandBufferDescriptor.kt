package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUCommandBufferDescriptor
import org.graphiks.webgpu.bindings.WGPUCommandBufferDescriptor
import org.graphiks.webgpu.bindings.createJsObject

internal fun map(input: GPUCommandBufferDescriptor): WGPUCommandBufferDescriptor =
    createJsObject<WGPUCommandBufferDescriptor>()
        .also { it.label = input.label }
