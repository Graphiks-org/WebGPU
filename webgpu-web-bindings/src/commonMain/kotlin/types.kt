@file:Suppress("unused")
@file:OptIn(ExperimentalWasmJsInterop::class)
// This file has been generated DO NO EDIT
package org.graphiks.webgpu.bindings

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.JsArray
import js.promise.Promise
import js.collections.JsSet

typealias WGPUSupportedFeatures = JsSet<JsAny /* DOMString */>
external interface WGPUObjectBase : JsAny {
	var label: String /* USVString */
}

external interface WGPUSupportedLimits : JsAny {
	var maxTextureDimension1D: JsNumber /* unsigned long */
	var maxTextureDimension2D: JsNumber /* unsigned long */
	var maxTextureDimension3D: JsNumber /* unsigned long */
	var maxTextureArrayLayers: JsNumber /* unsigned long */
	var maxBindGroups: JsNumber /* unsigned long */
	var maxBindGroupsPlusVertexBuffers: JsNumber /* unsigned long */
	var maxImmediateSize: JsNumber /* unsigned long */
	var maxBindingsPerBindGroup: JsNumber /* unsigned long */
	var maxDynamicUniformBuffersPerPipelineLayout: JsNumber /* unsigned long */
	var maxDynamicStorageBuffersPerPipelineLayout: JsNumber /* unsigned long */
	var maxSampledTexturesPerShaderStage: JsNumber /* unsigned long */
	var maxSamplersPerShaderStage: JsNumber /* unsigned long */
	var maxStorageBuffersPerShaderStage: JsNumber /* unsigned long */
	var maxStorageBuffersInVertexStage: JsNumber /* unsigned long */
	var maxStorageBuffersInFragmentStage: JsNumber /* unsigned long */
	var maxStorageTexturesPerShaderStage: JsNumber /* unsigned long */
	var maxStorageTexturesInVertexStage: JsNumber /* unsigned long */
	var maxStorageTexturesInFragmentStage: JsNumber /* unsigned long */
	var maxUniformBuffersPerShaderStage: JsNumber /* unsigned long */
	var maxUniformBufferBindingSize: JsNumber /* unsigned long long */
	var maxStorageBufferBindingSize: JsNumber /* unsigned long long */
	var minUniformBufferOffsetAlignment: JsNumber /* unsigned long */
	var minStorageBufferOffsetAlignment: JsNumber /* unsigned long */
	var maxVertexBuffers: JsNumber /* unsigned long */
	var maxBufferSize: JsNumber /* unsigned long long */
	var maxVertexAttributes: JsNumber /* unsigned long */
	var maxVertexBufferArrayStride: JsNumber /* unsigned long */
	var maxInterStageShaderVariables: JsNumber /* unsigned long */
	var maxColorAttachments: JsNumber /* unsigned long */
	var maxColorAttachmentBytesPerSample: JsNumber /* unsigned long */
	var maxComputeWorkgroupStorageSize: JsNumber /* unsigned long */
	var maxComputeInvocationsPerWorkgroup: JsNumber /* unsigned long */
	var maxComputeWorkgroupSizeX: JsNumber /* unsigned long */
	var maxComputeWorkgroupSizeY: JsNumber /* unsigned long */
	var maxComputeWorkgroupSizeZ: JsNumber /* unsigned long */
	var maxComputeWorkgroupsPerDimension: JsNumber /* unsigned long */
}

external interface WGPUAdapterInfo : JsAny {
	var vendor: String /* DOMString */
	var architecture: String /* DOMString */
	var device: String /* DOMString */
	var description: String /* DOMString */
	var subgroupMinSize: JsNumber /* unsigned long */
	var subgroupMaxSize: JsNumber /* unsigned long */
	var isFallbackAdapter: Boolean
}

external interface WGPU : JsAny {
	var wgslLanguageFeatures: JsAny /* WGSLLanguageFeatures */
	fun requestAdapter(): Promise<JsAny?> /* Promise */
	fun requestAdapter(options: WGPURequestAdapterOptions  /* GPURequestAdapterOptions */): Promise<JsAny?> /* Promise */
	fun getPreferredCanvasFormat(): String  /* GPUTextureFormat */
}

external interface WGPUAdapter : JsAny {
	var features: WGPUSupportedFeatures  /* GPUSupportedFeatures */
	var limits: WGPUSupportedLimits  /* GPUSupportedLimits */
	var info: WGPUAdapterInfo  /* GPUAdapterInfo */
	fun requestDevice(): Promise<JsAny> /* Promise */
	fun requestDevice(descriptor: WGPUDeviceDescriptor  /* GPUDeviceDescriptor */): Promise<JsAny> /* Promise */
}

