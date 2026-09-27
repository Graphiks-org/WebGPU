package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUCommandEncoderDescriptor
import org.graphiks.webgpu.bindings.WGPUCommandEncoderDescriptor
import org.graphiks.webgpu.bindings.createJsObject

internal fun map(input: GPUCommandEncoderDescriptor): WGPUCommandEncoderDescriptor =
    createJsObject<WGPUCommandEncoderDescriptor>().apply {
        label = input.label
    }
