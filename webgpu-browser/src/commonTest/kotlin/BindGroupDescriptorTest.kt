@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUBindGroupDescriptor
import org.graphiks.webgpu.bindings.WGPUBindGroupLayout
import org.graphiks.webgpu.bindings.WGPUBuffer
import org.graphiks.webgpu.bindings.WGPUSampler
import org.graphiks.webgpu.bindings.WGPUTexture
import org.graphiks.webgpu.bindings.WGPUTextureView
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.browser.mapper.map
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun resourceAt(descriptor: WGPUBindGroupDescriptor, index: Int): JsAny =
    js("descriptor.entries[index].resource")

class BindGroupDescriptorTest {

    @Test
    fun directBindingResourcesKeepTheirNativeIdentity() {
        val layout = BindGroupLayout(createJsObject<WGPUBindGroupLayout>())
        val buffer = Buffer(createJsObject<WGPUBuffer>())
        val texture = Texture(createJsObject<WGPUTexture>())
        val view = TextureView(createJsObject<WGPUTextureView>())
        val sampler = Sampler(createJsObject<WGPUSampler>())
        val out = map(
            BindGroupDescriptor(
                layout = layout,
                entries = listOf(
                    BindGroupEntry(0u, buffer),
                    BindGroupEntry(1u, texture),
                    BindGroupEntry(2u, view),
                    BindGroupEntry(3u, sampler),
                ),
            ),
        )
        assertTrue(sameJs(buffer.handler, resourceAt(out, 0)))
        assertTrue(sameJs(texture.handler, resourceAt(out, 1)))
        assertTrue(sameJs(view.handler, resourceAt(out, 2)))
        assertTrue(sameJs(sampler.handler, resourceAt(out, 3)))
    }

    @Test
    fun bufferBindingDictionaryForwardsOffsetAndSize() {
        val layout = BindGroupLayout(createJsObject<WGPUBindGroupLayout>())
        val buffer = Buffer(createJsObject<WGPUBuffer>())
        val out = map(
            BindGroupDescriptor(
                layout = layout,
                entries = listOf(
                    BindGroupEntry(0u, BufferBinding(buffer, offset = 8uL, size = 16uL)),
                ),
            ),
        )
        val resource = resourceAt(out, 0)
        assertTrue(sameJs(buffer.handler, propertyValue(resource, "buffer")))
        assertEquals(8.0, propertyNumber(resource, "offset"))
        assertEquals(16.0, propertyNumber(resource, "size"))
    }
}
