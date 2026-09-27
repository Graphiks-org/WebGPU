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
import kotlin.js.toInt
import kotlin.toUInt

internal fun map(input: GPUSupportedLimits): WebGpuRecord = createWebGpuRecord().also { record ->
    setRecordValue(record, "maxTextureDimension1D", input.maxTextureDimension1D.asJsNumber())
    setRecordValue(record, "maxTextureDimension2D", input.maxTextureDimension2D.asJsNumber())
    setRecordValue(record, "maxTextureDimension3D", input.maxTextureDimension3D.asJsNumber())
    setRecordValue(record, "maxTextureArrayLayers", input.maxTextureArrayLayers.asJsNumber())
    setRecordValue(record, "maxBindGroups", input.maxBindGroups.asJsNumber())
    setRecordValue(record, "maxBindGroupsPlusVertexBuffers", input.maxBindGroupsPlusVertexBuffers.asJsNumber())
    setRecordValue(record, "maxBindingsPerBindGroup", input.maxBindingsPerBindGroup.asJsNumber())
    setRecordValue(record, "maxDynamicUniformBuffersPerPipelineLayout", input.maxDynamicUniformBuffersPerPipelineLayout.asJsNumber())
    setRecordValue(record, "maxDynamicStorageBuffersPerPipelineLayout", input.maxDynamicStorageBuffersPerPipelineLayout.asJsNumber())
    setRecordValue(record, "maxSampledTexturesPerShaderStage", input.maxSampledTexturesPerShaderStage.asJsNumber())
    setRecordValue(record, "maxSamplersPerShaderStage", input.maxSamplersPerShaderStage.asJsNumber())
    setRecordValue(record, "maxStorageBuffersPerShaderStage", input.maxStorageBuffersPerShaderStage.asJsNumber())
    setRecordValue(record, "maxStorageTexturesPerShaderStage", input.maxStorageTexturesPerShaderStage.asJsNumber())
    setRecordValue(record, "maxUniformBuffersPerShaderStage", input.maxUniformBuffersPerShaderStage.asJsNumber())
    setRecordValue(record, "maxUniformBufferBindingSize", input.maxUniformBufferBindingSize.asJsNumber())
    setRecordValue(record, "maxStorageBufferBindingSize", input.maxStorageBufferBindingSize.asJsNumber())
    setRecordValue(record, "minUniformBufferOffsetAlignment", input.minUniformBufferOffsetAlignment.asJsNumber())
    setRecordValue(record, "minStorageBufferOffsetAlignment", input.minStorageBufferOffsetAlignment.asJsNumber())
    setRecordValue(record, "maxVertexBuffers", input.maxVertexBuffers.asJsNumber())
    setRecordValue(record, "maxBufferSize", input.maxBufferSize.asJsNumber())
    setRecordValue(record, "maxVertexAttributes", input.maxVertexAttributes.asJsNumber())
    setRecordValue(record, "maxVertexBufferArrayStride", input.maxVertexBufferArrayStride.asJsNumber())
    setRecordValue(record, "maxInterStageShaderVariables", input.maxInterStageShaderVariables.asJsNumber())
    setRecordValue(record, "maxColorAttachments", input.maxColorAttachments.asJsNumber())
    setRecordValue(record, "maxColorAttachmentBytesPerSample", input.maxColorAttachmentBytesPerSample.asJsNumber())
    setRecordValue(record, "maxComputeWorkgroupStorageSize", input.maxComputeWorkgroupStorageSize.asJsNumber())
    setRecordValue(record, "maxComputeInvocationsPerWorkgroup", input.maxComputeInvocationsPerWorkgroup.asJsNumber())
    setRecordValue(record, "maxComputeWorkgroupSizeX", input.maxComputeWorkgroupSizeX.asJsNumber())
    setRecordValue(record, "maxComputeWorkgroupSizeY", input.maxComputeWorkgroupSizeY.asJsNumber())
    setRecordValue(record, "maxComputeWorkgroupSizeZ", input.maxComputeWorkgroupSizeZ.asJsNumber())
    setRecordValue(record, "maxComputeWorkgroupsPerDimension", input.maxComputeWorkgroupsPerDimension.asJsNumber())
    setRecordValue(record, "maxImmediateSize", input.maxImmediateSize.asJsNumber())
    setRecordValue(record, "maxStorageBuffersInVertexStage", input.maxStorageBuffersInVertexStage.asJsNumber())
    setRecordValue(record, "maxStorageBuffersInFragmentStage", input.maxStorageBuffersInFragmentStage.asJsNumber())
    setRecordValue(record, "maxStorageTexturesInVertexStage", input.maxStorageTexturesInVertexStage.asJsNumber())
    setRecordValue(record, "maxStorageTexturesInFragmentStage", input.maxStorageTexturesInFragmentStage.asJsNumber())
}

