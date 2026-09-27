package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

class BindGroup(val handler: WGPUBindGroup) : GPUBindGroup {

    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override fun close() {
        // Nothing to do on Web
    }

}
