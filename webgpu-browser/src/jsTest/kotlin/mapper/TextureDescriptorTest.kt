package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TextureDescriptorTest {
    private fun descriptor(dimension: GPUTextureViewDimension? = null) = TextureDescriptor(
        size = Extent3D(16u, 16u, 6u),
        format = GPUTextureFormat.RGBA8Unorm,
        usage = GPUTextureUsage.TextureBinding,
        textureBindingViewDimension = dimension,
    )

    @Test
    fun forwardsCubeBindingViewDimension() {
        assertEquals("cube", map(descriptor(GPUTextureViewDimension.Cube)).textureBindingViewDimension)
    }

    @Test
    fun omitsUnspecifiedBindingViewDimension() {
        val mapped = map(descriptor())
        assertFalse(mapped.asDynamic().hasOwnProperty("textureBindingViewDimension") as Boolean)
    }
}
