@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import kotlin.js.ExperimentalWasmJsInterop

class CommandEncoder(val handler: WGPUCommandEncoder) : GPUCommandEncoder {

    override var label: String
        get() = handler.label
        set(value) {
            handler.label = value
        }

    override fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder = map(descriptor)
        .let(handler::beginRenderPass)
        .let(::RenderPassEncoder)

    override fun beginComputePass(descriptor: GPUComputePassDescriptor?): GPUComputePassEncoder =
        when (descriptor) {
            null -> handler.beginComputePass()
            else -> handler.beginComputePass(map(descriptor))
        }.let(::ComputePassEncoder)

    override fun copyBufferToBuffer(
        source: GPUBuffer,
        sourceOffset: GPUSize64,
        destination: GPUBuffer,
        destinationOffset: GPUSize64,
        size: GPUSize64?
    ) {
        when (size) {
            null ->         handler.copyBufferToBuffer(
                (source as Buffer).handler,
                sourceOffset.asJsNumber(),
                (destination as Buffer).handler,
                destinationOffset.asJsNumber(),
            )
            else -> handler.copyBufferToBuffer(
                (source as Buffer).handler,
                sourceOffset.asJsNumber(),
                (destination as Buffer).handler,
                destinationOffset.asJsNumber(),
                size.asJsNumber()
            )
        }
    }

    override fun copyBufferToTexture(
        source: GPUTexelCopyBufferInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D
    ) {
        handler.copyBufferToTexture(
            map(source),
            map(destination),
            map(copySize)
        )
    }

    override fun copyTextureToBuffer(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyBufferInfo,
        copySize: GPUExtent3D
    ) {
        handler.copyTextureToBuffer(
            map(source),
            map(destination),
            map(copySize)
        )
    }

    override fun copyTextureToTexture(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D
    ) {
        handler.copyTextureToTexture(
            map(source),
            map(destination),
            map(copySize)
        )
    }

    override fun clearBuffer(
        buffer: GPUBuffer,
        offset: GPUSize64,
        size: GPUSize64?
    ) = when (size) {
        null -> handler.clearBuffer(
            (buffer as Buffer).handler,
            offset.asJsNumber()
        )
        else -> handler.clearBuffer(
            (buffer as Buffer).handler,
            offset.asJsNumber(),
            size.asJsNumber()
        )
    }

    override fun resolveQuerySet(
        querySet: GPUQuerySet,
        firstQuery: GPUSize32,
        queryCount: GPUSize32,
        destination: GPUBuffer,
        destinationOffset: GPUSize64
    ) {
        handler.resolveQuerySet(
            (querySet as QuerySet).handler,
            firstQuery.asJsNumber(),
            queryCount.asJsNumber(),
            (destination as Buffer).handler,
            destinationOffset.asJsNumber()
        )
    }

    override fun finish(descriptor: GPUCommandBufferDescriptor?): GPUCommandBuffer = when (descriptor) {
        null -> handler.finish()
        else -> handler.finish(map(descriptor))
    }.let(::CommandBuffer)

    override fun pushDebugGroup(groupLabel: String) {
        handler.pushDebugGroup(groupLabel)
    }

    override fun popDebugGroup() {
        handler.popDebugGroup()
    }

    override fun insertDebugMarker(markerLabel: String) {
        handler.insertDebugMarker(markerLabel)
    }

    override fun close() {
        // Nothing to do
    }
}
