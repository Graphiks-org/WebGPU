@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import kotlin.js.ExperimentalWasmJsInterop

class RenderPassEncoder(val handler: WGPURenderPassEncoder): GPURenderPassEncoder {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override fun end() {
        handler.end()
    }

    override fun setPipeline(pipeline: GPURenderPipeline) {
        handler.setPipeline((pipeline as RenderPipeline).handler)
    }

    override fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: GPUIndexFormat,
        offset: GPUSize64,
        size: GPUSize64?
    ) = when (size) {
        null -> handler.setIndexBuffer(
            (buffer as Buffer).handler,
            indexFormat.value,
            offset.asJsNumber()
        )
        else -> handler.setIndexBuffer(
            (buffer as Buffer).handler,
            indexFormat.value,
            offset.asJsNumber(),
            size.asJsNumber()
        )
    }

    override fun setVertexBuffer(
        slot: GPUIndex32,
        buffer: GPUBuffer?,
        offset: GPUSize64,
        size: GPUSize64?
    ) {
        val raw = (buffer as Buffer?)?.handler
        when {
            size != null -> handler.setVertexBuffer(slot.asJsNumber(), raw, offset.asJsNumber(), size.asJsNumber())
            offset == 0uL -> handler.setVertexBuffer(slot.asJsNumber(), raw)
            else -> handler.setVertexBuffer(slot.asJsNumber(), raw, offset.asJsNumber())
        }
    }

    override fun draw(
        vertexCount: GPUSize32,
        instanceCount: GPUSize32,
        firstVertex: GPUSize32,
        firstInstance: GPUSize32,
    ) {
        handler.draw(
            vertexCount.asJsNumber(),
            instanceCount.asJsNumber(),
            firstVertex.asJsNumber(),
            firstInstance.asJsNumber()
        )
    }

    override fun drawIndexed(
        indexCount: GPUSize32,
        instanceCount: GPUSize32,
        firstIndex: GPUSize32,
        baseVertex: GPUSignedOffset32,
        firstInstance: GPUSize32,
    ) {
        handler.drawIndexed(
            indexCount.asJsNumber(),
            instanceCount.asJsNumber(),
            firstIndex.asJsNumber(),
            baseVertex.asJsNumber(),
            firstInstance.asJsNumber()
        )
    }

    override fun drawIndirect(
        indirectBuffer: GPUBuffer,
        indirectOffset: GPUSize64
    ) {
        handler.drawIndirect((indirectBuffer as Buffer).handler, indirectOffset.asJsNumber())
    }

    override fun drawIndexedIndirect(
        indirectBuffer: GPUBuffer,
        indirectOffset: GPUSize64
    ) {
        handler.drawIndexedIndirect((indirectBuffer as Buffer).handler, indirectOffset.asJsNumber())
    }

    override fun executeBundles(bundles: List<GPURenderBundle>) {
        handler.executeBundles(bundles.mapJsArray { (it as RenderBundle).handler })
    }

    override fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float) {
        handler.setViewport(
            x.asJsNumber(),
            y.asJsNumber(),
            width.asJsNumber(),
            height.asJsNumber(),
            minDepth.asJsNumber(),
            maxDepth.asJsNumber()
        )
    }

    override fun setScissorRect(
        x: GPUIntegerCoordinate,
        y: GPUIntegerCoordinate,
        width: GPUIntegerCoordinate,
        height: GPUIntegerCoordinate,
    ) {
        handler.setScissorRect(
            x.asJsNumber(),
            y.asJsNumber(),
            width.asJsNumber(),
            height.asJsNumber()
        )
    }

    override fun setBlendConstant(color: GPUColor) {
        handler.setBlendConstant(map(color))
    }

    override fun setStencilReference(reference: GPUStencilValue) {
        handler.setStencilReference(reference.asJsNumber())
    }

    override fun beginOcclusionQuery(queryIndex: GPUSize32) {
        handler.beginOcclusionQuery(queryIndex.asJsNumber())
    }

    override fun endOcclusionQuery() {
        handler.endOcclusionQuery()
    }

    override fun pushDebugGroup(groupLabel: String) {
        handler.pushDebugGroup(groupLabel)
    }

    override fun popDebugGroup() {
        handler.popDebugGroup()
    }

    override fun insertDebugMarker(markerLabel: String) {
        handler.insertDebugMarker(markerLabel)
    }

    override fun setBindGroup(
        index: GPUIndex32,
        bindGroup: GPUBindGroup?,
        dynamicOffsetsData: List<UInt>
    ) {
        val raw = (bindGroup as BindGroup?)?.handler
        when {
            dynamicOffsetsData.isEmpty() -> handler.setBindGroup(index.asJsNumber(), raw)
            else -> handler.setBindGroup(index.asJsNumber(), raw, map(dynamicOffsetsData))
        }
    }

    override fun setImmediates(
        rangeOffset: GPUSize32,
        data: ArrayBuffer,
        dataOffset: GPUSize64,
        dataSize: GPUSize64?,
    ) {
        val raw = (data as WebArrayBuffer).buffer
        when (dataSize) {
            null -> handler.setImmediates(rangeOffset.asJsNumber(), raw, dataOffset.asJsNumber())
            else -> handler.setImmediates(rangeOffset.asJsNumber(), raw, dataOffset.asJsNumber(), dataSize.asJsNumber())
        }
    }

}
