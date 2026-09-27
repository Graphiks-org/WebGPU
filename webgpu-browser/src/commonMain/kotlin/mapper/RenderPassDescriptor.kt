@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPURenderPassColorAttachment
import org.graphiks.webgpu.GPURenderPassDepthStencilAttachment
import org.graphiks.webgpu.GPURenderPassDescriptor
import org.graphiks.webgpu.browser.TextureView
import org.graphiks.webgpu.bindings.WGPURenderPassColorAttachment
import org.graphiks.webgpu.bindings.WGPURenderPassDepthStencilAttachment
import org.graphiks.webgpu.bindings.WGPURenderPassDescriptor
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPURenderPassDescriptor): WGPURenderPassDescriptor =
    createJsObject<WGPURenderPassDescriptor>().apply {
        label = input.label
        colorAttachments = input.colorAttachments.mapJsArray { map(it) }
        input.depthStencilAttachment?.let { depthStencilAttachment = map(it) }
        // TODO map this occlusionQuerySet
        // TODO map this timestampWrites
        maxDrawCount = input.maxDrawCount.asJsNumber()
    }

private fun map(input: GPURenderPassDepthStencilAttachment): WGPURenderPassDepthStencilAttachment =
    createJsObject<WGPURenderPassDepthStencilAttachment>().apply {
        view = (input.view as TextureView).handler
        input.depthClearValue?.let { depthClearValue = it.asJsNumber() }
        input.depthLoadOp?.let { depthLoadOp = it.value }
        input.depthStoreOp?.let { depthStoreOp = it.value }
        depthReadOnly = input.depthReadOnly
        stencilClearValue = input.stencilClearValue.asJsNumber()
        input.stencilLoadOp?.let { stencilLoadOp = it.value }
        input.stencilStoreOp?.let { stencilStoreOp = it.value }
        stencilReadOnly = input.stencilReadOnly
    }

private fun map(input: GPURenderPassColorAttachment): WGPURenderPassColorAttachment =
    createJsObject<WGPURenderPassColorAttachment>().apply {
        view = (input.view as TextureView).handler
        loadOp = input.loadOp.value
        storeOp = input.storeOp.value
        input.depthSlice?.let { depthSlice = it.asJsNumber() }
        if (input.resolveTarget != null) resolveTarget = (input.resolveTarget as TextureView).handler
        input.clearValue?.let { clearValue = map(it) }
    }