external interface WGPUDevice : JsAny, EventTarget, WGPUObjectBase {
	var features: WGPUSupportedFeatures  /* GPUSupportedFeatures */
	var limits: WGPUSupportedLimits  /* GPUSupportedLimits */
	var adapterInfo: WGPUAdapterInfo  /* GPUAdapterInfo */
	var queue: WGPUQueue  /* GPUQueue */
	var lost: Promise<JsAny> /* Promise */
	var onuncapturederror: JsAny /* EventHandler */
	fun destroy()
	fun createBuffer(descriptor: WGPUBufferDescriptor  /* GPUBufferDescriptor */): WGPUBuffer  /* GPUBuffer */
	fun createTexture(descriptor: WGPUTextureDescriptor  /* GPUTextureDescriptor */): WGPUTexture  /* GPUTexture */
	fun createSampler(): WGPUSampler  /* GPUSampler */
	fun createSampler(descriptor: WGPUSamplerDescriptor  /* GPUSamplerDescriptor */): WGPUSampler  /* GPUSampler */
	fun importExternalTexture(descriptor: WGPUExternalTextureDescriptor  /* GPUExternalTextureDescriptor */): WGPUExternalTexture  /* GPUExternalTexture */
	fun createBindGroupLayout(descriptor: WGPUBindGroupLayoutDescriptor  /* GPUBindGroupLayoutDescriptor */): WGPUBindGroupLayout  /* GPUBindGroupLayout */
	fun createPipelineLayout(descriptor: WGPUPipelineLayoutDescriptor  /* GPUPipelineLayoutDescriptor */): WGPUPipelineLayout  /* GPUPipelineLayout */
	fun createBindGroup(descriptor: WGPUBindGroupDescriptor  /* GPUBindGroupDescriptor */): WGPUBindGroup  /* GPUBindGroup */
	fun createShaderModule(descriptor: WGPUShaderModuleDescriptor  /* GPUShaderModuleDescriptor */): WGPUShaderModule  /* GPUShaderModule */
	fun createComputePipeline(descriptor: WGPUComputePipelineDescriptor  /* GPUComputePipelineDescriptor */): WGPUComputePipeline  /* GPUComputePipeline */
	fun createRenderPipeline(descriptor: WGPURenderPipelineDescriptor  /* GPURenderPipelineDescriptor */): WGPURenderPipeline  /* GPURenderPipeline */
	fun createComputePipelineAsync(descriptor: WGPUComputePipelineDescriptor  /* GPUComputePipelineDescriptor */): Promise<JsAny> /* Promise */
	fun createRenderPipelineAsync(descriptor: WGPURenderPipelineDescriptor  /* GPURenderPipelineDescriptor */): Promise<JsAny> /* Promise */
	fun createCommandEncoder(): WGPUCommandEncoder  /* GPUCommandEncoder */
	fun createCommandEncoder(descriptor: WGPUCommandEncoderDescriptor  /* GPUCommandEncoderDescriptor */): WGPUCommandEncoder  /* GPUCommandEncoder */
	fun createRenderBundleEncoder(descriptor: WGPURenderBundleEncoderDescriptor  /* GPURenderBundleEncoderDescriptor */): WGPURenderBundleEncoder  /* GPURenderBundleEncoder */
	fun createQuerySet(descriptor: WGPUQuerySetDescriptor  /* GPUQuerySetDescriptor */): WGPUQuerySet  /* GPUQuerySet */
	fun pushErrorScope(filter: String  /* GPUErrorFilter */)
	fun popErrorScope(): Promise<JsAny?> /* Promise */
}

external interface WGPUBuffer : JsAny, WGPUObjectBase {
	var size: JsNumber  /* GPUSize64Out */
	var usage: JsNumber  /* GPUFlagsConstant */
	var mapState: String  /* GPUBufferMapState */
	fun mapAsync(mode: JsNumber  /* GPUMapModeFlags */): Promise<JsAny> /* Promise */
	fun mapAsync(mode: JsNumber  /* GPUMapModeFlags */, offset: JsNumber  /* GPUSize64 */): Promise<JsAny> /* Promise */
	fun mapAsync(mode: JsNumber  /* GPUMapModeFlags */, offset: JsNumber  /* GPUSize64 */, size: JsNumber  /* GPUSize64 */): Promise<JsAny> /* Promise */
	fun getMappedRange(): js.buffer.ArrayBuffer
	fun getMappedRange(offset: JsNumber  /* GPUSize64 */): js.buffer.ArrayBuffer
	fun getMappedRange(offset: JsNumber  /* GPUSize64 */, size: JsNumber  /* GPUSize64 */): js.buffer.ArrayBuffer
	fun unmap()
	fun destroy()
}

external interface WGPUTexture : JsAny, WGPUObjectBase {
	var width: JsNumber  /* GPUIntegerCoordinateOut */
	var height: JsNumber  /* GPUIntegerCoordinateOut */
	var depthOrArrayLayers: JsNumber  /* GPUIntegerCoordinateOut */
	var mipLevelCount: JsNumber  /* GPUIntegerCoordinateOut */
	var sampleCount: JsNumber  /* GPUSize32Out */
	var dimension: String  /* GPUTextureDimension */
	var format: String  /* GPUTextureFormat */
	var usage: JsNumber  /* GPUFlagsConstant */
	var textureBindingViewDimension: JsAny /* (GPUTextureViewDimension or undefined) */
	fun createView(): WGPUTextureView  /* GPUTextureView */
	fun createView(descriptor: WGPUTextureViewDescriptor  /* GPUTextureViewDescriptor */): WGPUTextureView  /* GPUTextureView */
	fun destroy()
}

external interface WGPUTextureView : JsAny, WGPUObjectBase
external interface WGPUExternalTexture : JsAny, WGPUObjectBase
external interface WGPUSampler : JsAny, WGPUObjectBase
external interface WGPUBindGroupLayout : JsAny, WGPUObjectBase
external interface WGPUBindGroup : JsAny, WGPUObjectBase
external interface WGPUPipelineLayout : JsAny, WGPUObjectBase
external interface WGPUShaderModule : JsAny, WGPUObjectBase {
	fun getCompilationInfo(): Promise<JsAny> /* Promise */
}

