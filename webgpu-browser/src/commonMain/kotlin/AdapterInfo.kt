package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

internal data class AdapterInfo(
    override val architecture: String,
    override val description: String,
    override val device: String,
    override val subgroupMaxSize: UInt,
    override val subgroupMinSize: UInt,
    override val vendor: String,
    override val isFallbackAdapter: Boolean
): GPUAdapterInfo
