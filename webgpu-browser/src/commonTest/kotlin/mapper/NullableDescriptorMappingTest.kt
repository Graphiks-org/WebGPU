@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUBlendState
import org.graphiks.webgpu.GPUColor
import org.graphiks.webgpu.GPUColorTargetState
import org.graphiks.webgpu.GPUColorWrite
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureOrGPUTextureView
import org.graphiks.webgpu.GPURenderPassColorAttachment
import org.graphiks.webgpu.GPUVertexAttribute
import org.graphiks.webgpu.GPUVertexBufferLayout
import org.graphiks.webgpu.GPUVertexStepMode
import org.graphiks.webgpu.bindings.WGPUBindGroupLayout
import org.graphiks.webgpu.bindings.WGPUColorTargetState
import org.graphiks.webgpu.bindings.WGPURenderPassColorAttachment
import org.graphiks.webgpu.bindings.WGPUShaderModule
import org.graphiks.webgpu.bindings.WGPUTexture
import org.graphiks.webgpu.bindings.WGPUVertexBufferLayout
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.browser.BindGroupLayout
import org.graphiks.webgpu.browser.ShaderModule
import org.graphiks.webgpu.browser.Texture
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.get
import kotlin.js.length
import kotlin.js.toInt
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class NullableDescriptorMappingTest {

    private fun fakeBindGroupLayout() = BindGroupLayout(createJsObject<WGPUBindGroupLayout>())

    private fun fakeTexture() = Texture.wrapBorrowed(createJsObject<WGPUTexture>())

    private fun fakeShaderModule(): GPUShaderModule = ShaderModule(createJsObject<WGPUShaderModule>())

    private fun fakeColorTarget(): GPUColorTargetState = object : GPUColorTargetState {
        override val format: GPUTextureFormat = GPUTextureFormat.RGBA8Unorm
        override val blend: GPUBlendState? = null
        override val writeMask: GPUColorWrite = GPUColorWrite.All
    }

    private fun fakeVertexBufferLayout(): GPUVertexBufferLayout = object : GPUVertexBufferLayout {
        override val arrayStride: ULong = 16uL
        override val stepMode: GPUVertexStepMode = GPUVertexStepMode.Vertex
        override val attributes: List<GPUVertexAttribute> = emptyList()
    }

    private fun fakeColorAttachment(view: GPUTextureOrGPUTextureView): GPURenderPassColorAttachment =
        object : GPURenderPassColorAttachment {
            override val view: GPUTextureOrGPUTextureView = view
            override val depthSlice: UInt? = null
            override val resolveTarget: GPUTextureOrGPUTextureView? = null
            override val clearValue: GPUColor? = null
            override val loadOp: GPULoadOp = GPULoadOp.Clear
            override val storeOp: GPUStoreOp = GPUStoreOp.Store
        }

    @Test
    fun pipelineLayoutDescriptor_preserves_null_slots_and_indices() {
        val layout = fakeBindGroupLayout()
        val mapped = map(
            PipelineLayoutDescriptor(
                bindGroupLayouts = listOf(null, layout, null),
            ),
        )
        val layouts = mapped.bindGroupLayouts
        assertEquals(3, layouts.length)
        assertNull(layouts[0])
        assertSame(layout.handler, layouts[1])
        assertNull(layouts[2])
    }

    @Test
    fun pipelineLayoutDescriptor_empty_list_stays_empty() {
        val mapped = map(PipelineLayoutDescriptor(bindGroupLayouts = emptyList()))
        assertEquals(0, mapped.bindGroupLayouts.length)
    }

    @Test
    fun fragmentState_preserves_null_target_slots() {
        val target = fakeColorTarget()
        val mapped = map(
            FragmentState(
                module = fakeShaderModule(),
                targets = listOf(target, null, target),
            ),
        )
        val targets = mapped.targets
        assertEquals(3, targets.length)
        assertEquals(
            target.format.value,
            targets[0]!!.unsafeCast<WGPUColorTargetState>().format,
        )
        assertNull(targets[1])
        assertEquals(
            target.format.value,
            targets[2]!!.unsafeCast<WGPUColorTargetState>().format,
        )
    }

    @Test
    fun vertexState_preserves_null_buffer_slots() {
        val buffer = fakeVertexBufferLayout()
        val mapped = map(
            VertexState(
                module = fakeShaderModule(),
                buffers = listOf(null, buffer),
            ),
        )
        val buffers = mapped.buffers
        assertEquals(2, buffers.length)
        assertNull(buffers[0])
        assertEquals(
            buffer.arrayStride.toInt(),
            buffers[1]!!.unsafeCast<WGPUVertexBufferLayout>().arrayStride.toInt(),
        )
    }

    @Test
    fun renderPassDescriptor_preserves_null_color_attachment_slots() {
        val texture = fakeTexture()
        val mapped = map(
            RenderPassDescriptor(
                colorAttachments = listOf(null, fakeColorAttachment(texture), null),
            ),
        )
        val colorAttachments = mapped.colorAttachments
        assertEquals(3, colorAttachments.length)
        assertNull(colorAttachments[0])
        assertSame(
            texture.handler,
            colorAttachments[1]!!.unsafeCast<WGPURenderPassColorAttachment>().view,
        )
        assertNull(colorAttachments[2])
    }

    @Test
    fun renderBundleEncoderDescriptor_preserves_null_color_format_slots() {
        val mapped = map(
            RenderBundleEncoderDescriptor(
                colorFormats = listOf(GPUTextureFormat.RGBA8Unorm, null, GPUTextureFormat.BGRA8Unorm),
            ),
        )
        val colorFormats = mapped.colorFormats
        assertEquals(3, colorFormats.length)
        assertEquals(GPUTextureFormat.RGBA8Unorm.value, colorFormats[0].toString())
        assertNull(colorFormats[1])
        assertEquals(GPUTextureFormat.BGRA8Unorm.value, colorFormats[2].toString())
    }
}