external interface WGPUCompilationMessage : JsAny {
	var message: String /* DOMString */
	var type: String  /* GPUCompilationMessageType */
	var lineNum: JsNumber /* unsigned long long */
	var linePos: JsNumber /* unsigned long long */
	var offset: JsNumber /* unsigned long long */
	var length: JsNumber /* unsigned long long */
}

external interface WGPUCompilationInfo : JsAny {
	var messages: JsArray<JsAny> /* FrozenArray<GPUCompilationMessage> */
}

external interface WGPUPipelineError : JsAny, DOMException {
	var reason: JsAny /* GPUPipelineErrorReason */
}

external interface WGPUPipelineBase : JsAny {
	fun getBindGroupLayout(index: JsNumber /* unsigned long */): WGPUBindGroupLayout  /* GPUBindGroupLayout */
}

external interface WGPUComputePipeline : JsAny, WGPUObjectBase, WGPUPipelineBase
external interface WGPURenderPipeline : JsAny, WGPUObjectBase, WGPUPipelineBase
external interface WGPUCommandBuffer : JsAny, WGPUObjectBase
external interface WGPUCommandsMixin : JsAny
external interface WGPUCommandEncoder : JsAny, WGPUObjectBase, WGPUCommandsMixin, WGPUDebugCommandsMixin {
	fun beginRenderPass(descriptor: WGPURenderPassDescriptor  /* GPURenderPassDescriptor */): WGPURenderPassEncoder  /* GPURenderPassEncoder */
	fun beginComputePass(): WGPUComputePassEncoder  /* GPUComputePassEncoder */
	fun beginComputePass(descriptor: WGPUComputePassDescriptor  /* GPUComputePassDescriptor */): WGPUComputePassEncoder  /* GPUComputePassEncoder */
	fun copyBufferToBuffer(source: WGPUBuffer  /* GPUBuffer */, destination: WGPUBuffer  /* GPUBuffer */)
	fun copyBufferToBuffer(source: WGPUBuffer  /* GPUBuffer */, destination: WGPUBuffer  /* GPUBuffer */, size: JsNumber  /* GPUSize64 */)
	fun copyBufferToBuffer(source: WGPUBuffer  /* GPUBuffer */, sourceOffset: JsNumber  /* GPUSize64 */, destination: WGPUBuffer  /* GPUBuffer */, destinationOffset: JsNumber  /* GPUSize64 */)
	fun copyBufferToBuffer(source: WGPUBuffer  /* GPUBuffer */, sourceOffset: JsNumber  /* GPUSize64 */, destination: WGPUBuffer  /* GPUBuffer */, destinationOffset: JsNumber  /* GPUSize64 */, size: JsNumber  /* GPUSize64 */)
	fun copyBufferToTexture(source: WGPUTexelCopyBufferInfo  /* GPUTexelCopyBufferInfo */, destination: WGPUTexelCopyTextureInfo  /* GPUTexelCopyTextureInfo */, copySize: WGPUExtent3D  /* GPUExtent3D */)
	fun copyTextureToBuffer(source: WGPUTexelCopyTextureInfo  /* GPUTexelCopyTextureInfo */, destination: WGPUTexelCopyBufferInfo  /* GPUTexelCopyBufferInfo */, copySize: WGPUExtent3D  /* GPUExtent3D */)
	fun copyTextureToTexture(source: WGPUTexelCopyTextureInfo  /* GPUTexelCopyTextureInfo */, destination: WGPUTexelCopyTextureInfo  /* GPUTexelCopyTextureInfo */, copySize: WGPUExtent3D  /* GPUExtent3D */)
	fun clearBuffer(buffer: WGPUBuffer  /* GPUBuffer */)
	fun clearBuffer(buffer: WGPUBuffer  /* GPUBuffer */, offset: JsNumber  /* GPUSize64 */)
	fun clearBuffer(buffer: WGPUBuffer  /* GPUBuffer */, offset: JsNumber  /* GPUSize64 */, size: JsNumber  /* GPUSize64 */)
	fun resolveQuerySet(querySet: WGPUQuerySet  /* GPUQuerySet */, firstQuery: JsNumber  /* GPUSize32 */, queryCount: JsNumber  /* GPUSize32 */, destination: WGPUBuffer  /* GPUBuffer */, destinationOffset: JsNumber  /* GPUSize64 */)
	fun finish(): WGPUCommandBuffer  /* GPUCommandBuffer */
	fun finish(descriptor: WGPUCommandBufferDescriptor  /* GPUCommandBufferDescriptor */): WGPUCommandBuffer  /* GPUCommandBuffer */
}

