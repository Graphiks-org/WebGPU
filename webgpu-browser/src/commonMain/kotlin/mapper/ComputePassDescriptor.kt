@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUComputePassDescriptor
import org.graphiks.webgpu.GPUComputePassTimestampWrites
import org.graphiks.webgpu.browser.QuerySet
import org.graphiks.webgpu.bindings.WGPUComputePassDescriptor
import org.graphiks.webgpu.bindings.WGPUComputePassTimestampWrites
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUComputePassDescriptor): WGPUComputePassDescriptor =
    createJsObject<WGPUComputePassDescriptor>().apply {
        label = input.label
        input.timestampWrites?.let { timestampWrites = map(it) }
    }

private fun map(input: GPUComputePassTimestampWrites): WGPUComputePassTimestampWrites =
    createJsObject<WGPUComputePassTimestampWrites>().apply {
        querySet = (input.querySet as QuerySet).handler
        input.beginningOfPassWriteIndex?.let { beginningOfPassWriteIndex = it.asJsNumber() }
        input.endOfPassWriteIndex?.let { endOfPassWriteIndex = it.asJsNumber() }
    }
