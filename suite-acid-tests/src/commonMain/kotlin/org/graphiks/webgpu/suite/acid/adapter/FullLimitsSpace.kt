package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.RequiredLimits
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidInput
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A device requested with `requiredLimits` set to the adapter's own value for **every** limit of the
 * contract succeeds, and the granted device reports exactly those values: the whole limit space is
 * enumerated, requested and observed field by field, not just the one field the sibling
 * `device.required-limits` case pins. This observes the concordance of the granted device with the
 * requested record — it is a state round trip, not an independent exercise of each limit — and the
 * core feature and the adapter identity are observed too.
 */
@AcidTest(
    id = AcidCaseId.DeviceFullLimitsSpace,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    contract = [
        ApiSymbols.GPUAdapter_requestDevice,
        ApiSymbols.GPUAdapter_limits,
        ApiSymbols.GPUSupportedLimits,
        ApiSymbols.GPUSupportedLimits_maxBufferSize,
        ApiSymbols.GPUSupportedLimits_maxTextureDimension2D,
        ApiSymbols.GPUSupportedLimits_maxColorAttachments,
        ApiSymbols.GPUSupportedLimits_maxComputeWorkgroupsPerDimension,
        ApiSymbols.GPURequiredLimits,
        ApiSymbols.GPUDeviceDescriptor_requiredLimits,
        ApiSymbols.GPUDevice_limits,
        ApiSymbols.GPUDevice_features,
        ApiSymbols.GPUDevice_adapterInfo,
        ApiSymbols.GPUFeatureName_CoreFeaturesAndLimits,
    ],
    input = AcidInput.Context,
)
suspend fun fullLimitsSpace(context: AcidContext) {
    val adapter = context.requestAdapter(null).getOrThrow()
    try {
        val requested = adapter.limits.asExplicitRequest()
        adapter.requestDevice(DeviceDescriptor(requiredLimits = requested)).getOrThrow().use { device ->
            assertTrue(
                GPUFeatureName.CoreFeaturesAndLimits in device.features,
                "A device granted without required features must still report the core feature set",
            )
            assertNotNull(device.adapterInfo, "A device must expose its adapter identity")

            val granted = device.limits
            assertEquals(requested.maxTextureDimension1D, granted.maxTextureDimension1D, "maxTextureDimension1D")
            assertEquals(requested.maxTextureDimension2D, granted.maxTextureDimension2D, "maxTextureDimension2D")
            assertEquals(requested.maxTextureDimension3D, granted.maxTextureDimension3D, "maxTextureDimension3D")
            assertEquals(requested.maxTextureArrayLayers, granted.maxTextureArrayLayers, "maxTextureArrayLayers")
            assertEquals(requested.maxBindGroups, granted.maxBindGroups, "maxBindGroups")
            assertEquals(requested.maxBindGroupsPlusVertexBuffers, granted.maxBindGroupsPlusVertexBuffers, "maxBindGroupsPlusVertexBuffers")
            assertEquals(requested.maxImmediateSize, granted.maxImmediateSize, "maxImmediateSize")
            assertEquals(requested.maxBindingsPerBindGroup, granted.maxBindingsPerBindGroup, "maxBindingsPerBindGroup")
            assertEquals(requested.maxDynamicUniformBuffersPerPipelineLayout, granted.maxDynamicUniformBuffersPerPipelineLayout, "maxDynamicUniformBuffersPerPipelineLayout")
            assertEquals(requested.maxDynamicStorageBuffersPerPipelineLayout, granted.maxDynamicStorageBuffersPerPipelineLayout, "maxDynamicStorageBuffersPerPipelineLayout")
            assertEquals(requested.maxSampledTexturesPerShaderStage, granted.maxSampledTexturesPerShaderStage, "maxSampledTexturesPerShaderStage")
            assertEquals(requested.maxSamplersPerShaderStage, granted.maxSamplersPerShaderStage, "maxSamplersPerShaderStage")
            assertEquals(requested.maxStorageBuffersPerShaderStage, granted.maxStorageBuffersPerShaderStage, "maxStorageBuffersPerShaderStage")
            assertEquals(requested.maxStorageBuffersInVertexStage, granted.maxStorageBuffersInVertexStage, "maxStorageBuffersInVertexStage")
            assertEquals(requested.maxStorageBuffersInFragmentStage, granted.maxStorageBuffersInFragmentStage, "maxStorageBuffersInFragmentStage")
            assertEquals(requested.maxStorageTexturesPerShaderStage, granted.maxStorageTexturesPerShaderStage, "maxStorageTexturesPerShaderStage")
            assertEquals(requested.maxStorageTexturesInVertexStage, granted.maxStorageTexturesInVertexStage, "maxStorageTexturesInVertexStage")
            assertEquals(requested.maxStorageTexturesInFragmentStage, granted.maxStorageTexturesInFragmentStage, "maxStorageTexturesInFragmentStage")
            assertEquals(requested.maxUniformBuffersPerShaderStage, granted.maxUniformBuffersPerShaderStage, "maxUniformBuffersPerShaderStage")
            assertEquals(requested.maxUniformBufferBindingSize, granted.maxUniformBufferBindingSize, "maxUniformBufferBindingSize")
            assertEquals(requested.maxStorageBufferBindingSize, granted.maxStorageBufferBindingSize, "maxStorageBufferBindingSize")
            assertEquals(requested.minUniformBufferOffsetAlignment, granted.minUniformBufferOffsetAlignment, "minUniformBufferOffsetAlignment")
            assertEquals(requested.minStorageBufferOffsetAlignment, granted.minStorageBufferOffsetAlignment, "minStorageBufferOffsetAlignment")
            assertEquals(requested.maxVertexBuffers, granted.maxVertexBuffers, "maxVertexBuffers")
            assertEquals(requested.maxBufferSize, granted.maxBufferSize, "maxBufferSize")
            assertEquals(requested.maxVertexAttributes, granted.maxVertexAttributes, "maxVertexAttributes")
            assertEquals(requested.maxVertexBufferArrayStride, granted.maxVertexBufferArrayStride, "maxVertexBufferArrayStride")
            assertEquals(requested.maxInterStageShaderVariables, granted.maxInterStageShaderVariables, "maxInterStageShaderVariables")
            assertEquals(requested.maxColorAttachments, granted.maxColorAttachments, "maxColorAttachments")
            assertEquals(requested.maxColorAttachmentBytesPerSample, granted.maxColorAttachmentBytesPerSample, "maxColorAttachmentBytesPerSample")
            assertEquals(requested.maxComputeWorkgroupStorageSize, granted.maxComputeWorkgroupStorageSize, "maxComputeWorkgroupStorageSize")
            assertEquals(requested.maxComputeInvocationsPerWorkgroup, granted.maxComputeInvocationsPerWorkgroup, "maxComputeInvocationsPerWorkgroup")
            assertEquals(requested.maxComputeWorkgroupSizeX, granted.maxComputeWorkgroupSizeX, "maxComputeWorkgroupSizeX")
            assertEquals(requested.maxComputeWorkgroupSizeY, granted.maxComputeWorkgroupSizeY, "maxComputeWorkgroupSizeY")
            assertEquals(requested.maxComputeWorkgroupSizeZ, granted.maxComputeWorkgroupSizeZ, "maxComputeWorkgroupSizeZ")
            assertEquals(requested.maxComputeWorkgroupsPerDimension, granted.maxComputeWorkgroupsPerDimension, "maxComputeWorkgroupsPerDimension")
        }
    } finally {
        adapter.close()
    }
}