external interface WGPUBindingCommandsMixin : JsAny {
	fun setBindGroup(index: JsNumber  /* GPUIndex32 */, bindGroup: WGPUBindGroup?  /* GPUBindGroup? */)
	fun setBindGroup(index: JsNumber  /* GPUIndex32 */, bindGroup: WGPUBindGroup?  /* GPUBindGroup? */, dynamicOffsets: JsArray<JsAny> /* sequence<GPUBufferDynamicOffset> */)
	fun setBindGroup(index: JsNumber  /* GPUIndex32 */, bindGroup: WGPUBindGroup?  /* GPUBindGroup? */, dynamicOffsetsData: JsAny /* Uint32Array */, dynamicOffsetsDataStart: JsNumber  /* GPUSize64 */, dynamicOffsetsDataLength: JsNumber  /* GPUSize32 */)
	fun setImmediates(rangeOffset: JsNumber  /* GPUSize32 */, data: js.buffer.ArrayBuffer /* AllowSharedBufferSource */)
	fun setImmediates(rangeOffset: JsNumber  /* GPUSize32 */, data: js.buffer.ArrayBuffer /* AllowSharedBufferSource */, dataOffset: JsNumber  /* GPUSize64 */)
	fun setImmediates(rangeOffset: JsNumber  /* GPUSize32 */, data: js.buffer.ArrayBuffer /* AllowSharedBufferSource */, dataOffset: JsNumber  /* GPUSize64 */, dataSize: JsNumber  /* GPUSize64 */)
}

external interface WGPUDebugCommandsMixin : JsAny {
	fun pushDebugGroup(groupLabel: String /* USVString */)
	fun popDebugGroup()
	fun insertDebugMarker(markerLabel: String /* USVString */)
}

external interface WGPUComputePassEncoder : JsAny, WGPUObjectBase, WGPUCommandsMixin, WGPUDebugCommandsMixin, WGPUBindingCommandsMixin {
	fun setPipeline(pipeline: WGPUComputePipeline  /* GPUComputePipeline */)
	fun dispatchWorkgroups(workgroupCountX: JsNumber  /* GPUSize32 */)
	fun dispatchWorkgroups(workgroupCountX: JsNumber  /* GPUSize32 */, workgroupCountY: JsNumber  /* GPUSize32 */)
	fun dispatchWorkgroups(workgroupCountX: JsNumber  /* GPUSize32 */, workgroupCountY: JsNumber  /* GPUSize32 */, workgroupCountZ: JsNumber  /* GPUSize32 */)
	fun dispatchWorkgroupsIndirect(indirectBuffer: WGPUBuffer  /* GPUBuffer */, indirectOffset: JsNumber  /* GPUSize64 */)
	fun end()
}

external interface WGPURenderPassEncoder : JsAny, WGPUObjectBase, WGPUCommandsMixin, WGPUDebugCommandsMixin, WGPUBindingCommandsMixin, WGPURenderCommandsMixin {
	fun setViewport(x: JsNumber /* float */, y: JsNumber /* float */, width: JsNumber /* float */, height: JsNumber /* float */, minDepth: JsNumber /* float */, maxDepth: JsNumber /* float */)
	fun setScissorRect(x: JsNumber  /* GPUIntegerCoordinate */, y: JsNumber  /* GPUIntegerCoordinate */, width: JsNumber  /* GPUIntegerCoordinate */, height: JsNumber  /* GPUIntegerCoordinate */)
	fun setBlendConstant(color: WGPUColor  /* GPUColor */)
	fun setStencilReference(reference: JsNumber  /* GPUStencilValue */)
	fun beginOcclusionQuery(queryIndex: JsNumber  /* GPUSize32 */)
	fun endOcclusionQuery()
	fun executeBundles(bundles: JsArray<JsAny> /* sequence<GPURenderBundle> */)
	fun end()
}

external interface WGPURenderCommandsMixin : JsAny {
	fun setPipeline(pipeline: WGPURenderPipeline  /* GPURenderPipeline */)
	fun setIndexBuffer(buffer: WGPUBuffer  /* GPUBuffer */, indexFormat: String  /* GPUIndexFormat */)
	fun setIndexBuffer(buffer: WGPUBuffer  /* GPUBuffer */, indexFormat: String  /* GPUIndexFormat */, offset: JsNumber  /* GPUSize64 */)
	fun setIndexBuffer(buffer: WGPUBuffer  /* GPUBuffer */, indexFormat: String  /* GPUIndexFormat */, offset: JsNumber  /* GPUSize64 */, size: JsNumber  /* GPUSize64 */)
	fun setVertexBuffer(slot: JsNumber  /* GPUIndex32 */, buffer: WGPUBuffer?  /* GPUBuffer? */)
	fun setVertexBuffer(slot: JsNumber  /* GPUIndex32 */, buffer: WGPUBuffer?  /* GPUBuffer? */, offset: JsNumber  /* GPUSize64 */)
	fun setVertexBuffer(slot: JsNumber  /* GPUIndex32 */, buffer: WGPUBuffer?  /* GPUBuffer? */, offset: JsNumber  /* GPUSize64 */, size: JsNumber  /* GPUSize64 */)
	fun draw(vertexCount: JsNumber  /* GPUSize32 */)
	fun draw(vertexCount: JsNumber  /* GPUSize32 */, instanceCount: JsNumber  /* GPUSize32 */)
	fun draw(vertexCount: JsNumber  /* GPUSize32 */, instanceCount: JsNumber  /* GPUSize32 */, firstVertex: JsNumber  /* GPUSize32 */)
	fun draw(vertexCount: JsNumber  /* GPUSize32 */, instanceCount: JsNumber  /* GPUSize32 */, firstVertex: JsNumber  /* GPUSize32 */, firstInstance: JsNumber  /* GPUSize32 */)
	fun drawIndexed(indexCount: JsNumber  /* GPUSize32 */)
	fun drawIndexed(indexCount: JsNumber  /* GPUSize32 */, instanceCount: JsNumber  /* GPUSize32 */)
	fun drawIndexed(indexCount: JsNumber  /* GPUSize32 */, instanceCount: JsNumber  /* GPUSize32 */, firstIndex: JsNumber  /* GPUSize32 */)
	fun drawIndexed(indexCount: JsNumber  /* GPUSize32 */, instanceCount: JsNumber  /* GPUSize32 */, firstIndex: JsNumber  /* GPUSize32 */, baseVertex: JsNumber  /* GPUSignedOffset32 */)
	fun drawIndexed(indexCount: JsNumber  /* GPUSize32 */, instanceCount: JsNumber  /* GPUSize32 */, firstIndex: JsNumber  /* GPUSize32 */, baseVertex: JsNumber  /* GPUSignedOffset32 */, firstInstance: JsNumber  /* GPUSize32 */)
	fun drawIndirect(indirectBuffer: WGPUBuffer  /* GPUBuffer */, indirectOffset: JsNumber  /* GPUSize64 */)
	fun drawIndexedIndirect(indirectBuffer: WGPUBuffer  /* GPUBuffer */, indirectOffset: JsNumber  /* GPUSize64 */)
}

