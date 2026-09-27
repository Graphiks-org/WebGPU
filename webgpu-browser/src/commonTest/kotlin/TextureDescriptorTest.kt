@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUTextureSwizzle
import org.graphiks.webgpu.GPUTextureSwizzleSource
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.browser.mapper.map
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals

class TextureDescriptorTest {

    @Test
    fun textureViewForwardsUsageAndSwizzle() {
        val out = map(
            TextureViewDescriptor(
                usage = GPUTextureUsage.TextureBinding,
                swizzle = GPUTextureSwizzle(
                    GPUTextureSwizzleSource.Blue,
                    GPUTextureSwizzleSource.Zero,
                    GPUTextureSwizzleSource.One,
                    GPUTextureSwizzleSource.Red,
                ),
            ),
        )
        assertEquals("b01r", out.swizzle)
        assertEquals(GPUTextureUsage.TextureBinding.value.toDouble(), propertyNumber(out, "usage"))
    }

    @Test
    fun pipelineLayoutForwardsImmediateSize() {
        val out = map(PipelineLayoutDescriptor(bindGroupLayouts = emptyList(), immediateSize = 16u))
        assertEquals(16.0, propertyNumber(out, "immediateSize"))
    }
}
