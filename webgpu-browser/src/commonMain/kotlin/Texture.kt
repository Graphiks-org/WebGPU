package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.OptIn
import kotlin.String
import kotlin.error
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toInt
import kotlin.toUInt

/**
 * A WebGPU texture wrapper with an explicit ownership contract.
 *
 * Use [wrapOwned] for a texture this wrapper is responsible for destroying, and [wrapBorrowed]
 * for a texture owned by someone else (a canvas context, an imported handle): closing a borrowed
 * wrapper never destroys the underlying texture.
 */
@OptIn(ExperimentalWasmJsInterop::class)
class Texture private constructor(
    /** Interop escape hatch: the raw WebGPU handle. Calling operations on it directly can
     *  invalidate the wrapper's contract (ownership and close-once semantics). */
    val handler: WGPUTexture,
    private val ownership: Ownership,
) : GPUTexture {

    private enum class Ownership { Owned, Borrowed }

    @Deprecated(
        "Ownership is now explicit: use Texture.wrapOwned(handler) for a texture this wrapper " +
            "must destroy, or Texture.wrapBorrowed(handler) for a texture owned by someone else " +
            "(a canvas context, an imported handle). The old canBeDestroy flag maps to " +
            "wrapOwned when true and wrapBorrowed when false.",
    )
    constructor(handler: WGPUTexture, canBeDestroy: Boolean = true) : this(
        handler,
        if (canBeDestroy) Ownership.Owned else Ownership.Borrowed,
    )

    @Deprecated("Ownership is now explicit: use Texture.wrapOwned or Texture.wrapBorrowed.")
    val canBeDestroy: Boolean
        get() = ownership == Ownership.Owned

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
    override val usage: GPUTextureUsage
        get() = GPUTextureUsage.fromBits(handler.usage.toULong())

    override fun createView(descriptor: GPUTextureViewDescriptor?): GPUTextureView {
        return TextureView(
            when (descriptor) {
                null -> handler.createView()
                else -> handler.createView(map(descriptor))
            }
        )
    }

    private var closed = false

    override fun close() {
        // A repeated close must not release the same owned reference twice.
        if (closed) return
        closed = true
        // On firefox, canvas textures throw an exception when calling destroy
        if (ownership == Ownership.Owned) handler.destroy()
    }

    companion object {
        /**
         * Wraps a texture whose destruction is transferred to the wrapper: [Texture.close]
         * destroys the handle. No new texture is created.
         */
        fun wrapOwned(handler: WGPUTexture): Texture = Texture(handler, Ownership.Owned)

        /**
         * Wraps a texture owned by someone else (for example the canvas of a
         * [CanvasSurface]): [Texture.close] only releases the wrapper, never the handle. The
         * caller must not rely on the wrapper to keep the source resource alive.
         */
        fun wrapBorrowed(handler: WGPUTexture): Texture = Texture(handler, Ownership.Borrowed)
    }
}