external interface WGPURenderBundle : JsAny, WGPUObjectBase
external interface WGPURenderBundleEncoder : JsAny, WGPUObjectBase, WGPUCommandsMixin, WGPUDebugCommandsMixin, WGPUBindingCommandsMixin, WGPURenderCommandsMixin {
	fun finish(): WGPURenderBundle  /* GPURenderBundle */
	fun finish(descriptor: WGPURenderBundleDescriptor  /* GPURenderBundleDescriptor */): WGPURenderBundle  /* GPURenderBundle */
}

external interface WGPUQueue : JsAny, WGPUObjectBase {
	fun submit(commandBuffers: JsArray<JsAny> /* sequence<GPUCommandBuffer> */)
	fun onSubmittedWorkDone(): Promise<JsAny> /* Promise */
	fun writeBuffer(buffer: WGPUBuffer  /* GPUBuffer */, bufferOffset: JsNumber  /* GPUSize64 */, data: js.buffer.ArrayBuffer /* AllowSharedBufferSource */)
	fun writeBuffer(buffer: WGPUBuffer  /* GPUBuffer */, bufferOffset: JsNumber  /* GPUSize64 */, data: js.buffer.ArrayBuffer /* AllowSharedBufferSource */, dataOffset: JsNumber  /* GPUSize64 */)
	fun writeBuffer(buffer: WGPUBuffer  /* GPUBuffer */, bufferOffset: JsNumber  /* GPUSize64 */, data: js.buffer.ArrayBuffer /* AllowSharedBufferSource */, dataOffset: JsNumber  /* GPUSize64 */, size: JsNumber  /* GPUSize64 */)
	fun writeTexture(destination: WGPUTexelCopyTextureInfo  /* GPUTexelCopyTextureInfo */, data: js.buffer.ArrayBuffer /* AllowSharedBufferSource */, dataLayout: WGPUTexelCopyBufferLayout  /* GPUTexelCopyBufferLayout */, size: WGPUExtent3D  /* GPUExtent3D */)
	fun copyExternalImageToTexture(source: WGPUCopyExternalImageSourceInfo  /* GPUCopyExternalImageSourceInfo */, destination: WGPUCopyExternalImageDestInfo  /* GPUCopyExternalImageDestInfo */, copySize: WGPUExtent3D  /* GPUExtent3D */)
}

external interface WGPUQuerySet : JsAny, WGPUObjectBase {
	var type: String  /* GPUQueryType */
	var count: JsNumber  /* GPUSize32Out */
	fun destroy()
}

external interface WGPUCanvasContext : JsAny {
	var canvas: JsAny /* (HTMLCanvasElement or OffscreenCanvas) */
	fun configure(configuration: WGPUCanvasConfiguration  /* GPUCanvasConfiguration */)
	fun unconfigure()
	fun getConfiguration(): WGPUCanvasConfiguration?  /* GPUCanvasConfiguration? */
	fun getCurrentTexture(): WGPUTexture  /* GPUTexture */
}

external interface WGPUDeviceLostInfo : JsAny {
	var reason: String  /* GPUDeviceLostReason */
	var message: String /* DOMString */
}

external interface WGPUError : JsAny {
	var message: String /* DOMString */
}

external interface WGPUValidationError : JsAny, WGPUError
external interface WGPUOutOfMemoryError : JsAny, WGPUError
external interface WGPUInternalError : JsAny, WGPUError
external interface WGPUUncapturedErrorEvent : JsAny, Event {
	var error: WGPUError  /* GPUError */
}

external interface WGPUObjectDescriptorBase : JsAny {
	var label: String /* USVString */
}

external interface WGPURequestAdapterOptions : JsAny {
	var featureLevel: String /* DOMString */
	var powerPreference: String  /* GPUPowerPreference */
	var forceFallbackAdapter: Boolean
	var xrCompatible: Boolean
}

external interface WGPUDeviceDescriptor : JsAny, WGPUObjectDescriptorBase {
	var requiredFeatures: JsArray<JsAny> /* sequence<GPUFeatureName> */
	var requiredLimits: WebGpuRecord /* record<DOMString, (GPUSize64orundefined)>  */
	var defaultQueue: WGPUQueueDescriptor  /* GPUQueueDescriptor */
}

external interface WGPUBufferDescriptor : JsAny, WGPUObjectDescriptorBase {
	var size: JsNumber  /* GPUSize64 */
	var usage: JsNumber  /* GPUBufferUsageFlags */
	var mappedAtCreation: Boolean
}

