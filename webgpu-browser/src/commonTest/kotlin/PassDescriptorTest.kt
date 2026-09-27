@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.bindings.WGPUQuerySet
import org.graphiks.webgpu.bindings.WGPUTexture
import org.graphiks.webgpu.bindings.WGPUTextureView
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.browser.mapper.map
import org.graphiks.webgpu.descriptors.ComputePassDescriptor
import org.graphiks.webgpu.descriptors.ComputePassTimestampWrites
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPassTimestampWrites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PassDescriptorTest {

    @Test
    fun computeTimestampPreservesZeroAndOmitsMissingEnd() {
        val query = QuerySet(createJsObject<WGPUQuerySet>())
        val descriptor = ComputePassDescriptor(
            timestampWrites = ComputePassTimestampWrites(query, beginningOfPassWriteIndex = 0u),
        )
        val out = map(descriptor)
        assertTrue(sameJs(query.handler, out.timestampWrites.querySet))
        assertEquals(0.0, propertyNumber(out.timestampWrites, "beginningOfPassWriteIndex"))
        assertFalse(hasOwn(out.timestampWrites, "endOfPassWriteIndex"))
    }

    @Test
    fun renderPassForwardsQueriesAndTimestampsSeparately() {
        val query = QuerySet(createJsObject<WGPUQuerySet>())
        val descriptor = RenderPassDescriptor(
            colorAttachments = emptyList(),
            occlusionQuerySet = query,
            timestampWrites = RenderPassTimestampWrites(query, endOfPassWriteIndex = 3u),
        )
        val out = map(descriptor)
        assertTrue(sameJs(query.handler, out.occlusionQuerySet))
        assertTrue(sameJs(query.handler, out.timestampWrites.querySet))
        assertFalse(hasOwn(out.timestampWrites, "beginningOfPassWriteIndex"))
        assertEquals(3.0, propertyNumber(out.timestampWrites, "endOfPassWriteIndex"))
    }

    @Test
    fun attachmentUnionsKeepTextureAndViewIdentity() {
        val texture = Texture(createJsObject<WGPUTexture>())
        val view = TextureView(createJsObject<WGPUTextureView>())

        val out = map(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = texture,
                        loadOp = GPULoadOp.Clear,
                        storeOp = GPUStoreOp.Store,
                        resolveTarget = view,
                    ),
                    RenderPassColorAttachment(
                        view = view,
                        loadOp = GPULoadOp.Clear,
                        storeOp = GPUStoreOp.Store,
                        resolveTarget = null,
                    ),
                ),
                depthStencilAttachment = RenderPassDepthStencilAttachment(view = texture),
            ),
        )

        val first = out.colorAttachments[0]
        assertTrue(sameJs(texture.handler, propertyValue(first, "view")))
        assertTrue(sameJs(view.handler, propertyValue(first, "resolveTarget")))

        val second = out.colorAttachments[1]
        assertTrue(sameJs(view.handler, propertyValue(second, "view")))
        assertFalse(hasOwn(second, "resolveTarget"))

        assertTrue(sameJs(texture.handler, propertyValue(out.depthStencilAttachment, "view")))
    }
}
