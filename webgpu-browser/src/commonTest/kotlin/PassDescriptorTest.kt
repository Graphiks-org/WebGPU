@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUQuerySet
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.browser.mapper.map
import org.graphiks.webgpu.descriptors.ComputePassDescriptor
import org.graphiks.webgpu.descriptors.ComputePassTimestampWrites
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
}
