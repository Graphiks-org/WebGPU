@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUBuffer
import org.graphiks.webgpu.bindings.WGPUDevice
import org.graphiks.webgpu.bindings.WGPUQuerySet
import org.graphiks.webgpu.bindings.WGPUTexture
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals

private external interface CloseCountedBuffer : WGPUBuffer {
    var destroyCount: Double
}

private external interface CloseCountedQuerySet : WGPUQuerySet {
    var destroyCount: Double
}

private external interface CloseCountedTexture : WGPUTexture {
    var destroyCount: Double
}

private external interface CloseCountedDevice : WGPUDevice {
    var destroyCount: Double
}

private fun closeCountedBuffer(): CloseCountedBuffer =
    js("({ destroyCount: 0, destroy: function() { this.destroyCount++; } })")

private fun closeCountedQuerySet(): CloseCountedQuerySet =
    js("({ destroyCount: 0, destroy: function() { this.destroyCount++; } })")

private fun closeCountedTexture(): CloseCountedTexture =
    js("({ destroyCount: 0, destroy: function() { this.destroyCount++; } })")

private fun closeCountedDevice(): CloseCountedDevice =
    js("({ destroyCount: 0, destroy: function() { this.destroyCount++; } })")

class ResourceCloseTest {

    @Test
    fun closingABufferTwiceDestroysItOnce() {
        val raw = closeCountedBuffer()
        val buffer = Buffer(raw)
        buffer.close()
        buffer.close()
        assertEquals(1.0, raw.destroyCount, "A repeated close must not release the same reference twice")
    }

    @Test
    fun closingAQuerySetTwiceDestroysItOnce() {
        val raw = closeCountedQuerySet()
        val querySet = QuerySet(raw)
        querySet.close()
        querySet.close()
        assertEquals(1.0, raw.destroyCount)
    }

    @Test
    fun closingAnOwnedTextureTwiceDestroysItOnce() {
        val raw = closeCountedTexture()
        val texture = Texture.wrapOwned(raw)
        texture.close()
        texture.close()
        assertEquals(1.0, raw.destroyCount)
    }

    @Test
    fun closingABorrowedTextureNeverDestroysIt() {
        val raw = closeCountedTexture()
        val texture = Texture.wrapBorrowed(raw)
        texture.close()
        texture.close()
        assertEquals(0.0, raw.destroyCount, "A borrowed canvas texture must not be destroyed by its wrapper")
    }

    @Test
    fun closingADeviceTwiceDestroysItOnce() {
        val raw = closeCountedDevice()
        val device = Device(raw)
        device.close()
        device.close()
        assertEquals(1.0, raw.destroyCount)
    }
}