external interface WGPUTextureDescriptor : JsAny, WGPUObjectDescriptorBase {
	var size: WGPUExtent3D  /* GPUExtent3D */
	var mipLevelCount: JsNumber  /* GPUIntegerCoordinate */
	var sampleCount: JsNumber  /* GPUSize32 */
	var dimension: String  /* GPUTextureDimension */
	var format: String  /* GPUTextureFormat */
	var usage: JsNumber  /* GPUTextureUsageFlags */
	var viewFormats: JsArray<JsAny> /* sequence<GPUTextureFormat> */
	var textureBindingViewDimension: String  /* GPUTextureViewDimension */
}

external interface WGPUTextureViewDescriptor : JsAny, WGPUObjectDescriptorBase {
	var format: String  /* GPUTextureFormat */
	var dimension: String  /* GPUTextureViewDimension */
	var usage: JsNumber  /* GPUTextureUsageFlags */
	var aspect: String  /* GPUTextureAspect */
	var baseMipLevel: JsNumber  /* GPUIntegerCoordinate */
	var mipLevelCount: JsNumber  /* GPUIntegerCoordinate */
	var baseArrayLayer: JsNumber  /* GPUIntegerCoordinate */
	var arrayLayerCount: JsNumber  /* GPUIntegerCoordinate */
	var swizzle: String /* DOMString */
}

external interface WGPUExternalTextureDescriptor : JsAny, WGPUObjectDescriptorBase {
	var source: JsAny /* (HTMLVideoElement or VideoFrame) */
	var colorSpace: JsAny /* PredefinedColorSpace */
}

external interface WGPUSamplerDescriptor : JsAny, WGPUObjectDescriptorBase {
	var addressModeU: String  /* GPUAddressMode */
	var addressModeV: String  /* GPUAddressMode */
	var addressModeW: String  /* GPUAddressMode */
	var magFilter: String  /* GPUFilterMode */
	var minFilter: String  /* GPUFilterMode */
	var mipmapFilter: String  /* GPUMipmapFilterMode */
	var lodMinClamp: JsNumber /* float */
	var lodMaxClamp: JsNumber /* float */
	var compare: String  /* GPUCompareFunction */
	var maxAnisotropy: JsNumber /* unsigned short */
}

external interface WGPUBindGroupLayoutDescriptor : JsAny, WGPUObjectDescriptorBase {
	var entries: JsArray<JsAny> /* sequence<GPUBindGroupLayoutEntry> */
}

external interface WGPUBindGroupLayoutEntry : JsAny {
	var binding: JsNumber  /* GPUIndex32 */
	var visibility: JsNumber  /* GPUShaderStageFlags */
	var buffer: WGPUBufferBindingLayout  /* GPUBufferBindingLayout */
	var sampler: WGPUSamplerBindingLayout  /* GPUSamplerBindingLayout */
	var texture: WGPUTextureBindingLayout  /* GPUTextureBindingLayout */
	var storageTexture: WGPUStorageTextureBindingLayout  /* GPUStorageTextureBindingLayout */
	var externalTexture: WGPUExternalTextureBindingLayout  /* GPUExternalTextureBindingLayout */
}

external interface WGPUBufferBindingLayout : JsAny {
	var type: String  /* GPUBufferBindingType */
	var hasDynamicOffset: Boolean
	var minBindingSize: JsNumber  /* GPUSize64 */
}

external interface WGPUSamplerBindingLayout : JsAny {
	var type: String  /* GPUSamplerBindingType */
}

external interface WGPUTextureBindingLayout : JsAny {
	var sampleType: String  /* GPUTextureSampleType */
	var viewDimension: String  /* GPUTextureViewDimension */
	var multisampled: Boolean
}

external interface WGPUStorageTextureBindingLayout : JsAny {
	var access: String  /* GPUStorageTextureAccess */
	var format: String  /* GPUTextureFormat */
	var viewDimension: String  /* GPUTextureViewDimension */
}

external interface WGPUExternalTextureBindingLayout : JsAny
external interface WGPUBindGroupDescriptor : JsAny, WGPUObjectDescriptorBase {
	var layout: WGPUBindGroupLayout  /* GPUBindGroupLayout */
	var entries: JsArray<JsAny> /* sequence<GPUBindGroupEntry> */
}

external interface WGPUBindGroupEntry : JsAny {
	var binding: JsNumber  /* GPUIndex32 */
	var resource: JsAny /* GPUBindingResource */
}

external interface WGPUBufferBinding : JsAny {
	var buffer: WGPUBuffer  /* GPUBuffer */
	var offset: JsNumber  /* GPUSize64 */
	var size: JsNumber  /* GPUSize64 */
}

external interface WGPUPipelineLayoutDescriptor : JsAny, WGPUObjectDescriptorBase {
	var bindGroupLayouts: JsArray<JsAny?> /* sequence<GPUBindGroupLayout?> */
	var immediateSize: JsNumber  /* GPUSize32 */
}

external interface WGPUShaderModuleDescriptor : JsAny, WGPUObjectDescriptorBase {
	var code: String /* USVString */
	var compilationHints: JsArray<JsAny> /* sequence<GPUShaderModuleCompilationHint> */
}

external interface WGPUShaderModuleCompilationHint : JsAny {
	var entryPoint: String /* USVString */
	var layout: JsAny /* (GPUPipelineLayout or GPUAutoLayoutMode) */
}

