package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

class RenderBundle(val handler: WGPURenderBundle): GPURenderBundle {
    override var label: String
        get() = handler.label
        set(value) { handler.label = value }
}
