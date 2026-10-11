@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.browser.Limits
import org.graphiks.webgpu.bindings.WGPUSupportedLimits
import org.graphiks.webgpu.bindings.toULong
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsNumber
import kotlin.js.js
import kotlin.js.toInt
import kotlin.toUInt

/**
 * Reads one raw limit, treating a property the implementation does not expose as zero.
 *
 * Chromium does not define every limit yet (for example `maxImmediateSize`), and reading such a
 * property as a non-null `JsNumber` throws on Kotlin/Wasm. Falling back to zero keeps the reader
 * usable there.
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
