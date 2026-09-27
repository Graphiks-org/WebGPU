@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUTexelCopyBufferLayout
import org.graphiks.webgpu.bindings.WGPUTexelCopyBufferLayout
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUTexelCopyBufferLayout): WGPUTexelCopyBufferLayout = createJsObject<WGPUTexelCopyBufferLayout>().apply {
    offset = input.offset.asJsNumber()
    input.bytesPerRow?.let { bytesPerRow = it.asJsNumber() }
    input.rowsPerImage?.let { rowsPerImage = it.asJsNumber() }
}
