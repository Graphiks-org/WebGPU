@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUSamplerDescriptor
import org.graphiks.webgpu.bindings.WGPUSamplerDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUSamplerDescriptor): WGPUSamplerDescriptor = createJsObject<WGPUSamplerDescriptor>().apply {
    label = input.label
    addressModeU = input.addressModeU.value
    addressModeV = input.addressModeV.value
    addressModeW = input.addressModeW.value
    magFilter = input.magFilter.value
    minFilter = input.minFilter.value
    mipmapFilter = input.mipmapFilter.value
    lodMinClamp = input.lodMinClamp.asJsNumber()
    lodMaxClamp = input.lodMaxClamp.asJsNumber()
    input.compare?.let { compare = it.value }
    maxAnisotropy = input.maxAnisotropy.asJsNumber()
}
