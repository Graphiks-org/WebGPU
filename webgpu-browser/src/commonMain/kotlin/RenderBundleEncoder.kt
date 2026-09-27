@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import kotlin.js.ExperimentalWasmJsInterop

class RenderBundleEncoder(
    val handler: WGPURenderBundleEncoder,
) : GPURenderBundleEncoder {

    override var label: String
        get() = handler.label
        set(value) {
            handler.label = value
        }

    override fun finish(descriptor: GPURenderBundleDescriptor?): GPURenderBundle = when (descriptor) {
        null -> handler.finish()
        else -> handler.finish(map(descriptor))
    }.let(::RenderBundle)

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
    ) = when (size) {
        null -> handler.setVertexBuffer(
            slot.asJsNumber(),
            (buffer as Buffer).handler,
            offset.asJsNumber()
        )
        else -> handler.setVertexBuffer(
            slot.asJsNumber(),
            (buffer as Buffer).handler,
            offset.asJsNumber(),
            size.asJsNumber()
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
        handler.setBindGroup(
            index.asJsNumber(),
            (bindGroup as BindGroup).handler,
            map(dynamicOffsetsData)
        )
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

    override fun close() {
        // Nothing to do on js
    }

}
