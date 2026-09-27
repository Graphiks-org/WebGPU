package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUPipelineConstantValue
import org.graphiks.webgpu.bindings.WebGpuRecord
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createWebGpuRecord
import org.graphiks.webgpu.bindings.setRecordValue

internal fun mapConstants(input: Map<String, GPUPipelineConstantValue>): WebGpuRecord =
    createWebGpuRecord().also { record ->
        input.forEach { (name, value) -> setRecordValue(record, name, value.asJsNumber()) }
    }
