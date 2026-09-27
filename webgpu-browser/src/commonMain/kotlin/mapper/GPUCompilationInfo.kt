@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUCompilationInfo
import org.graphiks.webgpu.GPUCompilationMessage
import org.graphiks.webgpu.GPUCompilationMessageType
import org.graphiks.webgpu.bindings.WGPUCompilationInfo
import org.graphiks.webgpu.bindings.WGPUCompilationMessage
import org.graphiks.webgpu.bindings.map
import org.graphiks.webgpu.bindings.toULong
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.unsafeCast

internal fun map(input: WGPUCompilationInfo): GPUCompilationInfo = object : GPUCompilationInfo {
    override val messages: List<GPUCompilationMessage> = input.messages.map { map(it.unsafeCast<WGPUCompilationMessage>()) }
}

internal fun map(input: WGPUCompilationMessage): GPUCompilationMessage = object : GPUCompilationMessage {
    override val message: String = input.message
    override val type: GPUCompilationMessageType = GPUCompilationMessageType.of(input.type) ?: error("Unknown compilation message type: ${input.type}")
    override val lineNum: ULong = input.lineNum.toULong()
    override val linePos: ULong = input.linePos.toULong()
    override val offset: ULong = input.offset.toULong()
    override val length: ULong = input.length.toULong()
}