external interface WGPUPipelineErrorInit : JsAny {
	var reason: JsAny /* GPUPipelineErrorReason */
}

external interface WGPUPipelineDescriptorBase : JsAny, WGPUObjectDescriptorBase {
	var layout: JsAny /* (GPUPipelineLayout or GPUAutoLayoutMode) */
}

external interface WGPUProgrammableStage : JsAny {
	var module: WGPUShaderModule  /* GPUShaderModule */
	var entryPoint: String /* USVString */
	var constants: WebGpuRecord /* record<USVString, GPUPipelineConstantValue>  */
}

external interface WGPUComputePipelineDescriptor : JsAny, WGPUPipelineDescriptorBase {
	var compute: WGPUProgrammableStage  /* GPUProgrammableStage */
}

external interface WGPURenderPipelineDescriptor : JsAny, WGPUPipelineDescriptorBase {
	var vertex: WGPUVertexState  /* GPUVertexState */
	var primitive: WGPUPrimitiveState  /* GPUPrimitiveState */
	var depthStencil: WGPUDepthStencilState  /* GPUDepthStencilState */
	var multisample: WGPUMultisampleState  /* GPUMultisampleState */
	var fragment: WGPUFragmentState  /* GPUFragmentState */
}

external interface WGPUPrimitiveState : JsAny {
	var topology: String  /* GPUPrimitiveTopology */
	var stripIndexFormat: String  /* GPUIndexFormat */
	var frontFace: String  /* GPUFrontFace */
	var cullMode: String  /* GPUCullMode */
	var unclippedDepth: Boolean
}

external interface WGPUMultisampleState : JsAny {
	var count: JsNumber  /* GPUSize32 */
	var mask: JsNumber  /* GPUSampleMask */
	var alphaToCoverageEnabled: Boolean
}

external interface WGPUFragmentState : JsAny, WGPUProgrammableStage {
	var targets: JsArray<JsAny?> /* sequence<GPUColorTargetState?> */
}

external interface WGPUColorTargetState : JsAny {
	var format: String  /* GPUTextureFormat */
	var blend: WGPUBlendState  /* GPUBlendState */
	var writeMask: JsNumber  /* GPUColorWriteFlags */
}

external interface WGPUBlendState : JsAny {
	var color: WGPUBlendComponent  /* GPUBlendComponent */
	var alpha: WGPUBlendComponent  /* GPUBlendComponent */
}

external interface WGPUBlendComponent : JsAny {
	var operation: String  /* GPUBlendOperation */
	var srcFactor: String  /* GPUBlendFactor */
	var dstFactor: String  /* GPUBlendFactor */
}

external interface WGPUDepthStencilState : JsAny {
	var format: String  /* GPUTextureFormat */
	var depthWriteEnabled: Boolean
	var depthCompare: String  /* GPUCompareFunction */
	var stencilFront: WGPUStencilFaceState  /* GPUStencilFaceState */
	var stencilBack: WGPUStencilFaceState  /* GPUStencilFaceState */
	var stencilReadMask: JsNumber  /* GPUStencilValue */
	var stencilWriteMask: JsNumber  /* GPUStencilValue */
	var depthBias: JsNumber  /* GPUDepthBias */
	var depthBiasSlopeScale: JsNumber /* float */
	var depthBiasClamp: JsNumber /* float */
}

external interface WGPUStencilFaceState : JsAny {
	var compare: String  /* GPUCompareFunction */
	var failOp: String  /* GPUStencilOperation */
	var depthFailOp: String  /* GPUStencilOperation */
	var passOp: String  /* GPUStencilOperation */
}

external interface WGPUVertexState : JsAny, WGPUProgrammableStage {
	var buffers: JsArray<JsAny?> /* sequence<GPUVertexBufferLayout?> */
}

external interface WGPUVertexBufferLayout : JsAny {
	var arrayStride: JsNumber  /* GPUSize64 */
	var stepMode: String  /* GPUVertexStepMode */
	var attributes: JsArray<JsAny> /* sequence<GPUVertexAttribute> */
}

external interface WGPUVertexAttribute : JsAny {
	var format: String  /* GPUVertexFormat */
	var offset: JsNumber  /* GPUSize64 */
	var shaderLocation: JsNumber  /* GPUIndex32 */
}

external interface WGPUTexelCopyBufferLayout : JsAny {
	var offset: JsNumber  /* GPUSize64 */
	var bytesPerRow: JsNumber  /* GPUSize32 */
	var rowsPerImage: JsNumber  /* GPUSize32 */
}

external interface WGPUTexelCopyBufferInfo : JsAny, WGPUTexelCopyBufferLayout {
	var buffer: WGPUBuffer  /* GPUBuffer */
}

external interface WGPUTexelCopyTextureInfo : JsAny {
	var texture: WGPUTexture  /* GPUTexture */
	var mipLevel: JsNumber  /* GPUIntegerCoordinate */
	var origin: WGPUOrigin3D  /* GPUOrigin3D */
	var aspect: String  /* GPUTextureAspect */
}

external interface WGPUCopyExternalImageDestInfo : JsAny, WGPUTexelCopyTextureInfo {
	var colorSpace: JsAny /* PredefinedColorSpace */
	var premultipliedAlpha: Boolean
}

