@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUBindGroupLayoutDescriptor
import org.graphiks.webgpu.GPUBindGroupLayoutEntry
import org.graphiks.webgpu.bindings.WGPUBindGroupLayoutDescriptor
import org.graphiks.webgpu.bindings.WGPUBindGroupLayoutEntry
import org.graphiks.webgpu.bindings.WGPUBufferBindingLayout
import org.graphiks.webgpu.bindings.WGPUSamplerBindingLayout
import org.graphiks.webgpu.bindings.WGPUStorageTextureBindingLayout
import org.graphiks.webgpu.bindings.WGPUTextureBindingLayout
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop

// TODO: add unit test
internal fun map(input: GPUBindGroupLayoutDescriptor): WGPUBindGroupLayoutDescriptor =
    createJsObject<WGPUBindGroupLayoutDescriptor>().apply {
        label = input.label
        entries = input.entries.mapJsArray {
            val entry: WGPUBindGroupLayoutEntry = map(it)
            entry
        }
    }

private fun map(input: GPUBindGroupLayoutEntry): WGPUBindGroupLayoutEntry =
    createJsObject<WGPUBindGroupLayoutEntry>().apply {
        binding = input.binding.asJsNumber()
        visibility = input.visibility.value.asJsNumber()
        input.buffer?.let { input ->
            buffer = createJsObject<WGPUBufferBindingLayout>().apply {
                type = input.type.value
                hasDynamicOffset = input.hasDynamicOffset
                minBindingSize = input.minBindingSize.asJsNumber()
            }
        }
        input.sampler?.let { input ->
            sampler = createJsObject<WGPUSamplerBindingLayout>().apply {
                type = input.type.value
            }
        }
        input.texture?.let { input ->
            texture = createJsObject<WGPUTextureBindingLayout>().apply {
                sampleType = input.sampleType.value
                viewDimension = input.viewDimension.value
                multisampled = input.multisampled
            }
        }
        input.storageTexture?.let { input ->
            storageTexture = createJsObject<WGPUStorageTextureBindingLayout>().apply {
                access = input.access.value
                format = input.format.value
                viewDimension = input.viewDimension.value
            }
        }
    }
