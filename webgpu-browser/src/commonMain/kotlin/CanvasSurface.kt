@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.bindings.WGPUCanvasContext
import org.graphiks.webgpu.bindings.navigator
import org.graphiks.webgpu.browser.mapper.map
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.unsafeCast

/**
 * A configured WebGPU canvas context. The textures it hands out are borrowed from the canvas:
 * closing their wrapper never destroys them.
 */
class CanvasSurface(val handler: WGPUCanvasContext) : AutoCloseable {

    val width: UInt
        get() = handler.canvas.unsafeCast<HTMLCanvasElement>().width.toUInt()

    val height: UInt
        get() = handler.canvas.unsafeCast<HTMLCanvasElement>().height.toUInt()

    val preferredCanvasFormat: GPUTextureFormat?
        get() = navigator.gpu
            ?.getPreferredCanvasFormat()
            ?.let { GPUTextureFormat.of(it) }

    fun getCurrentTexture(): SurfaceTexture =
        SurfaceTexture(Texture.wrapBorrowed(handler.getCurrentTexture()))

    /** Presentation is handled by the browser once the pass is submitted. */
    fun present() {
        // nothing to do on web
    }

    fun configure(configuration: SurfaceConfiguration) {
        handler.configure(map(configuration))
    }

    /**
     * Unconfigures the canvas context. The device passed to [configure] is not owned by this
     * surface and is not closed.
     */
    override fun close() {
        handler.unconfigure()
    }
}
