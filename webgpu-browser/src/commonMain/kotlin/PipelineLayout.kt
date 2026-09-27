package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

class PipelineLayout(internal var handler: WGPUPipelineLayout) : GPUPipelineLayout {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override fun close() {
        // Nothing to do on js
    }
}
