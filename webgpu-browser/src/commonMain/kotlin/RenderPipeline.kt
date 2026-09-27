@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import kotlin.js.ExperimentalWasmJsInterop

class RenderPipeline(internal var handler: WGPURenderPipeline) : GPURenderPipeline {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override fun getBindGroupLayout(index: GPUSize32): GPUBindGroupLayout = handler
        .getBindGroupLayout(index.asJsNumber())
        .let(::BindGroupLayout)


    override fun close() {
        // Nothing to do on js
    }

}
