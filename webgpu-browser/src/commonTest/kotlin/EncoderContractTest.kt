@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.WebArrayBuffer
import org.graphiks.webgpu.bindings.WGPUBindGroup
import org.graphiks.webgpu.bindings.WGPUBuffer
import org.graphiks.webgpu.bindings.WGPUComputePassEncoder
import org.graphiks.webgpu.bindings.WGPUQueue
import org.graphiks.webgpu.bindings.WGPURenderBundleEncoder
import org.graphiks.webgpu.bindings.WGPURenderPassEncoder
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun recorder(): JsAny = js(
    """({
        calls: [],
        setBindGroup(...args) { this.calls.push(args); },
        setVertexBuffer(...args) { this.calls.push(args); },
        setImmediates(...args) { this.calls.push(args); },
        writeBuffer(...args) { this.calls.push(args); }
    })""",
)
private fun argCount(raw: JsAny, call: Int): Int = js("raw.calls[call].length")
private fun argIsNull(raw: JsAny, call: Int, index: Int): Boolean = js("raw.calls[call][index] === null")
private fun argNumber(raw: JsAny, call: Int, index: Int): Double = js("raw.calls[call][index]")
private fun argSame(raw: JsAny, call: Int, index: Int, expected: JsAny): Boolean =
    js("raw.calls[call][index] === expected")
private fun arrayNumber(raw: JsAny, call: Int, index: Int, arrayIndex: Int): Double =
    js("raw.calls[call][index][arrayIndex]")

class EncoderContractTest {

    @Test
    fun setBindGroupForwardsNullIdentityAndDynamicOffsets() {
        val raw = recorder()
        val compute = ComputePassEncoder(raw.unsafeCast<WGPUComputePassEncoder>())
        val render = RenderPassEncoder(raw.unsafeCast<WGPURenderPassEncoder>())
        val bundle = RenderBundleEncoder(raw.unsafeCast<WGPURenderBundleEncoder>())
        val group = BindGroup(createJsObject<WGPUBindGroup>())

        compute.setBindGroup(0u, null)
        compute.setBindGroup(0u, group, listOf(256u, 512u))
        render.setBindGroup(1u, null)
        render.setBindGroup(1u, group)
        bundle.setBindGroup(2u, null)
        bundle.setBindGroup(2u, group)

        assertEquals(2, argCount(raw, 0))
        assertTrue(argIsNull(raw, 0, 1))
        assertTrue(argSame(raw, 1, 1, group.handler))
        assertEquals(256.0, arrayNumber(raw, 1, 2, 0))
        assertEquals(512.0, arrayNumber(raw, 1, 2, 1))
        assertTrue(argIsNull(raw, 2, 1))
        assertTrue(argSame(raw, 3, 1, group.handler))
        assertTrue(argIsNull(raw, 4, 1))
        assertTrue(argSame(raw, 5, 1, group.handler))
    }

    @Test
    fun setVertexBufferForwardsNullOffsetsAndSize() {
        val raw = recorder()
        val render = RenderPassEncoder(raw.unsafeCast<WGPURenderPassEncoder>())
        val bundle = RenderBundleEncoder(raw.unsafeCast<WGPURenderBundleEncoder>())
        val buffer = Buffer(createJsObject<WGPUBuffer>())

        render.setVertexBuffer(1u, null)
        render.setVertexBuffer(1u, buffer, 8uL, 16uL)
        bundle.setVertexBuffer(2u, null)
        bundle.setVertexBuffer(2u, buffer, 8uL, 16uL)

        assertEquals(2, argCount(raw, 0))
        assertTrue(argIsNull(raw, 0, 1))
        assertEquals(4, argCount(raw, 1))
        assertTrue(argSame(raw, 1, 1, buffer.handler))
        assertEquals(8.0, argNumber(raw, 1, 2))
        assertEquals(16.0, argNumber(raw, 1, 3))
        assertEquals(2, argCount(raw, 2))
        assertTrue(argIsNull(raw, 2, 1))
        assertEquals(4, argCount(raw, 3))
        assertEquals(8.0, argNumber(raw, 3, 2))
        assertEquals(16.0, argNumber(raw, 3, 3))
    }

    @Test
    fun setImmediatesForwardsOffsetsAndOmitsMissingSize() {
        val raw = recorder()
        val compute = ComputePassEncoder(raw.unsafeCast<WGPUComputePassEncoder>())
        val render = RenderPassEncoder(raw.unsafeCast<WGPURenderPassEncoder>())
        val bundle = RenderBundleEncoder(raw.unsafeCast<WGPURenderBundleEncoder>())
        val data = ArrayBuffer.of(intArrayOf(1, 2, 3, 4))
        val rawData = (data as WebArrayBuffer).buffer

        compute.setImmediates(4u, data, 8uL, null)
        compute.setImmediates(4u, data, 8uL, 4uL)
        render.setImmediates(4u, data, 8uL, null)
        render.setImmediates(4u, data, 8uL, 4uL)
        bundle.setImmediates(4u, data, 8uL, null)
        bundle.setImmediates(4u, data, 8uL, 4uL)

        for (call in listOf(0, 2, 4)) {
            assertEquals(3, argCount(raw, call))
            assertEquals(4.0, argNumber(raw, call, 0))
            assertTrue(argSame(raw, call, 1, rawData))
            assertEquals(8.0, argNumber(raw, call, 2))
        }
        for (call in listOf(1, 3, 5)) {
            assertEquals(4, argCount(raw, call))
            assertEquals(4.0, argNumber(raw, call, 3))
        }
    }

    @Test
    fun queueWriteBufferForwardsOffsetsAndOmitsMissingSize() {
        val raw = recorder()
        val queue = Queue(raw.unsafeCast<WGPUQueue>())
        val target = Buffer(createJsObject<WGPUBuffer>())
        val data = ArrayBuffer.of(intArrayOf(1, 2, 3, 4))
        val rawData = (data as WebArrayBuffer).buffer

        queue.writeBuffer(target, 8uL, data, 4uL, null)
        queue.writeBuffer(target, 8uL, data, 4uL, 8uL)

        assertEquals(4, argCount(raw, 0))
        assertTrue(argSame(raw, 0, 0, target.handler))
        assertEquals(8.0, argNumber(raw, 0, 1))
        assertTrue(argSame(raw, 0, 2, rawData))
        assertEquals(4.0, argNumber(raw, 0, 3))
        assertEquals(5, argCount(raw, 1))
        assertEquals(8.0, argNumber(raw, 1, 4))
    }
}
