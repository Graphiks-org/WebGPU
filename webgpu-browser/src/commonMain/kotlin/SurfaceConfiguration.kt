@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import kotlin.js.ExperimentalWasmJsInterop

/** Color space used by a canvas configuration. */
enum class PredefinedColorSpace(val value: String) {
    srgb("srgb"),
    displayp3("display-p3"),
}

/** Alpha compositing mode used by a canvas configuration. */
enum class GPUCanvasAlphaMode(val value: String) {
    Opaque("opaque"),
    Premultiplied("premultiplied"),
}

/** Parameters used to configure a canvas surface for a device. */
data class SurfaceConfiguration(
    val device: GPUDevice,
    val format: GPUTextureFormat,
    val usage: GPUTextureUsage = GPUTextureUsage.RenderAttachment,
    val viewFormats: Set<GPUTextureFormat> = emptySet(),
    val colorSpace: PredefinedColorSpace = PredefinedColorSpace.srgb,
    val alphaMode: GPUCanvasAlphaMode = GPUCanvasAlphaMode.Opaque,
)

/** A texture acquired from a canvas surface. The browser owns the underlying texture. */
data class SurfaceTexture(val texture: GPUTexture)
