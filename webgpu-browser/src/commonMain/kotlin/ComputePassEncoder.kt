@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import kotlin.js.ExperimentalWasmJsInterop

class ComputePassEncoder(val handler: WGPUComputePassEncoder) : GPUComputePassEncoder {
    override var label: String
        get() = handler.label
        set(value) {
            handler.label = value
        }

    override fun setPipeline(pipeline: GPUComputePipeline) = handler.setPipeline((pipeline as ComputePipeline).handler)

    override fun dispatchWorkgroups(
        workgroupCountX: GPUSize32,
        workgroupCountY: GPUSize32,
        workgroupCountZ: GPUSize32
    ) = handler.dispatchWorkgroups(
        workgroupCountX.asJsNumber(),
        workgroupCountY.asJsNumber(),
        workgroupCountZ.asJsNumber()
    )

    override fun dispatchWorkgroupsIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) =
        handler.dispatchWorkgroupsIndirect((indirectBuffer as Buffer).handler, indirectOffset.asJsNumber())

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

    override fun end() = handler.end()
}