/** Requests every limit of the contract explicitly, at the adapter's own value for each. */
private fun GPUSupportedLimits.asExplicitRequest(): RequiredLimits = RequiredLimits(
    maxTextureDimension1D = maxTextureDimension1D,
    maxTextureDimension2D = maxTextureDimension2D,
    maxTextureDimension3D = maxTextureDimension3D,
    maxTextureArrayLayers = maxTextureArrayLayers,
    maxBindGroups = maxBindGroups,
    maxBindGroupsPlusVertexBuffers = maxBindGroupsPlusVertexBuffers,
    maxImmediateSize = maxImmediateSize,
    maxBindingsPerBindGroup = maxBindingsPerBindGroup,
    maxDynamicUniformBuffersPerPipelineLayout = maxDynamicUniformBuffersPerPipelineLayout,
    maxDynamicStorageBuffersPerPipelineLayout = maxDynamicStorageBuffersPerPipelineLayout,
    maxSampledTexturesPerShaderStage = maxSampledTexturesPerShaderStage,
    maxSamplersPerShaderStage = maxSamplersPerShaderStage,
    maxStorageBuffersPerShaderStage = maxStorageBuffersPerShaderStage,
    maxStorageBuffersInVertexStage = maxStorageBuffersInVertexStage,
    maxStorageBuffersInFragmentStage = maxStorageBuffersInFragmentStage,
    maxStorageTexturesPerShaderStage = maxStorageTexturesPerShaderStage,
    maxStorageTexturesInVertexStage = maxStorageTexturesInVertexStage,
    maxStorageTexturesInFragmentStage = maxStorageTexturesInFragmentStage,
    maxUniformBuffersPerShaderStage = maxUniformBuffersPerShaderStage,
    maxUniformBufferBindingSize = maxUniformBufferBindingSize,
    maxStorageBufferBindingSize = maxStorageBufferBindingSize,
    minUniformBufferOffsetAlignment = minUniformBufferOffsetAlignment,
    minStorageBufferOffsetAlignment = minStorageBufferOffsetAlignment,
    maxVertexBuffers = maxVertexBuffers,
    maxBufferSize = maxBufferSize,
    maxVertexAttributes = maxVertexAttributes,
    maxVertexBufferArrayStride = maxVertexBufferArrayStride,
    maxInterStageShaderVariables = maxInterStageShaderVariables,
    maxColorAttachments = maxColorAttachments,
    maxColorAttachmentBytesPerSample = maxColorAttachmentBytesPerSample,
    maxComputeWorkgroupStorageSize = maxComputeWorkgroupStorageSize,
    maxComputeInvocationsPerWorkgroup = maxComputeInvocationsPerWorkgroup,
    maxComputeWorkgroupSizeX = maxComputeWorkgroupSizeX,
    maxComputeWorkgroupSizeY = maxComputeWorkgroupSizeY,
    maxComputeWorkgroupSizeZ = maxComputeWorkgroupSizeZ,
    maxComputeWorkgroupsPerDimension = maxComputeWorkgroupsPerDimension,
)
