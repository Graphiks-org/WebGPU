@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.bindings.WGPUCanvasContext
import org.graphiks.webgpu.bindings.WGPUDevice
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun emptyDevice(): WGPUDevice = js("({})")

private fun canvasHandler(): JsAny = js(
    """({
        canvas: { width: 2, height: 3 },
        configured: null,
        texture: {},
        unconfigured: 0,
        configure(value) { this.configured = value; },
        getCurrentTexture() { return this.texture; },
        unconfigure() { this.unconfigured++; }
    })""",
)

private fun canvasWithoutContextRaw(): JsAny = js("({ width: 1, height: 1, getContext: () => null })")

private fun canvasWithoutContext(): HTMLCanvasElement =
    canvasWithoutContextRaw().unsafeCast<HTMLCanvasElement>()

class CanvasSurfaceTest {

    @Test
    fun dimensionsConfigurationAndBorrowedTexture() {
        val raw = canvasHandler()
        val surface = CanvasSurface(raw.unsafeCast<WGPUCanvasContext>())
        assertEquals(2u, surface.width)
        assertEquals(3u, surface.height)

        val device = Device(emptyDevice())
        val config = SurfaceConfiguration(
            device = device,
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            alphaMode = GPUCanvasAlphaMode.Premultiplied,
        )
        surface.configure(config)

        val configured = propertyValue(raw, "configured")
        assertTrue(sameJs(device.handler, propertyValue(configured, "device")))
        assertEquals("rgba8unorm", propertyString(configured, "format"))
        assertEquals(17.0, propertyNumber(configured, "usage"))
        assertEquals("premultiplied", propertyString(configured, "alphaMode"))
        assertEquals("srgb", propertyString(configured, "colorSpace"))

        val texture = surface.getCurrentTexture().texture as Texture
        assertTrue(sameJs(propertyValue(raw, "texture"), texture.handler))
        assertFalse(texture.canBeDestroy)

        surface.close()
        assertEquals(1.0, propertyNumber(raw, "unconfigured"))
    }

    @Test
    fun canvasWithoutWebGpuContextFailsExplicitly() {
        val error = assertFailsWith<IllegalStateException> { canvasWithoutContext().getCanvasSurface() }
        assertTrue(error.message.orEmpty().contains("WebGPU"))
    }
}
