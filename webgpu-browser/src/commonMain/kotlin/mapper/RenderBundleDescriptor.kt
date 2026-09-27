package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPURenderBundleDescriptor
import org.graphiks.webgpu.bindings.WGPURenderBundleDescriptor
import org.graphiks.webgpu.bindings.createJsObject

internal fun map(input: GPURenderBundleDescriptor): WGPURenderBundleDescriptor =
    createJsObject<WGPURenderBundleDescriptor>().apply {
        label = input.label
    }
