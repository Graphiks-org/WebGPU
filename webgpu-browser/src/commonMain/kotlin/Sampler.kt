package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

class Sampler(val handler: WGPUSampler) : GPUSampler {
    override var label: String
        get() = handler.label
        set(value) { handler.label = value }

    override fun close() {
        // Nothing to do on JS
    }

}
