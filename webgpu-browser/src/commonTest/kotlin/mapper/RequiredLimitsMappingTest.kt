@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.bindings.WebGpuRecord
import org.graphiks.webgpu.bindings.map
import org.graphiks.webgpu.descriptors.RequiredLimits
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.js
import kotlin.js.toInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private fun recordKeysRaw(record: WebGpuRecord): JsArray<JsAny> = js("Object.keys(record)")

private fun recordKeys(record: WebGpuRecord): List<String> = recordKeysRaw(record).map { it.toString() }

private fun recordValue(record: WebGpuRecord, key: String): JsNumber? =
    js("record[key] === undefined ? null : record[key]")

class RequiredLimitsMappingTest {

    @Test
    fun empty_request_serializes_no_keys() {
        val record = map(RequiredLimits())
        assertEquals(emptyList(), recordKeys(record))
    }

    @Test
    fun single_buffer_size_request_serializes_exactly_one_key() {
        val record = map(RequiredLimits(maxBufferSize = 65536uL))
        assertEquals(listOf("maxBufferSize"), recordKeys(record))
        assertEquals(65536, recordValue(record, "maxBufferSize")?.toInt())
    }

    @Test
    fun explicit_zero_alignment_is_sent() {
        val record = map(RequiredLimits(minUniformBufferOffsetAlignment = 0u))
        assertEquals(listOf("minUniformBufferOffsetAlignment"), recordKeys(record))
        assertNotNull(recordValue(record, "minUniformBufferOffsetAlignment"))
        assertEquals(0, recordValue(record, "minUniformBufferOffsetAlignment")?.toInt())
    }

    @Test
    fun explicit_zero_maximum_stays_present() {
        val record = map(RequiredLimits(maxTextureDimension1D = 0u))
        assertEquals(listOf("maxTextureDimension1D"), recordKeys(record))
        assertEquals(0, recordValue(record, "maxTextureDimension1D")?.toInt())
    }

    @Test
    fun absent_key_is_not_serialized_as_null() {
        val record = map(RequiredLimits(maxBindGroups = 4u))
        assertNull(recordValue(record, "maxBufferSize"))
        assertNull(recordValue(record, "minStorageBufferOffsetAlignment"))
    }

    @Test
    fun full_request_covers_the_whole_contract() {
        val record = map(
            RequiredLimits(
                maxTextureDimension1D = 1u,
                maxTextureDimension2D = 2u,
                maxTextureDimension3D = 3u,
                maxTextureArrayLayers = 4u,
                maxBindGroups = 5u,
                maxBindGroupsPlusVertexBuffers = 6u,
                maxImmediateSize = 7u,
                maxBindingsPerBindGroup = 8u,
                maxDynamicUniformBuffersPerPipelineLayout = 9u,
                maxDynamicStorageBuffersPerPipelineLayout = 10u,
                maxSampledTexturesPerShaderStage = 11u,
                maxSamplersPerShaderStage = 12u,
                maxStorageBuffersPerShaderStage = 13u,
                maxStorageBuffersInVertexStage = 14u,
                maxStorageBuffersInFragmentStage = 15u,
                maxStorageTexturesPerShaderStage = 16u,
                maxStorageTexturesInVertexStage = 17u,
                maxStorageTexturesInFragmentStage = 18u,
                maxUniformBuffersPerShaderStage = 19u,
                maxUniformBufferBindingSize = 20uL,
                maxStorageBufferBindingSize = 21uL,
                minUniformBufferOffsetAlignment = 22u,
                minStorageBufferOffsetAlignment = 23u,
                maxVertexBuffers = 24u,
                maxBufferSize = 25uL,
                maxVertexAttributes = 26u,
                maxVertexBufferArrayStride = 27u,
                maxInterStageShaderVariables = 28u,
                maxColorAttachments = 29u,
                maxColorAttachmentBytesPerSample = 30u,
                maxComputeWorkgroupStorageSize = 31u,
                maxComputeInvocationsPerWorkgroup = 32u,
                maxComputeWorkgroupSizeX = 33u,
                maxComputeWorkgroupSizeY = 34u,
                maxComputeWorkgroupSizeZ = 35u,
                maxComputeWorkgroupsPerDimension = 36u,
            ),
        )
        assertEquals(CONTRACT_KEYS.toSet(), recordKeys(record).toSet())
        assertEquals(36, recordKeys(record).size)
    }

    private companion object {
        val CONTRACT_KEYS = listOf(
            "maxTextureDimension1D",
            "maxTextureDimension2D",
            "maxTextureDimension3D",
            "maxTextureArrayLayers",
            "maxBindGroups",
            "maxBindGroupsPlusVertexBuffers",
            "maxImmediateSize",
            "maxBindingsPerBindGroup",
            "maxDynamicUniformBuffersPerPipelineLayout",
            "maxDynamicStorageBuffersPerPipelineLayout",
            "maxSampledTexturesPerShaderStage",
            "maxSamplersPerShaderStage",
            "maxStorageBuffersPerShaderStage",
            "maxStorageBuffersInVertexStage",
            "maxStorageBuffersInFragmentStage",
            "maxStorageTexturesPerShaderStage",
            "maxStorageTexturesInVertexStage",
            "maxStorageTexturesInFragmentStage",
            "maxUniformBuffersPerShaderStage",
            "maxUniformBufferBindingSize",
            "maxStorageBufferBindingSize",
            "minUniformBufferOffsetAlignment",
            "minStorageBufferOffsetAlignment",
            "maxVertexBuffers",
            "maxBufferSize",
            "maxVertexAttributes",
            "maxVertexBufferArrayStride",
            "maxInterStageShaderVariables",
            "maxColorAttachments",
            "maxColorAttachmentBytesPerSample",
            "maxComputeWorkgroupStorageSize",
            "maxComputeInvocationsPerWorkgroup",
            "maxComputeWorkgroupSizeX",
            "maxComputeWorkgroupSizeY",
            "maxComputeWorkgroupSizeZ",
            "maxComputeWorkgroupsPerDimension",
        )
    }
}
