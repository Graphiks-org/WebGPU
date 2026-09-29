@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.browser.Limits
import org.graphiks.webgpu.bindings.WGPUSupportedLimits
import org.graphiks.webgpu.bindings.WebGpuRecord
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createWebGpuRecord
import org.graphiks.webgpu.bindings.setRecordValue
import org.graphiks.webgpu.bindings.toULong
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsNumber
import kotlin.js.js
import kotlin.js.toInt
import kotlin.toUInt

/**
 * Maps the requested limits onto a `requiredLimits` record.
 *
 * A maximum-class limit whose value is zero is omitted. The reader maps a limit the implementation
 * does not expose to zero, and a browser rejects a `requiredLimits` key it does not recognise
 * (`maxImmediateSize`, for example, is absent on older Chromium). For a maximum, a zero requirement
 * is always satisfiable, so omitting it keeps the request valid on implementations older than the
 * contract.
 *
 * The two alignment-class limits are always sent. Zero is not a satisfiable alignment: it is an
 * invalid requirement, and omitting it would silently replace a bad request with the default
 * alignment instead of rejecting it.
 */
internal fun map(input: GPUSupportedLimits): WebGpuRecord = createWebGpuRecord().also { record ->
    putLimit(record, "maxTextureDimension1D", input.maxTextureDimension1D)
    putLimit(record, "maxTextureDimension2D", input.maxTextureDimension2D)
    putLimit(record, "maxTextureDimension3D", input.maxTextureDimension3D)
    putLimit(record, "maxTextureArrayLayers", input.maxTextureArrayLayers)
    putLimit(record, "maxBindGroups", input.maxBindGroups)
    putLimit(record, "maxBindGroupsPlusVertexBuffers", input.maxBindGroupsPlusVertexBuffers)
    putLimit(record, "maxBindingsPerBindGroup", input.maxBindingsPerBindGroup)
    putLimit(record, "maxDynamicUniformBuffersPerPipelineLayout", input.maxDynamicUniformBuffersPerPipelineLayout)
    putLimit(record, "maxDynamicStorageBuffersPerPipelineLayout", input.maxDynamicStorageBuffersPerPipelineLayout)
    putLimit(record, "maxSampledTexturesPerShaderStage", input.maxSampledTexturesPerShaderStage)
    putLimit(record, "maxSamplersPerShaderStage", input.maxSamplersPerShaderStage)
    putLimit(record, "maxStorageBuffersPerShaderStage", input.maxStorageBuffersPerShaderStage)
    putLimit(record, "maxStorageTexturesPerShaderStage", input.maxStorageTexturesPerShaderStage)
    putLimit(record, "maxUniformBuffersPerShaderStage", input.maxUniformBuffersPerShaderStage)
    putLimit(record, "maxUniformBufferBindingSize", input.maxUniformBufferBindingSize)
    putLimit(record, "maxStorageBufferBindingSize", input.maxStorageBufferBindingSize)
    putRequiredLimit(record, "minUniformBufferOffsetAlignment", input.minUniformBufferOffsetAlignment)
    putRequiredLimit(record, "minStorageBufferOffsetAlignment", input.minStorageBufferOffsetAlignment)
    putLimit(record, "maxVertexBuffers", input.maxVertexBuffers)
    putLimit(record, "maxBufferSize", input.maxBufferSize)
    putLimit(record, "maxVertexAttributes", input.maxVertexAttributes)
    putLimit(record, "maxVertexBufferArrayStride", input.maxVertexBufferArrayStride)
    putLimit(record, "maxInterStageShaderVariables", input.maxInterStageShaderVariables)
    putLimit(record, "maxColorAttachments", input.maxColorAttachments)
    putLimit(record, "maxColorAttachmentBytesPerSample", input.maxColorAttachmentBytesPerSample)
    putLimit(record, "maxComputeWorkgroupStorageSize", input.maxComputeWorkgroupStorageSize)
    putLimit(record, "maxComputeInvocationsPerWorkgroup", input.maxComputeInvocationsPerWorkgroup)
    putLimit(record, "maxComputeWorkgroupSizeX", input.maxComputeWorkgroupSizeX)
    putLimit(record, "maxComputeWorkgroupSizeY", input.maxComputeWorkgroupSizeY)
    putLimit(record, "maxComputeWorkgroupSizeZ", input.maxComputeWorkgroupSizeZ)
    putLimit(record, "maxComputeWorkgroupsPerDimension", input.maxComputeWorkgroupsPerDimension)
    putLimit(record, "maxImmediateSize", input.maxImmediateSize)
    putLimit(record, "maxStorageBuffersInVertexStage", input.maxStorageBuffersInVertexStage)
    putLimit(record, "maxStorageBuffersInFragmentStage", input.maxStorageBuffersInFragmentStage)
    putLimit(record, "maxStorageTexturesInVertexStage", input.maxStorageTexturesInVertexStage)
    putLimit(record, "maxStorageTexturesInFragmentStage", input.maxStorageTexturesInFragmentStage)
}

private fun putLimit(record: WebGpuRecord, name: String, value: UInt) {
    if (value != 0u) setRecordValue(record, name, value.asJsNumber())
}

private fun putLimit(record: WebGpuRecord, name: String, value: ULong) {
    if (value != 0uL) setRecordValue(record, name, value.asJsNumber())
}

