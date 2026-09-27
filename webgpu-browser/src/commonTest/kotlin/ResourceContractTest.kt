@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.bindings.WGPUTexture
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals

private fun fakeTexture(usage: Double): WGPUTexture =
    js("({ usage, destroyed: 0, destroy() { this.destroyed++; } })")
private fun destroyed(texture: WGPUTexture): Int = js("texture.destroyed")

class ResourceContractTest {

    @Test
    fun textureUsageContainsOnlySetBits() {
        assertEquals(emptySet(), Texture(fakeTexture(0.0)).usage)
        assertEquals(
            setOf(GPUTextureUsage.CopySrc),
            Texture(fakeTexture(GPUTextureUsage.CopySrc.value.toDouble())).usage,
        )
        val flags = GPUTextureUsage.CopySrc or GPUTextureUsage.RenderAttachment
        assertEquals(
            setOf(GPUTextureUsage.CopySrc, GPUTextureUsage.RenderAttachment),
            Texture(fakeTexture(flags.value.toDouble())).usage,
        )
    }

    @Test
    fun borrowedCanvasTextureIsNotDestroyed() {
        val borrowed = fakeTexture(0.0)
        Texture(borrowed, canBeDestroy = false).close()
        assertEquals(0, destroyed(borrowed))
        val owned = fakeTexture(0.0)
        Texture(owned).close()
        assertEquals(1, destroyed(owned))
    }
}
