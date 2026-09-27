package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUTextureOrGPUTextureView
import org.graphiks.webgpu.browser.Texture
import org.graphiks.webgpu.browser.TextureView
import kotlin.js.JsAny

internal fun mapAttachment(input: GPUTextureOrGPUTextureView): JsAny = when (input) {
    is Texture -> input.handler
    is TextureView -> input.handler
    else -> error("The attachment must belong to the browser implementation.")
}
