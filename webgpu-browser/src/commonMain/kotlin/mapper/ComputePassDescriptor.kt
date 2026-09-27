package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUComputePassDescriptor
import org.graphiks.webgpu.bindings.WGPUComputePassDescriptor
import org.graphiks.webgpu.bindings.createJsObject

internal fun map(input: GPUComputePassDescriptor): WGPUComputePassDescriptor =
    createJsObject<WGPUComputePassDescriptor>().apply {
        label = input.label
        // TODO: timestampWrites mapping
    }
