package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

class BindGroupLayout(val handler: WGPUBindGroupLayout) : GPUBindGroupLayout {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override fun close() {
        // Nothing to do on JS
    }
    
}
