@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.map
import js.promise.await
import kotlin.js.ExperimentalWasmJsInterop

class Queue(val handler: WGPUQueue) : GPUQueue {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override suspend fun onSubmittedWorkDone(): Result<Unit> = runCatching {
        handler.onSubmittedWorkDone()
            .await()
    }

    override fun submit(commandBuffers: List<GPUCommandBuffer>) {
        handler.submit(commandBuffers.mapJsArray { (it as CommandBuffer).handler })
    }

    override fun writeBuffer(
        buffer: GPUBuffer,
        bufferOffset: GPUSize64,
        data: ArrayBuffer,
        dataOffset: GPUSize64,
        size: GPUSize64?
    ) = when (size) {
        null -> handler.writeBuffer(
            (buffer as Buffer).handler,
            bufferOffset.asJsNumber(),
            (data as WebArrayBuffer).buffer,
            dataOffset.asJsNumber()
        )
        else -> handler.writeBuffer(
            (buffer as Buffer).handler,
            bufferOffset.asJsNumber(),
            (data as WebArrayBuffer).buffer,
            dataOffset.asJsNumber(),
            size.asJsNumber()
        )
    }

    override fun writeTexture(
        destination: GPUTexelCopyTextureInfo,
        data: ArrayBuffer,
        dataLayout: GPUTexelCopyBufferLayout,
        size: GPUExtent3D
    ) {
        handler.writeTexture(
            map(destination),
            (data as WebArrayBuffer).buffer,
            map(dataLayout),
            map(size)
        )
    }
}
