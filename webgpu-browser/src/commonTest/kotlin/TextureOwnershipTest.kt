@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.bindings.WGPUCanvasContext
import org.graphiks.webgpu.bindings.WGPUDevice
import org.graphiks.webgpu.bindings.WGPUTexture
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals

private external interface OwnedTexture : WGPUTexture {
    var destroyCount: Double
}

private fun ownedTexture(): OwnedTexture =
    js("({ destroyCount: 0, destroy: function() { this.destroyCount++; } })")

private external interface FakeCanvasContext : WGPUCanvasContext {
    var unconfigureCount: Double
    var acquireCount: Double
    var currentTexture: WGPUTexture
}

private fun fakeCanvasContext(texture: WGPUTexture): FakeCanvasContext =
    js(
        "({ unconfigureCount: 0, acquireCount: 0, currentTexture: texture, " +
            "unconfigure: function() { this.unconfigureCount++; }, " +
            "getCurrentTexture: function() { this.acquireCount++; return this.currentTexture; } })",
    )

private fun fakeDeviceWithTexture(texture: WGPUTexture): WGPUDevice =
    js("({ createTexture: function() { return texture; } })")

class TextureOwnershipTest {

    @Test
    fun ownedWrapperDestroysTheHandleExactlyOnce() {
        val raw = ownedTexture()
        val texture = Texture.wrapOwned(raw)
        texture.close()
        texture.close()
        assertEquals(1.0, raw.destroyCount, "A repeated close must not destroy the same handle twice")
    }

    @Test
    fun borrowedWrapperNeverDestroysTheHandle() {
        val raw = ownedTexture()
        val texture = Texture.wrapBorrowed(raw)
        texture.close()
        texture.close()
        assertEquals(0.0, raw.destroyCount, "A borrowed texture must not be destroyed by its wrapper")
    }

    @Test
    fun canvasTextureIsBorrowedAndTheNextFrameIsStillAcquirable() {
        val raw = ownedTexture()
        val context = fakeCanvasContext(raw)
        val surface = CanvasSurface(context)

        val frame = surface.getCurrentTexture()
        frame.texture.close()
        assertEquals(0.0, raw.destroyCount, "Closing a canvas texture wrapper must not destroy the canvas texture")

        // Closing the wrapper must not prevent acquiring and rendering the next frame.
        val next = surface.getCurrentTexture()
        assertEquals(2.0, context.acquireCount)
        next.texture.close()
        assertEquals(0.0, raw.destroyCount)
    }

    @Test
    fun canvasSurfaceCloseUnconfiguresAndUseClosesIt() {
        val context = fakeCanvasContext(ownedTexture())
        CanvasSurface(context).use { surface ->
            assertEquals(0.0, context.unconfigureCount)
            surface.getCurrentTexture()
        }
        assertEquals(1.0, context.unconfigureCount, "close() must unconfigure the canvas context")
    }

    @Test
    fun deviceCreatedTextureIsOwned() {
        val raw = ownedTexture()
        val device = Device(fakeDeviceWithTexture(raw))
        val texture = device.createTexture(
            TextureDescriptor(
                size = Extent3D(1u, 1u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.RenderAttachment,
            ),
        )
        texture.close()
        assertEquals(1.0, raw.destroyCount, "A device-created texture is owned by its wrapper")
    }
}
