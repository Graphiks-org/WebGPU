@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.browser.AdapterInfo
import org.graphiks.webgpu.GPUAdapterInfo
import org.graphiks.webgpu.bindings.WGPUAdapterInfo
import kotlin.OptIn
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toInt
import kotlin.toUInt

internal fun map(input: WGPUAdapterInfo): GPUAdapterInfo = AdapterInfo(
    architecture = input.architecture,
    vendor = input.vendor,
    device = input.device,
    description = input.description,
    subgroupMinSize = input.subgroupMinSize.toInt().toUInt(),
    subgroupMaxSize = input.subgroupMaxSize.toInt().toUInt(),
    isFallbackAdapter = input.isFallbackAdapter
)
