@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUTexelCopyTextureInfo
import org.graphiks.webgpu.browser.Texture
import org.graphiks.webgpu.bindings.WGPUTexelCopyTextureInfo
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUTexelCopyTextureInfo): WGPUTexelCopyTextureInfo = createJsObject<WGPUTexelCopyTextureInfo>().apply {
    texture = (input.texture as Texture).handler
    mipLevel = input.mipLevel.asJsNumber()
    origin = map(input.origin)
    aspect = input.aspect.value
}
