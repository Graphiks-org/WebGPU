@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.browser.Buffer
import org.graphiks.webgpu.GPUTexelCopyBufferInfo
import org.graphiks.webgpu.bindings.WGPUTexelCopyBufferInfo
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import kotlin.js.ExperimentalWasmJsInterop

internal fun map(input: GPUTexelCopyBufferInfo) = createJsObject<WGPUTexelCopyBufferInfo>().apply {
    buffer = (input.buffer as Buffer).handler
    offset = input.offset.asJsNumber()
    input.bytesPerRow?.let { bytesPerRow = it.asJsNumber() }
    input.rowsPerImage?.let { rowsPerImage = it.asJsNumber() }
}
