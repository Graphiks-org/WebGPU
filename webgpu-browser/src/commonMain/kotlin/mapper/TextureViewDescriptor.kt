@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUTextureViewDescriptor
import org.graphiks.webgpu.bindings.WGPUTextureViewDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUTextureViewDescriptor): WGPUTextureViewDescriptor =
    createJsObject<WGPUTextureViewDescriptor>().apply {
        label = input.label
        input.format?.let { format = it.value }
        input.dimension?.let { dimension = it.value }
        aspect = input.aspect.value
        baseMipLevel = input.baseMipLevel.asJsNumber()
        input.mipLevelCount?.let { mipLevelCount = it.asJsNumber() }
        baseArrayLayer = input.baseArrayLayer.asJsNumber()
        input.arrayLayerCount?.let { arrayLayerCount = it.asJsNumber() }
        usage = input.usage.value.asJsNumber()
        swizzle = input.swizzle.toWebGpuString()
    }
