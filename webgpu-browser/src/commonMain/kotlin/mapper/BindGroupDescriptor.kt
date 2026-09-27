@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.browser.BindGroupLayout
import org.graphiks.webgpu.browser.Buffer
import org.graphiks.webgpu.GPUBindGroupDescriptor
import org.graphiks.webgpu.GPUBindGroupEntry
import org.graphiks.webgpu.GPUBufferBinding
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUSampler
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.browser.Sampler
import org.graphiks.webgpu.browser.Texture
import org.graphiks.webgpu.browser.TextureView
import org.graphiks.webgpu.bindings.WGPUBindGroupDescriptor
import org.graphiks.webgpu.bindings.WGPUBindGroupEntry
import org.graphiks.webgpu.bindings.WGPUBufferBinding
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUBindGroupDescriptor): WGPUBindGroupDescriptor = createJsObject<WGPUBindGroupDescriptor>().apply {
    label = input.label
    layout = (input.layout as BindGroupLayout).handler
    entries = input.entries.mapJsArray {
        val entry: WGPUBindGroupEntry = map(it)
        entry
    }
}

private fun map(input: GPUBindGroupEntry): WGPUBindGroupEntry =
    createJsObject<WGPUBindGroupEntry>().apply {
        binding = input.binding.asJsNumber()
        resource = when (val localResource = input.resource) {
            is GPUSampler -> (localResource as Sampler).handler
            is GPUBuffer -> (localResource as Buffer).handler
            is GPUTexture -> (localResource as Texture).handler
            is GPUBufferBinding -> createJsObject<WGPUBufferBinding>().apply {
                buffer = (localResource.buffer as Buffer).handler
                offset = localResource.offset.asJsNumber()
                localResource.size?.let { size = it.asJsNumber() }
            }

            is GPUTextureView -> (localResource as TextureView).handler
        }
    }