/** Sends a limit whose value is always meaningful, including an invalid zero. */
private fun putRequiredLimit(record: WebGpuRecord, name: String, value: UInt) {
    setRecordValue(record, name, value.asJsNumber())
}

/**
 * Reads one raw limit, treating a property the implementation does not expose as zero.
 *
 * Chromium does not define every limit yet (for example `maxImmediateSize`), and reading such a
 * property as a non-null `JsNumber` throws on Kotlin/Wasm. Falling back to zero keeps the mapper
 * usable there; a required limit is always present.
 */
private fun limitValue(input: WGPUSupportedLimits, name: String): JsNumber =
    js("input[name] === undefined ? 0 : input[name]")

internal fun map(input: WGPUSupportedLimits): GPUSupportedLimits = Limits(
    maxTextureDimension1D = limitValue(input, "maxTextureDimension1D").toInt().toUInt(),
    maxTextureDimension2D = limitValue(input, "maxTextureDimension2D").toInt().toUInt(),
    maxTextureDimension3D = limitValue(input, "maxTextureDimension3D").toInt().toUInt(),
    maxTextureArrayLayers = limitValue(input, "maxTextureArrayLayers").toInt().toUInt(),
    maxBindGroups = limitValue(input, "maxBindGroups").toInt().toUInt(),
    maxBindGroupsPlusVertexBuffers = limitValue(input, "maxBindGroupsPlusVertexBuffers").toInt().toUInt(),
    maxBindingsPerBindGroup = limitValue(input, "maxBindingsPerBindGroup").toInt().toUInt(),
    maxDynamicUniformBuffersPerPipelineLayout = limitValue(input, "maxDynamicUniformBuffersPerPipelineLayout").toInt().toUInt(),
    maxDynamicStorageBuffersPerPipelineLayout = limitValue(input, "maxDynamicStorageBuffersPerPipelineLayout").toInt().toUInt(),
    maxSampledTexturesPerShaderStage = limitValue(input, "maxSampledTexturesPerShaderStage").toInt().toUInt(),
    maxSamplersPerShaderStage = limitValue(input, "maxSamplersPerShaderStage").toInt().toUInt(),
    maxStorageBuffersPerShaderStage = limitValue(input, "maxStorageBuffersPerShaderStage").toInt().toUInt(),
    maxStorageTexturesPerShaderStage = limitValue(input, "maxStorageTexturesPerShaderStage").toInt().toUInt(),
    maxUniformBuffersPerShaderStage = limitValue(input, "maxUniformBuffersPerShaderStage").toInt().toUInt(),
    maxUniformBufferBindingSize = limitValue(input, "maxUniformBufferBindingSize").toULong(),
    maxStorageBufferBindingSize = limitValue(input, "maxStorageBufferBindingSize").toULong(),
    minUniformBufferOffsetAlignment = limitValue(input, "minUniformBufferOffsetAlignment").toInt().toUInt(),
    minStorageBufferOffsetAlignment = limitValue(input, "minStorageBufferOffsetAlignment").toInt().toUInt(),
    maxVertexBuffers = limitValue(input, "maxVertexBuffers").toInt().toUInt(),
    maxBufferSize = limitValue(input, "maxBufferSize").toULong(),
    maxVertexAttributes = limitValue(input, "maxVertexAttributes").toInt().toUInt(),
    maxVertexBufferArrayStride = limitValue(input, "maxVertexBufferArrayStride").toInt().toUInt(),
    maxInterStageShaderVariables = limitValue(input, "maxInterStageShaderVariables").toInt().toUInt(),
    maxColorAttachments = limitValue(input, "maxColorAttachments").toInt().toUInt(),
    maxColorAttachmentBytesPerSample = limitValue(input, "maxColorAttachmentBytesPerSample").toInt().toUInt(),
    maxComputeWorkgroupStorageSize = limitValue(input, "maxComputeWorkgroupStorageSize").toInt().toUInt(),
    maxComputeInvocationsPerWorkgroup = limitValue(input, "maxComputeInvocationsPerWorkgroup").toInt().toUInt(),
    maxComputeWorkgroupSizeX = limitValue(input, "maxComputeWorkgroupSizeX").toInt().toUInt(),
    maxComputeWorkgroupSizeY = limitValue(input, "maxComputeWorkgroupSizeY").toInt().toUInt(),
    maxComputeWorkgroupSizeZ = limitValue(input, "maxComputeWorkgroupSizeZ").toInt().toUInt(),
    maxComputeWorkgroupsPerDimension = limitValue(input, "maxComputeWorkgroupsPerDimension").toInt().toUInt(),
    maxImmediateSize = limitValue(input, "maxImmediateSize").toInt().toUInt(),
    maxStorageBuffersInVertexStage = limitValue(input, "maxStorageBuffersInVertexStage").toInt().toUInt(),
    maxStorageBuffersInFragmentStage = limitValue(input, "maxStorageBuffersInFragmentStage").toInt().toUInt(),
    maxStorageTexturesInVertexStage = limitValue(input, "maxStorageTexturesInVertexStage").toInt().toUInt(),
    maxStorageTexturesInFragmentStage = limitValue(input, "maxStorageTexturesInFragmentStage").toInt().toUInt(),
)
