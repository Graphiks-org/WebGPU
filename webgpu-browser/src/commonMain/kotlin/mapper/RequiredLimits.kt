@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPURequiredLimits
import org.graphiks.webgpu.bindings.WebGpuRecord
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createWebGpuRecord
import org.graphiks.webgpu.bindings.setRecordValue
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Serializes a requested-limits record, emitting only the explicitly requested keys.
 *
 * A `null` property means "no constraint" and is omitted; an explicit zero is a real request and
 * is always written, including for the alignment-class limits where zero is invalid and must
 * reject the request instead of being silently replaced by the default alignment.
 */
internal fun map(input: GPURequiredLimits): WebGpuRecord = createWebGpuRecord().also { record ->
    input.maxTextureDimension1D?.let { putRequiredLimit(record, "maxTextureDimension1D", it) }
    input.maxTextureDimension2D?.let { putRequiredLimit(record, "maxTextureDimension2D", it) }
    input.maxTextureDimension3D?.let { putRequiredLimit(record, "maxTextureDimension3D", it) }
    input.maxTextureArrayLayers?.let { putRequiredLimit(record, "maxTextureArrayLayers", it) }
    input.maxBindGroups?.let { putRequiredLimit(record, "maxBindGroups", it) }
    input.maxBindGroupsPlusVertexBuffers?.let { putRequiredLimit(record, "maxBindGroupsPlusVertexBuffers", it) }
    input.maxImmediateSize?.let { putRequiredLimit(record, "maxImmediateSize", it) }
    input.maxBindingsPerBindGroup?.let { putRequiredLimit(record, "maxBindingsPerBindGroup", it) }
    input.maxDynamicUniformBuffersPerPipelineLayout?.let {
        putRequiredLimit(record, "maxDynamicUniformBuffersPerPipelineLayout", it)
    }
    input.maxDynamicStorageBuffersPerPipelineLayout?.let {
        putRequiredLimit(record, "maxDynamicStorageBuffersPerPipelineLayout", it)
    }
    input.maxSampledTexturesPerShaderStage?.let { putRequiredLimit(record, "maxSampledTexturesPerShaderStage", it) }
    input.maxSamplersPerShaderStage?.let { putRequiredLimit(record, "maxSamplersPerShaderStage", it) }
    input.maxStorageBuffersPerShaderStage?.let { putRequiredLimit(record, "maxStorageBuffersPerShaderStage", it) }
    input.maxStorageBuffersInVertexStage?.let { putRequiredLimit(record, "maxStorageBuffersInVertexStage", it) }
    input.maxStorageBuffersInFragmentStage?.let { putRequiredLimit(record, "maxStorageBuffersInFragmentStage", it) }
    input.maxStorageTexturesPerShaderStage?.let { putRequiredLimit(record, "maxStorageTexturesPerShaderStage", it) }
    input.maxStorageTexturesInVertexStage?.let { putRequiredLimit(record, "maxStorageTexturesInVertexStage", it) }
    input.maxStorageTexturesInFragmentStage?.let { putRequiredLimit(record, "maxStorageTexturesInFragmentStage", it) }
    input.maxUniformBuffersPerShaderStage?.let { putRequiredLimit(record, "maxUniformBuffersPerShaderStage", it) }
    input.maxUniformBufferBindingSize?.let { putRequiredLimit(record, "maxUniformBufferBindingSize", it) }
    input.maxStorageBufferBindingSize?.let { putRequiredLimit(record, "maxStorageBufferBindingSize", it) }
    input.minUniformBufferOffsetAlignment?.let {
        putRequiredLimit(record, "minUniformBufferOffsetAlignment", it)
    }
    input.minStorageBufferOffsetAlignment?.let {
        putRequiredLimit(record, "minStorageBufferOffsetAlignment", it)
    }
    input.maxVertexBuffers?.let { putRequiredLimit(record, "maxVertexBuffers", it) }
    input.maxBufferSize?.let { putRequiredLimit(record, "maxBufferSize", it) }
    input.maxVertexAttributes?.let { putRequiredLimit(record, "maxVertexAttributes", it) }
    input.maxVertexBufferArrayStride?.let { putRequiredLimit(record, "maxVertexBufferArrayStride", it) }
    input.maxInterStageShaderVariables?.let { putRequiredLimit(record, "maxInterStageShaderVariables", it) }
    input.maxColorAttachments?.let { putRequiredLimit(record, "maxColorAttachments", it) }
    input.maxColorAttachmentBytesPerSample?.let { putRequiredLimit(record, "maxColorAttachmentBytesPerSample", it) }
    input.maxComputeWorkgroupStorageSize?.let { putRequiredLimit(record, "maxComputeWorkgroupStorageSize", it) }
    input.maxComputeInvocationsPerWorkgroup?.let { putRequiredLimit(record, "maxComputeInvocationsPerWorkgroup", it) }
    input.maxComputeWorkgroupSizeX?.let { putRequiredLimit(record, "maxComputeWorkgroupSizeX", it) }
    input.maxComputeWorkgroupSizeY?.let { putRequiredLimit(record, "maxComputeWorkgroupSizeY", it) }
    input.maxComputeWorkgroupSizeZ?.let { putRequiredLimit(record, "maxComputeWorkgroupSizeZ", it) }
    input.maxComputeWorkgroupsPerDimension?.let { putRequiredLimit(record, "maxComputeWorkgroupsPerDimension", it) }
}

/** Writes a requested limit without filtering the value: an explicit zero is a real request. */
private fun putRequiredLimit(record: WebGpuRecord, name: String, value: UInt) {
    setRecordValue(record, name, value.asJsNumber())
}

private fun putRequiredLimit(record: WebGpuRecord, name: String, value: ULong) {
    setRecordValue(record, name, value.asJsNumber())
}