external interface WGPUCopyExternalImageSourceInfo : JsAny {
	var source: JsAny /* GPUCopyExternalImageSource */
	var origin: WGPUOrigin2D  /* GPUOrigin2D */
	var flipY: Boolean
}

external interface WGPUCommandBufferDescriptor : JsAny, WGPUObjectDescriptorBase
external interface WGPUCommandEncoderDescriptor : JsAny, WGPUObjectDescriptorBase
external interface WGPUComputePassTimestampWrites : JsAny {
	var querySet: WGPUQuerySet  /* GPUQuerySet */
	var beginningOfPassWriteIndex: JsNumber  /* GPUSize32 */
	var endOfPassWriteIndex: JsNumber  /* GPUSize32 */
}

external interface WGPUComputePassDescriptor : JsAny, WGPUObjectDescriptorBase {
	var timestampWrites: WGPUComputePassTimestampWrites  /* GPUComputePassTimestampWrites */
}

external interface WGPURenderPassTimestampWrites : JsAny {
	var querySet: WGPUQuerySet  /* GPUQuerySet */
	var beginningOfPassWriteIndex: JsNumber  /* GPUSize32 */
	var endOfPassWriteIndex: JsNumber  /* GPUSize32 */
}

external interface WGPURenderPassDescriptor : JsAny, WGPUObjectDescriptorBase {
	var colorAttachments: JsArray<JsAny?> /* sequence<GPURenderPassColorAttachment?> */
	var depthStencilAttachment: WGPURenderPassDepthStencilAttachment  /* GPURenderPassDepthStencilAttachment */
	var occlusionQuerySet: WGPUQuerySet  /* GPUQuerySet */
	var timestampWrites: WGPURenderPassTimestampWrites  /* GPURenderPassTimestampWrites */
	var maxDrawCount: JsNumber  /* GPUSize64 */
}

external interface WGPURenderPassColorAttachment : JsAny {
	var view: JsAny /* (GPUTexture or GPUTextureView) */
	var depthSlice: JsNumber  /* GPUIntegerCoordinate */
	var resolveTarget: JsAny /* (GPUTexture or GPUTextureView) */
	var clearValue: WGPUColor  /* GPUColor */
	var loadOp: String  /* GPULoadOp */
	var storeOp: String  /* GPUStoreOp */
}

external interface WGPURenderPassDepthStencilAttachment : JsAny {
	var view: JsAny /* (GPUTexture or GPUTextureView) */
	var depthClearValue: JsNumber /* float */
	var depthLoadOp: String  /* GPULoadOp */
	var depthStoreOp: String  /* GPUStoreOp */
	var depthReadOnly: Boolean
	var stencilClearValue: JsNumber  /* GPUStencilValue */
	var stencilLoadOp: String  /* GPULoadOp */
	var stencilStoreOp: String  /* GPUStoreOp */
	var stencilReadOnly: Boolean
}

external interface WGPURenderPassLayout : JsAny, WGPUObjectDescriptorBase {
	var colorFormats: JsArray<JsAny?> /* sequence<GPUTextureFormat?> */
	var depthStencilFormat: String  /* GPUTextureFormat */
	var sampleCount: JsNumber  /* GPUSize32 */
}

external interface WGPURenderBundleDescriptor : JsAny, WGPUObjectDescriptorBase
external interface WGPURenderBundleEncoderDescriptor : JsAny, WGPURenderPassLayout {
	var depthReadOnly: Boolean
	var stencilReadOnly: Boolean
}

external interface WGPUQueueDescriptor : JsAny, WGPUObjectDescriptorBase
external interface WGPUQuerySetDescriptor : JsAny, WGPUObjectDescriptorBase {
	var type: String  /* GPUQueryType */
	var count: JsNumber  /* GPUSize32 */
}

external interface WGPUCanvasToneMapping : JsAny {
	var mode: JsAny /* GPUCanvasToneMappingMode */
}

external interface WGPUCanvasConfiguration : JsAny {
	var device: WGPUDevice  /* GPUDevice */
	var format: String  /* GPUTextureFormat */
	var usage: JsNumber  /* GPUTextureUsageFlags */
	var viewFormats: JsArray<JsAny> /* sequence<GPUTextureFormat> */
	var colorSpace: JsAny /* PredefinedColorSpace */
	var toneMapping: WGPUCanvasToneMapping  /* GPUCanvasToneMapping */
	var alphaMode: JsAny /* GPUCanvasAlphaMode */
}

external interface WGPUUncapturedErrorEventInit : JsAny, EventInit {
	var error: WGPUError  /* GPUError */
}

external interface WGPUColor : JsAny {
	var r: JsNumber /* double */
	var g: JsNumber /* double */
	var b: JsNumber /* double */
	var a: JsNumber /* double */
}

external interface WGPUOrigin2D : JsAny {
	var x: JsNumber  /* GPUIntegerCoordinate */
	var y: JsNumber  /* GPUIntegerCoordinate */
}

external interface WGPUOrigin3D : JsAny {
	var x: JsNumber  /* GPUIntegerCoordinate */
	var y: JsNumber  /* GPUIntegerCoordinate */
	var z: JsNumber  /* GPUIntegerCoordinate */
}

external interface WGPUExtent3D : JsAny {
	var width: JsNumber  /* GPUIntegerCoordinate */
	var height: JsNumber  /* GPUIntegerCoordinate */
	var depthOrArrayLayers: JsNumber  /* GPUIntegerCoordinate */
}