internal fun map(input: WGPUSupportedLimits): GPUSupportedLimits = Limits(
    maxTextureDimension1D = input.maxTextureDimension1D.toInt().toUInt(),
    maxTextureDimension2D = input.maxTextureDimension2D.toInt().toUInt(),
    maxTextureDimension3D = input.maxTextureDimension3D.toInt().toUInt(),
    maxTextureArrayLayers = input.maxTextureArrayLayers.toInt().toUInt(),
    maxBindGroups = input.maxBindGroups.toInt().toUInt(),
    maxBindGroupsPlusVertexBuffers = input.maxBindGroupsPlusVertexBuffers.toInt().toUInt(),
    maxBindingsPerBindGroup = input.maxBindingsPerBindGroup.toInt().toUInt(),
    maxDynamicUniformBuffersPerPipelineLayout = input.maxDynamicUniformBuffersPerPipelineLayout.toInt().toUInt(),
    maxDynamicStorageBuffersPerPipelineLayout = input.maxDynamicStorageBuffersPerPipelineLayout.toInt().toUInt(),
    maxSampledTexturesPerShaderStage = input.maxSampledTexturesPerShaderStage.toInt().toUInt(),
    maxSamplersPerShaderStage = input.maxSamplersPerShaderStage.toInt().toUInt(),
    maxStorageBuffersPerShaderStage = input.maxStorageBuffersPerShaderStage.toInt().toUInt(),
    maxStorageTexturesPerShaderStage = input.maxStorageTexturesPerShaderStage.toInt().toUInt(),
    maxUniformBuffersPerShaderStage = input.maxUniformBuffersPerShaderStage.toInt().toUInt(),
    maxUniformBufferBindingSize = input.maxUniformBufferBindingSize.toULong(),
    maxStorageBufferBindingSize = input.maxStorageBufferBindingSize.toULong(),
    minUniformBufferOffsetAlignment = input.minUniformBufferOffsetAlignment.toInt().toUInt(),
    minStorageBufferOffsetAlignment = input.minStorageBufferOffsetAlignment.toInt().toUInt(),
    maxVertexBuffers = input.maxVertexBuffers.toInt().toUInt(),
    maxBufferSize = input.maxBufferSize.toULong(),
    maxVertexAttributes = input.maxVertexAttributes.toInt().toUInt(),
    maxVertexBufferArrayStride = input.maxVertexBufferArrayStride.toInt().toUInt(),
    maxInterStageShaderVariables = input.maxInterStageShaderVariables.toInt().toUInt(),
    maxColorAttachments = input.maxColorAttachments.toInt().toUInt(),
    maxColorAttachmentBytesPerSample = input.maxColorAttachmentBytesPerSample.toInt().toUInt(),
    maxComputeWorkgroupStorageSize = input.maxComputeWorkgroupStorageSize.toInt().toUInt(),
    maxComputeInvocationsPerWorkgroup = input.maxComputeInvocationsPerWorkgroup.toInt().toUInt(),
    maxComputeWorkgroupSizeX = input.maxComputeWorkgroupSizeX.toInt().toUInt(),
    maxComputeWorkgroupSizeY = input.maxComputeWorkgroupSizeY.toInt().toUInt(),
    maxComputeWorkgroupSizeZ = input.maxComputeWorkgroupSizeZ.toInt().toUInt(),
    maxComputeWorkgroupsPerDimension = input.maxComputeWorkgroupsPerDimension.toInt().toUInt(),
    maxImmediateSize = input.maxImmediateSize.toInt().toUInt(),
    maxStorageBuffersInVertexStage = input.maxStorageBuffersInVertexStage.toInt().toUInt(),
    maxStorageBuffersInFragmentStage = input.maxStorageBuffersInFragmentStage.toInt().toUInt(),
    maxStorageTexturesInVertexStage = input.maxStorageTexturesInVertexStage.toInt().toUInt(),
    maxStorageTexturesInFragmentStage = input.maxStorageTexturesInFragmentStage.toInt().toUInt(),
)
