package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.error
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toInt
import kotlin.toUInt

@OptIn(ExperimentalWasmJsInterop::class)
class Texture(val handler: WGPUTexture, val canBeDestroy: Boolean = true) : GPUTexture {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }
    override val width: GPUIntegerCoordinateOut
        get() = handler.width.toInt().toUInt()
    override val height: GPUIntegerCoordinateOut
        get() = handler.height.toInt().toUInt()
    override val depthOrArrayLayers: GPUIntegerCoordinateOut
        get() = handler.depthOrArrayLayers.toInt().toUInt()
    override val mipLevelCount: GPUIntegerCoordinateOut
        get() = handler.mipLevelCount.toInt().toUInt()
    override val sampleCount: GPUSize32Out
        get() = handler.sampleCount.toInt().toUInt()
    override val dimension: GPUTextureDimension
        get() = GPUTextureDimension.of(handler.dimension) ?: error("unsupported texture dimension ${handler.dimension}")
    override val format: GPUTextureFormat
        get() = GPUTextureFormat.of(handler.format) ?: error("unsupported texture format ${handler.format}")
    override val usage: Set<GPUTextureUsage>
        get() = GPUTextureUsage.entries.filter { (it.value or handler.usage.toULong()) != 0uL }.toSet()

    override fun createView(descriptor: GPUTextureViewDescriptor?): GPUTextureView {
        return TextureView(
            when (descriptor) {
                null -> handler.createView()
                else -> handler.createView(map(descriptor))
            }
        )
    }

    override fun close() {
        // On firefox, canvas textures throw an exception when calling destroy
        if (canBeDestroy) handler.destroy()
    }
}
