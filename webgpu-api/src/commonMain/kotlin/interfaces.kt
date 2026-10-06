@file:Suppress("unused")
// This file has been generated DO NO EDIT
package org.graphiks.webgpu

/**
 * A resource accepted by a bind group entry, such as a sampler, texture view, buffer binding, or external texture.
 *
 */
sealed interface GPUBindingResource
/**
 * A GPUBuffer represents a block of memory that can be used in GPU operations. Data is stored in linear layout, meaning that each byte of the allocation can be addressed by its offset from the start of the GPUBuffer, subject to alignment restrictions depending on the operation. Some GPUBuffers can be mapped which makes the block of memory accessible via an ArrayBuffer called its mapping.
 *
 * ## Lifetime
 *
 * A GPUBuffer created by a device is owned by the caller. `close()` destroys the buffer: it runs the WebGPU `destroy` operation, and a native backend must also release its owned reference. Closing an already closed buffer does not release the same reference a second time; general thread safety is not promised. Mapped ranges are borrowed: they are invalidated by `unmap()` and by destroying the buffer. Whether a backend detects an access to an invalidated range is not portable.
 *
 */
interface GPUBuffer : GPUBindingResource, GPUObjectBase, AutoCloseable {
	/**
	 * The length of the GPUBuffer allocation in bytes.
	 *
	 * See [GPUBuffer.size in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubuffer-size).
	 *
	 */
	val size: GPUSize64Out
	/**
	 * The allowed usages for this GPUBuffer.
	 *
	 * See [GPUBuffer.usage in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubuffer-usage).
	 *
	 */
	val usage: GPUBufferUsage
	/**
	 * The buffer is not mapped for use by this.getMappedRange(). A mapping of the buffer has been requested, but is pending. It may succeed, or fail validation in mapAsync().
	 *
	 */
	val mapState: GPUBufferMapState
	/**
	 * Maps the given range of the GPUBuffer and resolves the returned Promise when the GPUBuffer’s content is ready to be accessed with getMappedRange(). The resolution of the returned Promise only indicates that the buffer has been mapped. It does not guarantee the completion of any other operations visible to the content timeline, and in particular does not imply that any other Promise returned from onSubmittedWorkDone() or mapAsync() on other GPUBuffers have resolved.
	 *
	 * @param mode Whether the buffer should be mapped for reading or writing.
	 * @param offset Offset in bytes into the buffer to the start of the range to map.
	 * @param size Size in bytes of the range to map.
	 *
	 */
	suspend fun mapAsync(mode: GPUMapMode, offset: GPUSize64 = 0u, size: GPUSize64? = null): Result<Unit>
	/**
	 * Returns an ArrayBuffer with the contents of the GPUBuffer in the given mapped range.
	 *
	 * The returned range is borrowed, not owned: it is invalidated by `unmap()` and by destroying the buffer, and it must not be retained or used after either. Prefer `GPUBuffer.withMappedRange`, which maps, runs a non-suspending block with the borrowed range and unmaps in a `finally`.
	 *
	 * @param offset Offset in bytes into the buffer to return buffer contents from.
	 * @param size Size in bytes of the ArrayBuffer to return.
	 *
	 */
	fun getMappedRange(offset: GPUSize64 = 0u, size: GPUSize64? = null): ArrayBuffer
	/**
	 * Unmaps the mapped range of the GPUBuffer and makes its contents available for use by the GPU again. Every borrowed range previously returned by `getMappedRange` is invalidated by this call.
	 *
	 */
	fun unmap()
}

/**
 * A buffer resource and the byte range exposed to a shader through a bind group.
 *
 */
interface GPUBufferBinding : GPUBindingResource {
	/**
	 * The GPUBuffer to bind.
	 *
	 * See [GPUBufferBinding.buffer in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubufferbinding-buffer).
	 *
	 */
	val buffer: GPUBuffer
	/**
	 * The offset, in bytes, from the beginning of buffer to the beginning of the range exposed to the shader by the buffer binding.
	 *
	 */
	val offset: GPUSize64
	/**
	 * The size, in bytes, of the buffer binding. If not provided, specifies the range starting at offset and ending at the end of buffer.
	 *
	 */
	val size: GPUSize64?
}

/**
 * A GPUSampler encodes transformations and filtering information that can be used in a shader to interpret texture resource data.
 *
 */
interface GPUSampler : GPUBindingResource, GPUObjectBase, AutoCloseable
/**
 * A texture is made up of 1d, 2d, or 3d arrays of data which can contain multiple values per-element to represent things like colors. Textures can be read and written in many ways, depending on the GPUTextureUsage they are created with. For example, textures can be sampled, read, and written from render and compute pipeline shaders, and they can be written by render pass outputs. Internally, textures are often stored in GPU memory with a layout optimized for multidimensional access rather than linear access.
 *
 * ## Lifetime
 *
 * A GPUTexture created by a device is owned by the caller: `close()` destroys it, and a native backend must also release its owned reference. A texture obtained from a canvas context is borrowed: closing its wrapper does not destroy the texture owned by the canvas. Closing an already closed texture does not release the same reference a second time.
 *
 */
interface GPUTexture : GPUBindingResource, GPUObjectBase, GPUTextureOrGPUTextureView, AutoCloseable {
	/**
	 * The width of this GPUTexture.
	 *
	 * See [GPUTexture.width in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexture-width).
	 *
	 */
	val width: GPUIntegerCoordinateOut
	/**
	 * The height of this GPUTexture.
	 *
	 * See [GPUTexture.height in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexture-height).
	 *
	 */
	val height: GPUIntegerCoordinateOut
	/**
	 * The depth or layer count of this GPUTexture.
	 *
	 * See [GPUTexture.depthOrArrayLayers in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexture-depthorarraylayers).
	 *
	 */
	val depthOrArrayLayers: GPUIntegerCoordinateOut
	/**
	 * The number of mip levels of this GPUTexture.
	 *
	 * See [GPUTexture.mipLevelCount in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexture-miplevelcount).
	 *
	 */
	val mipLevelCount: GPUIntegerCoordinateOut
	/**
	 * The number of sample count of this GPUTexture.
	 *
	 * See [GPUTexture.sampleCount in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexture-samplecount).
	 *
	 */
	val sampleCount: GPUSize32Out
	/**
	 * The dimension of the set of texel for each of this GPUTexture’s subresources.
	 *
	 */
	val dimension: GPUTextureDimension
	/**
	 * The format of this GPUTexture.
	 *
	 * See [GPUTexture.format in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexture-format).
	 *
	 */
	val format: GPUTextureFormat
	/**
	 * The allowed usages for this GPUTexture.
	 *
	 * See [GPUTexture.usage in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexture-usage).
	 *
	 */
	val usage: GPUTextureUsage
	/**
	 * Creates a GPUTextureView.
	 *
	 * @param descriptor Description of the GPUTextureView to create.
	 *
	 */
	fun createView(descriptor: GPUTextureViewDescriptor? = null): GPUTextureView
}

/**
 * A GPUTextureView is a view onto some subset of the texture subresources defined by a particular GPUTexture.
 *
 */
interface GPUTextureView : GPUBindingResource, GPUObjectBase, GPUTextureOrGPUTextureView, AutoCloseable
/**
 * An RGBA color represented by four components.
 *
 * See [GPUColor in the WebGPU specification](https://www.w3.org/TR/webgpu/#typedefdef-gpucolor).
 *
 */
interface GPUColor {
	/**
	 * The red channel value.
	 *
	 * See [GPUColor.r in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolordict-r).
	 *
	 */
	val r: Double
	/**
	 * The green channel value.
	 *
	 * See [GPUColor.g in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolordict-g).
	 *
	 */
	val g: Double
	/**
	 * The blue channel value.
	 *
	 * See [GPUColor.b in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolordict-b).
	 *
	 */
	val b: Double
	/**
	 * The alpha channel value.
	 *
	 * See [GPUColor.a in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolordict-a).
	 *
	 */
	val a: Double
}

/**
 * A two-dimensional origin represented by x and y coordinates.
 *
 */
interface GPUOrigin2D {
	/**
	 * The X coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	val x: GPUIntegerCoordinate
	/**
	 * The Y coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	val y: GPUIntegerCoordinate
}

/**
 * A three-dimensional origin represented by x, y, and z coordinates.
 *
 */
interface GPUOrigin3D {
	/**
	 * The X coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	val x: GPUIntegerCoordinate
	/**
	 * The Y coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	val y: GPUIntegerCoordinate
	/**
	 * The Z coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	val z: GPUIntegerCoordinate
}

/**
 * A three-dimensional extent represented by width, height, and depth or array-layer count.
 *
 */
interface GPUExtent3D {
	/**
	 * The width of the extent.
	 *
	 * See [GPUExtent3D.width in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuextent3ddict-width).
	 *
	 */
	val width: GPUIntegerCoordinate
	/**
	 * The height of the extent.
	 *
	 * See [GPUExtent3D.height in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuextent3ddict-height).
	 *
	 */
	val height: GPUIntegerCoordinate
	/**
	 * The depth of the extent or the number of array layers it contains. If used with a GPUTexture with a GPUTextureDimension of "3d" defines the depth of the texture. If used with a GPUTexture with a GPUTextureDimension of "2d" defines the number of array layers in the texture.
	 *
	 */
	val depthOrArrayLayers: GPUIntegerCoordinate
}

/**
 * Common properties included by WebGPU objects. Its label can identify the object in diagnostics and developer tools.
 *
 */
interface GPUObjectBase {
	/**
	 * A developer-provided label which is used in an implementation-defined way. It can be used by the browser, OS, or other tools to help identify the underlying internal object to the developer. Examples include displaying the label in GPUError messages, console warnings, browser developer tools, and platform debugging utilities.
	 *
	 */
	var label: String
}

/**
 * GPUSupportedLimits exposes an adapter or device’s supported limits. See GPUAdapter.limits and GPUDevice.limits.
 *
 */
interface GPUSupportedLimits {
	/**
	 * The maximum allowed value for the size.width of a texture created with dimension "1d".
	 *
	 */
	val maxTextureDimension1D: UInt
	/**
	 * The maximum allowed value for the size.width and size.height of a texture created with dimension "2d".
	 *
	 */
	val maxTextureDimension2D: UInt
	/**
	 * The maximum allowed value for the size.width, size.height and size.depthOrArrayLayers of a texture created with dimension "3d".
	 *
	 */
	val maxTextureDimension3D: UInt
	/**
	 * The maximum allowed value for the size.depthOrArrayLayers of a texture created with dimension "2d".
	 *
	 */
	val maxTextureArrayLayers: UInt
	/**
	 * The maximum number of GPUBindGroupLayouts allowed in bindGroupLayouts when creating a GPUPipelineLayout.
	 *
	 */
	val maxBindGroups: UInt
	/**
	 * The maximum number of bind group and vertex buffer slots used simultaneously, counting any empty slots below the highest index. Validated in createRenderPipeline() and in draw calls.
	 *
	 */
	val maxBindGroupsPlusVertexBuffers: UInt
	/**
	 * The maximum size, in bytes, of immediate data range used in a pipeline. Note: 64 bytes is the size of a 4×4 matrix of f32 values.
	 *
	 */
	val maxImmediateSize: UInt
	/**
	 * The number of binding indices available when creating a GPUBindGroupLayout. Note: This limit is normative, but arbitrary. With the default binding slot limits, it is impossible to use 1000 bindings in one bind group, but this allows GPUBindGroupLayoutEntry.binding values up to 999. This limit allows implementations to treat binding space as an array, within reasonable memory space, rather than a sparse map structure.
	 *
	 */
	val maxBindingsPerBindGroup: UInt
	/**
	 * The maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are uniform buffers with dynamic offsets. See Exceeds the binding slot limits.
	 *
	 */
	val maxDynamicUniformBuffersPerPipelineLayout: UInt
	/**
	 * The maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are storage buffers with dynamic offsets. See Exceeds the binding slot limits.
	 *
	 */
	val maxDynamicStorageBuffersPerPipelineLayout: UInt
	/**
	 * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are sampled textures. See Exceeds the binding slot limits.
	 *
	 */
	val maxSampledTexturesPerShaderStage: UInt
	/**
	 * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are samplers. See Exceeds the binding slot limits.
	 *
	 */
	val maxSamplersPerShaderStage: UInt
	/**
	 * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are storage buffers. See Exceeds the binding slot limits. Note: This limit applies to all stages. At device initialization, it is normalized with maxStorageBuffersInVertexStage and maxStorageBuffersInFragmentStage so that in the validation algorithm, each stage can be checked against just one of the three limits.
	 *
	 */
	val maxStorageBuffersPerShaderStage: UInt
	/**
	 * For the vertex stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are storage buffers. See Exceeds the binding slot limits.
	 *
	 */
	val maxStorageBuffersInVertexStage: UInt
	/**
	 * For the fragment stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are storage buffers. See Exceeds the binding slot limits.
	 *
	 */
	val maxStorageBuffersInFragmentStage: UInt
	/**
	 * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are storage textures. See Exceeds the binding slot limits. Note: This limit applies to all stages. At device initialization, it is normalized with maxStorageTexturesInVertexStage and maxStorageTexturesInFragmentStage so that in the validation algorithm, each stage can be checked against just one of the three limits.
	 *
	 */
	val maxStorageTexturesPerShaderStage: UInt
	/**
	 * For the vertex stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are storage textures. See Exceeds the binding slot limits.
	 *
	 */
	val maxStorageTexturesInVertexStage: UInt
	/**
	 * For the fragment stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are storage textures. See Exceeds the binding slot limits.
	 *
	 */
	val maxStorageTexturesInFragmentStage: UInt
	/**
	 * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries across a GPUPipelineLayout which are uniform buffers. See Exceeds the binding slot limits.
	 *
	 */
	val maxUniformBuffersPerShaderStage: UInt
	/**
	 * The maximum GPUBufferBinding.size for bindings with a GPUBindGroupLayoutEntry entry for which entry.buffer?.type is "uniform".
	 *
	 */
	val maxUniformBufferBindingSize: ULong
	/**
	 * The maximum GPUBufferBinding.size for bindings with a GPUBindGroupLayoutEntry entry for which entry.buffer?.type is "storage" or "read-only-storage".
	 *
	 */
	val maxStorageBufferBindingSize: ULong
	/**
	 * The required alignment for GPUBufferBinding.offset and the dynamic offsets provided in setBindGroup(), for bindings with a GPUBindGroupLayoutEntry entry for which entry.buffer?.type is "uniform".
	 *
	 */
	val minUniformBufferOffsetAlignment: UInt
	/**
	 * The required alignment for GPUBufferBinding.offset and the dynamic offsets provided in setBindGroup(), for bindings with a GPUBindGroupLayoutEntry entry for which entry.buffer?.type is "storage" or "read-only-storage".
	 *
	 */
	val minStorageBufferOffsetAlignment: UInt
	/**
	 * The maximum number of buffers when creating a GPURenderPipeline.
	 *
	 */
	val maxVertexBuffers: UInt
	/**
	 * The maximum size of size when creating a GPUBuffer.
	 *
	 */
	val maxBufferSize: ULong
	/**
	 * The maximum number of attributes in total across buffers when creating a GPURenderPipeline.
	 *
	 */
	val maxVertexAttributes: UInt
	/**
	 * The maximum allowed arrayStride when creating a GPURenderPipeline.
	 *
	 */
	val maxVertexBufferArrayStride: UInt
	/**
	 * The maximum allowed number of input or output variables for inter-stage communication (like vertex outputs or fragment inputs).
	 *
	 */
	val maxInterStageShaderVariables: UInt
	/**
	 * The maximum allowed number of color attachments in GPURenderPipelineDescriptor.fragment.targets, GPURenderPassDescriptor.colorAttachments, and GPURenderPassLayout.colorFormats.
	 *
	 */
	val maxColorAttachments: UInt
	/**
	 * The maximum number of bytes necessary to hold one sample (pixel or subpixel) of render pipeline output data, across all color attachments.
	 *
	 */
	val maxColorAttachmentBytesPerSample: UInt
	/**
	 * The maximum number of bytes of workgroup storage used for a compute stage GPUShaderModule entry-point.
	 *
	 */
	val maxComputeWorkgroupStorageSize: UInt
	/**
	 * The maximum value of the product of the workgroup_size dimensions for a compute stage GPUShaderModule entry-point.
	 *
	 */
	val maxComputeInvocationsPerWorkgroup: UInt
	/**
	 * The maximum value of the workgroup_size X dimension for a compute stage GPUShaderModule entry-point.
	 *
	 */
	val maxComputeWorkgroupSizeX: UInt
	/**
	 * The maximum value of the workgroup_size Y dimensions for a compute stage GPUShaderModule entry-point.
	 *
	 */
	val maxComputeWorkgroupSizeY: UInt
	/**
	 * The maximum value of the workgroup_size Z dimensions for a compute stage GPUShaderModule entry-point.
	 *
	 */
	val maxComputeWorkgroupSizeZ: UInt
	/**
	 * The maximum value for the arguments of dispatchWorkgroups(workgroupCountX, workgroupCountY, workgroupCountZ).
	 *
	 */
	val maxComputeWorkgroupsPerDimension: UInt
}

/**
 * GPUAdapterInfo exposes various identifying information about an adapter.
 *
 */
interface GPUAdapterInfo {
	/**
	 * The name of the vendor of the adapter, if available. Empty string otherwise.
	 *
	 */
	val vendor: String
	/**
	 * The name of the family or class of GPUs the adapter belongs to, if available. Empty string otherwise.
	 *
	 */
	val architecture: String
	/**
	 * A vendor-specific identifier for the adapter, if available. Empty string otherwise. Note: This is a value that represents the type of adapter. For example, it may be a PCI device ID. It does not uniquely identify a given piece of hardware like a serial number.
	 *
	 */
	val device: String
	/**
	 * A human readable string describing the adapter as reported by the driver, if available. Empty string otherwise. Note: Because no formatting is applied to description attempting to parse this value is not recommended. Applications which change their behavior based on the GPUAdapterInfo, such as applying workarounds for known driver issues, should rely on the other fields when possible.
	 *
	 */
	val description: String
	/**
	 * If the "subgroups" feature is supported, the minimum supported subgroup size for the adapter.
	 *
	 */
	val subgroupMinSize: UInt
	/**
	 * If the "subgroups" feature is supported, the maximum supported subgroup size for the adapter.
	 *
	 */
	val subgroupMaxSize: UInt
	/**
	 * Whether the adapter is a fallback adapter.
	 *
	 * See [GPUAdapterInfo.isFallbackAdapter in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuadapterinfo-isfallbackadapter).
	 *
	 */
	val isFallbackAdapter: Boolean
}

/**
 * A GPUAdapter encapsulates an adapter, and describes its capabilities (features and limits).
 *
 */
interface GPUAdapter : AutoCloseable {
	/**
	 * The set of values in this.[[adapter]].[[features]].
	 *
	 */
	val features: GPUSupportedFeatures
	/**
	 * The limits in this.[[adapter]].[[limits]].
	 *
	 * See [GPUAdapter.limits in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuadapter-limits).
	 *
	 */
	val limits: GPUSupportedLimits
	/**
	 * Information about the physical adapter underlying this GPUAdapter. For a given GPUAdapter, the GPUAdapterInfo values exposed are constant over time.
	 *
	 */
	val info: GPUAdapterInfo
	/**
	 * Requests a device from the adapter. This is a one-time action: if a device is returned successfully, the adapter becomes "consumed".
	 *
	 * @param descriptor Description of the GPUDevice to request.
	 *
	 */
	suspend fun requestDevice(descriptor: GPUDeviceDescriptor? = null): Result<GPUDevice>
}

/**
 * A GPUDevice encapsulates a device and exposes the functionality of that device.
 *
 * ## Lifetime
 *
 * `close()` destroys the device and triggers the loss notification observed by `awaitLost`, with the destruction reason when the backend provides one. Closing an already closed device does not release the same reference a second time.
 *
 */
interface GPUDevice : GPUObjectBase, AutoCloseable {
	/**
	 * A set containing the GPUFeatureName values of the features supported by the device ([[device]].[[features]]).
	 *
	 */
	val features: GPUSupportedFeatures
	/**
	 * The limits supported by the device ([[device]].[[limits]]).
	 *
	 */
	val limits: GPUSupportedLimits
	/**
	 * Information about the physical adapter which created the device that this GPUDevice refers to. For a given GPUDevice, the GPUAdapterInfo values exposed are constant over time.
	 *
	 */
	val adapterInfo: GPUAdapterInfo
	/**
	 * The primary GPUQueue for this device.
	 *
	 * See [GPUDevice.queue in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpudevice-queue).
	 *
	 */
	val queue: GPUQueue
	/**
	 * Creates a GPUBuffer.
	 *
	 * @param descriptor Description of the GPUBuffer to create.
	 *
	 */
	fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer
	/**
	 * Creates a GPUTexture.
	 *
	 * @param descriptor Description of the GPUTexture to create.
	 *
	 */
	fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture
	/**
	 * Creates a GPUSampler.
	 *
	 * @param descriptor Description of the GPUSampler to create.
	 *
	 */
	fun createSampler(descriptor: GPUSamplerDescriptor? = null): GPUSampler
	/**
	 * Creates a GPUBindGroupLayout.
	 *
	 * @param descriptor Description of the GPUBindGroupLayout to create.
	 *
	 */
	fun createBindGroupLayout(descriptor: GPUBindGroupLayoutDescriptor): GPUBindGroupLayout
	/**
	 * Creates a GPUPipelineLayout.
	 *
	 * @param descriptor Description of the GPUPipelineLayout to create.
	 *
	 */
	fun createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): GPUPipelineLayout
	/**
	 * Creates a GPUBindGroup.
	 *
	 * @param descriptor Description of the GPUBindGroup to create.
	 *
	 */
	fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup
	/**
	 * Creates a GPUShaderModule.
	 *
	 * @param descriptor Description of the GPUShaderModule to create.
	 *
	 */
	fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule
	/**
	 * Creates a GPUComputePipeline using immediate pipeline creation.
	 *
	 * @param descriptor Description of the GPUComputePipeline to create.
	 *
	 */
	fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): GPUComputePipeline
	/**
	 * Creates a GPURenderPipeline using immediate pipeline creation.
	 *
	 * @param descriptor Description of the GPURenderPipeline to create.
	 *
	 */
	fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): GPURenderPipeline
	/**
	 * Creates a GPUComputePipeline using async pipeline creation. The returned Promise resolves when the created pipeline is ready to be used without additional delay. If pipeline creation fails, the returned Promise rejects with an GPUPipelineError. (A GPUError is not dispatched to the device.)
	 *
	 * @param descriptor Description of the GPUComputePipeline to create.
	 *
	 */
	suspend fun createComputePipelineAsync(descriptor: GPUComputePipelineDescriptor): Result<GPUComputePipeline>
	/**
	 * Creates a GPURenderPipeline using async pipeline creation. The returned Promise resolves when the created pipeline is ready to be used without additional delay. If pipeline creation fails, the returned Promise rejects with an GPUPipelineError. (A GPUError is not dispatched to the device.)
	 *
	 * @param descriptor Description of the GPURenderPipeline to create.
	 *
	 */
	suspend fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Result<GPURenderPipeline>
	/**
	 * Creates a GPUCommandEncoder.
	 *
	 * @param descriptor Description of the GPUCommandEncoder to create.
	 *
	 */
	fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor? = null): GPUCommandEncoder
	/**
	 * Creates a GPURenderBundleEncoder.
	 *
	 * @param descriptor Description of the GPURenderBundleEncoder to create.
	 *
	 */
	fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): GPURenderBundleEncoder
	/**
	 * Creates a GPUQuerySet.
	 *
	 * @param descriptor Description of the GPUQuerySet to create.
	 *
	 */
	fun createQuerySet(descriptor: GPUQuerySetDescriptor): GPUQuerySet
	/**
	 * Pushes a new GPU error scope onto the [[errorScopeStack]] for this.
	 *
	 * @param filter Which class of errors this error scope observes.
	 *
	 */
	fun pushErrorScope(filter: GPUErrorFilter)
	/**
	 * Pops a GPU error scope off the [[errorScopeStack]] for this and resolves to any GPUError observed by the error scope, or null if none. There is no guarantee of the ordering of promise resolution.
	 *
	 */
	suspend fun popErrorScope(): Result<GPUError?>
/**
 * Waits until this device is lost and resolves with the loss information.
 *
 * The loss is a successful result: it carries a [GPUDeviceLostReason] and an
 * implementation-provided message, it is not a failure of the returned [Result].
 * Several observers may wait at the same time and all of them observe the same
 * loss; an observer that starts waiting after the loss resolves immediately.
 * Cancelling one observer neither cancels the other observers nor destroys the
 * device. Closing the device explicitly notifies the loss with the destruction
 * reason when the backend provides one.
 */
	suspend fun awaitLost(): Result<GPUDeviceLostInfo>
}

/**
 * A GPUBindGroupLayout defines the interface between a set of resources bound in a GPUBindGroup and their accessibility in shader stages.
 *
 */
interface GPUBindGroupLayout : GPUObjectBase, AutoCloseable
/**
 * A GPUBindGroup defines a set of resources to be bound together in a group and how the resources are used in shader stages.
 *
 */
interface GPUBindGroup : GPUObjectBase, AutoCloseable
/**
 * A GPUPipelineLayout defines the mapping between resources of all GPUBindGroup objects set up during command encoding in setBindGroup(), and the shaders of the pipeline set by GPURenderCommandsMixin.setPipeline or GPUComputePassEncoder.setPipeline.
 *
 */
interface GPUPipelineLayout : GPUObjectBase, AutoCloseable
/**
 * Contains shader code compiled for use in GPU pipelines.
 *
 */
interface GPUShaderModule : GPUObjectBase, AutoCloseable {
	/**
	 * Returns any messages generated during the GPUShaderModule’s compilation. The locations, order, and contents of messages are implementation-defined. In particular, messages aren’t necessarily ordered by lineNum.
	 *
	 */
	suspend fun getCompilationInfo(): Result<GPUCompilationInfo>
}

/**
 * A GPUCompilationMessage is an informational, warning, or error message generated by the GPUShaderModule compiler. The messages are intended to be human readable to help developers diagnose issues with their shader code. Each message may correspond to a single point or range of the shader source, or may be unassociated with any specific part of the code.
 *
 */
interface GPUCompilationMessage {
	/**
	 * The human-readable, localizable text for this compilation message. Note: The message should follow the best practices for language and direction information. This includes making use of any future standards which may emerge regarding the reporting of string language and direction metadata.
	 *
	 */
	val message: String
	/**
	 * The severity level of the message. If the type is "error", it corresponds to a shader-creation error.
	 *
	 */
	val type: GPUCompilationMessageType
	/**
	 * The line number in the shader code the message corresponds to. Value is one-based, such that a lineNum of 1 indicates the first line of the shader code. Lines are delimited by line breaks. If the message corresponds to a substring this points to the line on which the substring begins. Must be 0 if the message does not correspond to any specific point in the shader code.
	 *
	 */
	val lineNum: ULong
	/**
	 * The offset, in UTF-16 code units, from the beginning of line lineNum of the shader code to the point or beginning of the substring that the message corresponds to. Value is one-based, such that a linePos of 1 indicates the first code unit of the line. If message corresponds to a substring this points to the first UTF-16 code unit of the substring. Must be 0 if the message does not correspond to any specific point in the shader code.
	 *
	 */
	val linePos: ULong
	/**
	 * The offset from the beginning of the shader code in UTF-16 code units to the point or beginning of the substring that message corresponds to. Must reference the same position as lineNum and linePos. Must be 0 if the message does not correspond to any specific point in the shader code.
	 *
	 */
	val offset: ULong
	/**
	 * The number of UTF-16 code units in the substring that message corresponds to. If the message does not correspond with a substring then length must be 0.
	 *
	 */
	val length: ULong
}

/**
 * Contains the messages produced while compiling a GPUShaderModule.
 *
 */
interface GPUCompilationInfo {
	/**
	 * Compilation messages, including errors, warnings, and informational messages.
	 *
	 */
	val messages: List<GPUCompilationMessage>
}

/**
 * Common base for GPU pipelines, including access to bind group layouts.
 *
 */
interface GPUPipelineBase {
	/**
	 * Gets a GPUBindGroupLayout that is compatible with the GPUPipelineBase’s GPUBindGroupLayout at index.
	 *
	 * @param index Index into the pipeline layout’s [[bindGroupLayouts]] sequence.
	 *
	 */
	fun getBindGroupLayout(index: UInt): GPUBindGroupLayout
}

/**
 * A GPUComputePipeline is a kind of pipeline that controls the compute shader stage, and can be used in GPUComputePassEncoder.
 *
 */
interface GPUComputePipeline : GPUObjectBase, GPUPipelineBase, AutoCloseable
/**
 * A GPURenderPipeline is a kind of pipeline that controls the vertex and fragment shader stages, and can be used in GPURenderPassEncoder as well as GPURenderBundleEncoder.
 *
 */
interface GPURenderPipeline : GPUObjectBase, GPUPipelineBase, AutoCloseable
/**
 * A recorded sequence of GPU commands ready for submission to a GPUQueue.
 *
 */
interface GPUCommandBuffer : GPUObjectBase, AutoCloseable
/**
 * GPUCommandsMixin defines state common to all interfaces which encode commands. It has no methods.
 *
 */
interface GPUCommandsMixin
/**
 * Encodes render, compute, and copy commands into a command buffer.
 *
 */
interface GPUCommandEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, AutoCloseable {
	/**
	 * Begins encoding a render pass described by descriptor.
	 *
	 * @param descriptor Description of the GPURenderPassEncoder to create.
	 *
	 */
	fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder
	/**
	 * Begins encoding a compute pass described by descriptor.
	 *
	 */
	fun beginComputePass(descriptor: GPUComputePassDescriptor? = null): GPUComputePassEncoder
	/**
	 * Encode a command into the GPUCommandEncoder that copies data from a sub-region of a GPUBuffer to a sub-region of another GPUBuffer.
	 *
	 * @param source The GPUBuffer to copy from.
	 * @param sourceOffset Offset in bytes into source to begin copying from.
	 * @param destination The GPUBuffer to copy to.
	 * @param destinationOffset Offset in bytes into destination to place the copied data.
	 * @param size Bytes to copy.
	 *
	 */
	fun copyBufferToBuffer(source: GPUBuffer, sourceOffset: GPUSize64, destination: GPUBuffer, destinationOffset: GPUSize64, size: GPUSize64? = null)
	/**
	 * Encode a command into the GPUCommandEncoder that copies data from a sub-region of a GPUBuffer to a sub-region of one or multiple continuous texture subresources.
	 *
	 * @param source Combined with copySize, defines the region of the source buffer.
	 * @param destination Combined with copySize, defines the region of the destination texture subresource.
	 *
	 */
	fun copyBufferToTexture(source: GPUTexelCopyBufferInfo, destination: GPUTexelCopyTextureInfo, copySize: GPUExtent3D)
	/**
	 * Encode a command into the GPUCommandEncoder that copies data from a sub-region of one or multiple continuous texture subresources to a sub-region of a GPUBuffer.
	 *
	 * @param source Combined with copySize, defines the region of the source texture subresources.
	 * @param destination Combined with copySize, defines the region of the destination buffer.
	 *
	 */
	fun copyTextureToBuffer(source: GPUTexelCopyTextureInfo, destination: GPUTexelCopyBufferInfo, copySize: GPUExtent3D)
	/**
	 * Encode a command into the GPUCommandEncoder that copies data from a sub-region of one or multiple contiguous texture subresources to another sub-region of one or multiple continuous texture subresources.
	 *
	 * @param source Combined with copySize, defines the region of the source texture subresources.
	 * @param destination Combined with copySize, defines the region of the destination texture subresources.
	 *
	 */
	fun copyTextureToTexture(source: GPUTexelCopyTextureInfo, destination: GPUTexelCopyTextureInfo, copySize: GPUExtent3D)
	/**
	 * Encode a command into the GPUCommandEncoder that fills a sub-region of a GPUBuffer with zeros.
	 *
	 * @param buffer The GPUBuffer to clear.
	 * @param offset Offset in bytes into buffer where the sub-region to clear begins.
	 * @param size Size in bytes of the sub-region to clear. Defaults to the size of the buffer minus offset.
	 *
	 */
	fun clearBuffer(buffer: GPUBuffer, offset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Resolves query results from a GPUQuerySet out into a range of a GPUBuffer.
	 *
	 */
	fun resolveQuerySet(querySet: GPUQuerySet, firstQuery: GPUSize32, queryCount: GPUSize32, destination: GPUBuffer, destinationOffset: GPUSize64)
	/**
	 * Completes recording of the commands sequence and returns a corresponding GPUCommandBuffer.
	 *
	 */
	fun finish(descriptor: GPUCommandBufferDescriptor? = null): GPUCommandBuffer
}

/**
 * GPUBindingCommandsMixin assumes the presence of GPUObjectBase and GPUCommandsMixin members on the same object. It must only be included by interfaces which also include those mixins.
 *
 */
interface GPUBindingCommandsMixin {
	/**
	 * Sets the current GPUBindGroup for the given index.
	 *
	 * See [GPUBindingCommandsMixin.setBindGroup(index, bindGroup, dynamicOffsetsData) in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubindingcommandsmixin-setbindgroup).
	 *
	 */
	fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt> = emptyList())
	/**
	 * Sets immediate data for subsequent render or compute commands.
	 *
	 * @param rangeOffset Offset in bytes into the immediate data range to begin writing at.
	 * @param data Data to write into the immediate data range.
	 * @param dataOffset Offset into data to begin writing from. Given in elements if data is a TypedArray and bytes otherwise.
	 * @param dataSize Size of content to write from data. Given in elements if data is a TypedArray and bytes otherwise.
	 *
	 */
	fun setImmediates(rangeOffset: GPUSize32, data: ArrayBuffer, dataOffset: GPUSize64 = 0u, dataSize: GPUSize64? = null)
}

/**
 * Adds debug marker and debug group commands to command encoders and pass encoders.
 *
 */
interface GPUDebugCommandsMixin {
	/**
	 * Begins a labeled debug group containing subsequent commands.
	 *
	 * @param groupLabel The label for the command group.
	 *
	 */
	fun pushDebugGroup(groupLabel: String)
	/**
	 * Ends the labeled debug group most recently started by pushDebugGroup().
	 *
	 */
	fun popDebugGroup()
	/**
	 * Marks a point in a stream of commands with a label.
	 *
	 * @param markerLabel The label to insert.
	 *
	 */
	fun insertDebugMarker(markerLabel: String)
}

/**
 * Encodes compute dispatches and related commands in a compute pass.
 *
 */
interface GPUComputePassEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin {
	/**
	 * Sets the current GPUComputePipeline.
	 *
	 * @param pipeline The compute pipeline to use for subsequent dispatch commands.
	 *
	 */
	fun setPipeline(pipeline: GPUComputePipeline)
	/**
	 * Dispatch work to be performed with the current GPUComputePipeline. See § 23.1 Computing for the detailed specification.
	 *
	 * @param workgroupCountX X dimension of the grid of workgroups to dispatch.
	 * @param workgroupCountY Y dimension of the grid of workgroups to dispatch.
	 * @param workgroupCountZ Z dimension of the grid of workgroups to dispatch.
	 *
	 */
	fun dispatchWorkgroups(workgroupCountX: GPUSize32, workgroupCountY: GPUSize32 = 1u, workgroupCountZ: GPUSize32 = 1u)
	/**
	 * Dispatch work to be performed with the current GPUComputePipeline using parameters read from a GPUBuffer. See § 23.1 Computing for the detailed specification. The indirect dispatch parameters encoded in the buffer must be a tightly packed block of three 32-bit unsigned integer values (12 bytes total), given in the same order as the arguments for dispatchWorkgroups().
	 *
	 * @param indirectBuffer Buffer containing the indirect dispatch parameters.
	 * @param indirectOffset Offset in bytes into indirectBuffer where the dispatch data begins.
	 *
	 */
	fun dispatchWorkgroupsIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
	/**
	 * Completes recording of the compute pass commands sequence.
	 *
	 */
	fun end()
}

/**
 * When executing encoded render pass commands as part of a GPUCommandBuffer, an internal RenderState object is used to track the current state required for rendering.
 *
 */
interface GPURenderPassEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin, GPURenderCommandsMixin {
	/**
	 * Sets the viewport used during the rasterization stage to linearly map from normalized device coordinates to viewport coordinates.
	 *
	 * @param x Minimum X value of the viewport in pixels.
	 * @param y Minimum Y value of the viewport in pixels.
	 * @param width Width of the viewport in pixels.
	 * @param height Height of the viewport in pixels.
	 * @param minDepth Minimum depth value of the viewport.
	 * @param maxDepth Maximum depth value of the viewport.
	 *
	 */
	fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float)
	/**
	 * Sets the scissor rectangle used during the rasterization stage. After transformation into viewport coordinates any fragments which fall outside the scissor rectangle will be discarded.
	 *
	 * @param x Minimum X value of the scissor rectangle in pixels.
	 * @param y Minimum Y value of the scissor rectangle in pixels.
	 * @param width Width of the scissor rectangle in pixels.
	 * @param height Height of the scissor rectangle in pixels.
	 *
	 */
	fun setScissorRect(x: GPUIntegerCoordinate, y: GPUIntegerCoordinate, width: GPUIntegerCoordinate, height: GPUIntegerCoordinate)
	/**
	 * Sets the constant blend color and alpha values used with "constant" and "one-minus-constant" GPUBlendFactors.
	 *
	 * @param color The color to use when blending.
	 *
	 */
	fun setBlendConstant(color: GPUColor)
	/**
	 * Sets the [[stencilReference]] value used during stencil tests with the "replace" GPUStencilOperation.
	 *
	 * @param reference The new stencil reference value.
	 *
	 */
	fun setStencilReference(reference: GPUStencilValue)
	/**
	 * Begins an occlusion query at queryIndex; endOcclusionQuery marks the end of the query.
	 *
	 */
	fun beginOcclusionQuery(queryIndex: GPUSize32)
	/**
	 * Ends the occlusion query begun by beginOcclusionQuery.
	 *
	 */
	fun endOcclusionQuery()
	/**
	 * Executes the commands previously recorded into the given GPURenderBundles as part of this render pass. When a GPURenderBundle is executed, it does not inherit the render pass’s pipeline, bind groups, immediate data, or vertex and index buffers. After a GPURenderBundle has executed, the render pass’s pipeline, bind group, immediate data, and vertex/index buffer state is cleared (to the initial, empty values).
	 *
	 * @param bundles List of render bundles to execute.
	 *
	 */
	fun executeBundles(bundles: List<GPURenderBundle>)
	/**
	 * Completes recording of the render pass commands sequence.
	 *
	 */
	fun end()
}

/**
 * GPURenderCommandsMixin defines rendering commands common to GPURenderPassEncoder and GPURenderBundleEncoder.
 *
 */
interface GPURenderCommandsMixin {
	/**
	 * Sets the current GPURenderPipeline.
	 *
	 * @param pipeline The render pipeline to use for subsequent drawing commands.
	 *
	 */
	fun setPipeline(pipeline: GPURenderPipeline)
	/**
	 * Sets the current index buffer.
	 *
	 * @param buffer Buffer containing index data to use for subsequent drawing commands.
	 * @param indexFormat Format of the index data contained in buffer.
	 * @param offset Offset in bytes into buffer where the index data begins. Defaults to 0.
	 * @param size Size in bytes of the index data in buffer. Defaults to the size of the buffer minus the offset.
	 *
	 */
	fun setIndexBuffer(buffer: GPUBuffer, indexFormat: GPUIndexFormat, offset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Sets the current vertex buffer for the given slot.
	 *
	 * @param slot The vertex buffer slot to set the vertex buffer for.
	 * @param buffer Buffer containing vertex data to use for subsequent drawing commands.
	 * @param offset Offset in bytes into buffer where the vertex data begins. Defaults to 0.
	 * @param size Size in bytes of the vertex data in buffer. Defaults to the size of the buffer minus the offset.
	 *
	 */
	fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Draws primitives. See § 23.2 Rendering for the detailed specification.
	 *
	 * @param vertexCount The number of vertices to draw.
	 * @param instanceCount The number of instances to draw.
	 * @param firstVertex Offset into the vertex buffers, in vertices, to begin drawing from.
	 * @param firstInstance First instance to draw.
	 *
	 */
	fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32 = 1u, firstVertex: GPUSize32 = 0u, firstInstance: GPUSize32 = 0u)
	/**
	 * Draws indexed primitives. See § 23.2 Rendering for the detailed specification.
	 *
	 * @param indexCount The number of indices to draw.
	 * @param instanceCount The number of instances to draw.
	 * @param firstIndex Offset into the index buffer, in indices, begin drawing from.
	 * @param baseVertex Added to each index value before indexing into the vertex buffers.
	 * @param firstInstance First instance to draw.
	 *
	 */
	fun drawIndexed(indexCount: GPUSize32, instanceCount: GPUSize32 = 1u, firstIndex: GPUSize32 = 0u, baseVertex: GPUSignedOffset32 = 0, firstInstance: GPUSize32 = 0u)
	/**
	 * Draws primitives using parameters read from a GPUBuffer. See § 23.2 Rendering for the detailed specification. The indirect draw parameters encoded in the buffer must be a tightly packed block of four 32-bit unsigned integer values (16 bytes total), given in the same order as the arguments for draw().
	 *
	 * @param indirectBuffer Buffer containing the indirect draw parameters.
	 * @param indirectOffset Offset in bytes into indirectBuffer where the drawing data begins.
	 *
	 */
	fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
	/**
	 * Draws indexed primitives using parameters read from a GPUBuffer. See § 23.2 Rendering for the detailed specification. The indirect drawIndexed parameters encoded in the buffer must be a tightly packed block of five 32-bit values (20 bytes total), given in the same order as the arguments for drawIndexed(). The value corresponding to baseVertex is a signed 32-bit integer, and all others are unsigned 32-bit integers.
	 *
	 * @param indirectBuffer Buffer containing the indirect drawIndexed parameters.
	 * @param indirectOffset Offset in bytes into indirectBuffer where the drawing data begins.
	 *
	 */
	fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
}

/**
 * A reusable sequence of render commands created by a GPUDevice.
 *
 */
interface GPURenderBundle : GPUObjectBase
/**
 * Encodes render commands into a GPURenderBundle.
 *
 * See [GPURenderBundleEncoder in the WebGPU specification](https://www.w3.org/TR/webgpu/#gpurenderbundleencoder).
 *
 */
interface GPURenderBundleEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin, GPURenderCommandsMixin, AutoCloseable {
	/**
	 * Completes recording of the render bundle commands sequence.
	 *
	 */
	fun finish(descriptor: GPURenderBundleDescriptor? = null): GPURenderBundle
}

/**
 * Submits command buffers and schedules writes and copies for execution by the GPUDevice.
 *
 */
interface GPUQueue : GPUObjectBase {
	/**
	 * Schedules the execution of the command buffers by the GPU on this queue. Submitted command buffers cannot be used again.
	 *
	 */
	fun submit(commandBuffers: List<GPUCommandBuffer>)
	/**
	 * Returns a Promise that resolves once this queue finishes processing all the work submitted up to this moment. Resolution of this Promise implies the completion of mapAsync() calls made prior to that call, on GPUBuffers last used exclusively on that queue.
	 *
	 */
	suspend fun onSubmittedWorkDone(): Result<Unit>
	/**
	 * Issues a write operation of the provided data into a GPUBuffer.
	 *
	 * @param buffer The buffer to write to.
	 * @param bufferOffset Offset in bytes into buffer to begin writing at.
	 * @param data Data to write into buffer.
	 * @param dataOffset Offset in into data to begin writing from. Given in elements if data is a TypedArray and bytes otherwise.
	 * @param size Size of content to write from data to buffer. Given in elements if data is a TypedArray and bytes otherwise.
	 *
	 */
	fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBuffer, dataOffset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Issues a write operation of the provided data into a GPUTexture.
	 *
	 * @param destination The texture subresource and origin to write to.
	 * @param data Data to write into destination.
	 * @param dataLayout Layout of the content in data.
	 * @param size Extents of the content to write from data to destination.
	 *
	 */
	fun writeTexture(destination: GPUTexelCopyTextureInfo, data: ArrayBuffer, dataLayout: GPUTexelCopyBufferLayout, size: GPUExtent3D)
}

/**
 * Stores the results of a configured kind and count of GPU queries.
 *
 */
interface GPUQuerySet : GPUObjectBase, AutoCloseable {
	/**
	 * The type of the queries managed by this GPUQuerySet.
	 *
	 */
	val type: GPUQueryType
	/**
	 * The number of queries managed by this GPUQuerySet.
	 *
	 * See [GPUQuerySet.count in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuqueryset-count).
	 *
	 */
	val count: GPUSize32Out
}

/**
 * Information resolved by GPUDevice.lost when the device is lost, including the reason and diagnostic message.
 *
 */
interface GPUDeviceLostInfo {
	/**
	 * Reason reported for the device loss.
	 *
	 * See [GPUDeviceLostInfo.reason in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpudevicelostinfo-reason).
	 *
	 */
	val reason: GPUDeviceLostReason
	/**
	 * Implementation-provided diagnostic message describing the device loss.
	 *
	 */
	val message: String
}

/**
 * GPUError is the base interface for all errors surfaced from popErrorScope() and the uncapturederror event.
 *
 */
sealed interface GPUError {
	/**
	 * A human-readable, localizable text message providing information about the error that occurred. Note: This message is generally intended for application developers to debug their applications and capture information for debug reports, not to be surfaced to end-users.
	 *
	 */
	val message: String
}

/**
 * GPUValidationError is a subtype of GPUError which indicates that an operation did not satisfy all validation requirements. Validation errors are always indicative of an application error, and is expected to fail the same way across all devices assuming the same [[features]] and [[limits]] are in use.
 *
 */
interface GPUValidationError : GPUError
/**
 * GPUOutOfMemoryError is a subtype of GPUError which indicates that there was not enough free memory to complete the requested operation. The operation may succeed if attempted again with a lower memory requirement (like using smaller texture dimensions), or if memory used by other resources is released first.
 *
 */
interface GPUOutOfMemoryError : GPUError
/**
 * GPUInternalError is a subtype of GPUError which indicates than an operation failed for a system or implementation-specific reason even when all validation requirements have been satisfied. For example, the operation may exceed the capabilities of the implementation in a way not easily captured by the supported limits. The same operation may succeed on other devices or under difference circumstances.
 *
 */
interface GPUInternalError : GPUError
/**
 * Common label field shared by descriptors that create WebGPU objects.
 *
 */
interface GPUObjectDescriptorBase {
	/**
	 * The initial value of GPUObjectBase.label.
	 *
	 * See [GPUObjectDescriptorBase.label in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuobjectdescriptorbase-label).
	 *
	 */
	val label: String
}

/**
 * Options that constrain which GPU adapter is selected.
 *
 */
interface GPURequestAdapterOptions {
	/**
	 * Requests an adapter that supports at least a particular set of capabilities. This influences the [[default feature level]] of devices created from this adapter. The capabilities for each level are defined below, and the exact steps are defined in requestAdapter() and "a new device". If the implementation or system does not support all of the capabilities in the requested feature level, requestAdapter() will return null.
	 *
	 */
	val featureLevel: String
	/**
	 * Optionally provides a hint indicating what class of adapter should be selected from the system’s available adapters. The value of this hint may influence which adapter is chosen, but it must not influence whether an adapter is returned or not.
	 *
	 */
	val powerPreference: GPUPowerPreference?
	/**
	 * When set to true indicates that only a fallback adapter may be returned. If the user agent does not support a fallback adapter, will cause requestAdapter() to resolve to null. Note: requestAdapter() may still return a fallback adapter if forceFallbackAdapter is set to false and either no other appropriate adapter is available or the user agent chooses to return a fallback adapter. Developers that wish to prevent their applications from running on fallback adapters should check the info.isFallbackAdapter attribute prior to requesting a GPUDevice.
	 *
	 */
	val forceFallbackAdapter: Boolean
	/**
	 * When set to true indicates that the best adapter for rendering to a WebXR session must be returned. If the user agent or system does not support WebXR sessions then adapter selection may ignore this value. Note: If xrCompatible is not set to true when the adapter is requested, GPUDevices created from the adapter cannot be used to render for WebXR sessions.
	 *
	 */
	val xrCompatible: Boolean
}

/**
 * GPUDeviceDescriptor describes a device request.
 *
 * See [GPUDeviceDescriptor in the WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpudevicedescriptor).
 *
 */
interface GPUDeviceDescriptor : GPUObjectDescriptorBase {
	/**
	 * Specifies the features that are required by the device request. The request will fail if the adapter cannot provide these features. Exactly the specified set of features, and no more or less, will be allowed in validation of API calls on the resulting device.
	 *
	 */
	val requiredFeatures: List<GPUFeatureName>
	/**
	 * Specifies the limits that are required by the device request. The request will fail if the adapter cannot provide these limits. Each key with a non-undefined value must be the name of a member of supported limits.
	 *
	 */
	val requiredLimits: GPURequiredLimits?
	/**
	 * The descriptor for the default GPUQueue.
	 *
	 * See [GPUDeviceDescriptor.defaultQueue in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpudevicedescriptor-defaultqueue).
	 *
	 */
	val defaultQueue: GPUQueueDescriptor
	/**
	 * Callback invoked for errors that are not captured by an error scope.
	 *
	 */
	val onUncapturedError: GPUUncapturedErrorCallback?
}

/**
 * Describes a GPUBuffer's size, usage, and optional mapped-at-creation state.
 *
 */
interface GPUBufferDescriptor : GPUObjectDescriptorBase {
	/**
	 * The size of the buffer in bytes.
	 *
	 * See [GPUBufferDescriptor.size in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubufferdescriptor-size).
	 *
	 */
	val size: GPUSize64
	/**
	 * The allowed usages for the buffer.
	 *
	 * See [GPUBufferDescriptor.usage in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubufferdescriptor-usage).
	 *
	 */
	val usage: GPUBufferUsage
	/**
	 * If true creates the buffer in an already mapped state, allowing getMappedRange() to be called immediately. It is valid to set mappedAtCreation to true even if usage does not contain MAP_READ or MAP_WRITE. This can be used to set the buffer’s initial data. Guarantees that even if the buffer creation eventually fails, it will still appear as if the mapped range can be written/read to until it is unmapped.
	 *
	 */
	val mappedAtCreation: Boolean
}

/**
 * Describes a GPUTexture's size, format, usage, and optional view formats.
 *
 */
interface GPUTextureDescriptor : GPUObjectDescriptorBase {
	/**
	 * The width, height, and depth or layer count of the texture.
	 *
	 */
	val size: GPUExtent3D
	/**
	 * The number of mip levels the texture will contain.
	 *
	 * See [GPUTextureDescriptor.mipLevelCount in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexturedescriptor-miplevelcount).
	 *
	 */
	val mipLevelCount: GPUIntegerCoordinate
	/**
	 * The sample count of the texture. A sampleCount > 1 indicates a multisampled texture.
	 *
	 */
	val sampleCount: GPUSize32
	/**
	 * Whether the texture is one-dimensional, an array of two-dimensional layers, or three-dimensional.
	 *
	 */
	val dimension: GPUTextureDimension
	/**
	 * The format of the texture.
	 *
	 * See [GPUTextureDescriptor.format in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexturedescriptor-format).
	 *
	 */
	val format: GPUTextureFormat
	/**
	 * The allowed usages for the texture.
	 *
	 * See [GPUTextureDescriptor.usage in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexturedescriptor-usage).
	 *
	 */
	val usage: GPUTextureUsage
	/**
	 * Specifies what view format values will be allowed when calling createView() on this texture (in addition to the texture’s actual format). Formats in this list must be texture view format compatible with the texture format.
	 *
	 */
	val viewFormats: List<GPUTextureFormat>
	/**
	 * On devices with "core-features-and-limits", this is ignored, and there is no such restriction.
	 *
	 */
	val textureBindingViewDimension: GPUTextureViewDimension?
}

/**
 * Describes the format, dimension, mip levels, array layers, and aspect of a GPUTextureView.
 *
 */
interface GPUTextureViewDescriptor : GPUObjectDescriptorBase {
	/**
	 * The format of the texture view. Must be either the format of the texture or one of the viewFormats specified during its creation.
	 *
	 */
	val format: GPUTextureFormat?
	/**
	 * The dimension to view the texture as.
	 *
	 * See [GPUTextureViewDescriptor.dimension in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputextureviewdescriptor-dimension).
	 *
	 */
	val dimension: GPUTextureViewDimension?
	/**
	 * The allowed usage(s) for the texture view. Must be a subset of the usage flags of the texture. If 0, defaults to the full set of usage flags of the texture. Note: If the view’s format doesn’t support all of the texture’s usages, the default will fail, and the view’s usage must be specified explicitly.
	 *
	 */
	val usage: GPUTextureUsage
	/**
	 * Which aspect(s) of the texture are accessible to the texture view.
	 *
	 */
	val aspect: GPUTextureAspect
	/**
	 * The first (most detailed) mipmap level accessible to the texture view.
	 *
	 */
	val baseMipLevel: GPUIntegerCoordinate
	/**
	 * How many mipmap levels, starting with baseMipLevel, are accessible to the texture view.
	 *
	 */
	val mipLevelCount: GPUIntegerCoordinate?
	/**
	 * The index of the first array layer accessible to the texture view.
	 *
	 */
	val baseArrayLayer: GPUIntegerCoordinate
	/**
	 * How many array layers, starting with baseArrayLayer, are accessible to the texture view.
	 *
	 */
	val arrayLayerCount: GPUIntegerCoordinate?
	/**
	 * A string of length four, with each character mapping to the texture view’s red/green/blue/alpha channels, respectively. "r": Take its value from the red channel of the texture.
	 *
	 */
	val swizzle: GPUTextureSwizzle
}

/**
 * Describes the address modes, filtering modes, and level-of-detail limits for a GPUSampler.
 *
 */
interface GPUSamplerDescriptor : GPUObjectDescriptorBase {
	/**
	 * Specifies the address modes for the texture width, height, and depth coordinates, respectively.
	 *
	 */
	val addressModeU: GPUAddressMode
	/**
	 * Specifies the address modes for the texture width, height, and depth coordinates, respectively.
	 *
	 */
	val addressModeV: GPUAddressMode
	/**
	 * Specifies the address modes for the texture width, height, and depth coordinates, respectively.
	 *
	 */
	val addressModeW: GPUAddressMode
	/**
	 * Specifies the sampling behavior when the sampled area is smaller than or equal to one texel.
	 *
	 */
	val magFilter: GPUFilterMode
	/**
	 * Specifies the sampling behavior when the sampled area is larger than one texel.
	 *
	 */
	val minFilter: GPUFilterMode
	/**
	 * Specifies behavior for sampling between mipmap levels.
	 *
	 */
	val mipmapFilter: GPUMipmapFilterMode
	/**
	 * Specifies the minimum and maximum levels of detail, respectively, used internally when sampling a texture.
	 *
	 */
	val lodMinClamp: Float
	/**
	 * Specifies the minimum and maximum levels of detail, respectively, used internally when sampling a texture.
	 *
	 */
	val lodMaxClamp: Float
	/**
	 * When provided the sampler will be a comparison sampler with the specified GPUCompareFunction. Note: Comparison samplers may use filtering, but the sampling results will be implementation-dependent and may differ from the normal filtering rules.
	 *
	 */
	val compare: GPUCompareFunction?
	/**
	 * Specifies the maximum anisotropy value clamp used by the sampler. Anisotropic filtering is enabled when maxAnisotropy is > 1 and the implementation supports it. Anisotropic filtering improves the image quality of textures sampled at oblique viewing angles. Higher maxAnisotropy values indicate the maximum ratio of anisotropy supported when filtering.
	 *
	 */
	val maxAnisotropy: UShort
}

/**
 * Describes the entries that define a GPUBindGroupLayout.
 *
 */
interface GPUBindGroupLayoutDescriptor : GPUObjectDescriptorBase {
	/**
	 * A list of entries describing the shader resource bindings for a bind group.
	 *
	 */
	val entries: List<GPUBindGroupLayoutEntry>
}

/**
 * Describes one binding's index, shader visibility, and resource-specific layout.
 *
 */
interface GPUBindGroupLayoutEntry {
	/**
	 * A unique identifier for a resource binding within the GPUBindGroupLayout, corresponding to a GPUBindGroupEntry.binding and a @binding attribute in the GPUShaderModule.
	 *
	 */
	val binding: GPUIndex32
	/**
	 * A bitset of the members of GPUShaderStage. Each set bit indicates that a GPUBindGroupLayoutEntry’s resource will be accessible from the associated shader stage.
	 *
	 */
	val visibility: GPUShaderStage
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	val buffer: GPUBufferBindingLayout?
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	val sampler: GPUSamplerBindingLayout?
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	val texture: GPUTextureBindingLayout?
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	val storageTexture: GPUStorageTextureBindingLayout?
}

/**
 * Describes the type, dynamic-offset behavior, and minimum size of a buffer binding.
 *
 */
interface GPUBufferBindingLayout {
	/**
	 * Indicates the type required for buffers bound to this binding.
	 *
	 */
	val type: GPUBufferBindingType
	/**
	 * Indicates whether this binding requires a dynamic offset.
	 *
	 */
	val hasDynamicOffset: Boolean
	/**
	 * Indicates the minimum size of a buffer binding used with this bind point. Bindings are always validated against this size in createBindGroup().
	 *
	 */
	val minBindingSize: GPUSize64
}

/**
 * Describes the sampler binding type expected by a shader.
 *
 */
interface GPUSamplerBindingLayout {
	/**
	 * Indicates the required type of a sampler bound to this binding.
	 *
	 */
	val type: GPUSamplerBindingType
}

/**
 * Describes the sample type, view dimension, and multisampling of a texture binding.
 *
 */
interface GPUTextureBindingLayout {
	/**
	 * Indicates the type required for texture views bound to this binding.
	 *
	 */
	val sampleType: GPUTextureSampleType
	/**
	 * Indicates the required dimension for texture views bound to this binding.
	 *
	 */
	val viewDimension: GPUTextureViewDimension
	/**
	 * Indicates whether or not texture views bound to this binding must be multisampled.
	 *
	 */
	val multisampled: Boolean
}

/**
 * Describes the access mode, format, and view dimension of a storage texture binding.
 *
 */
interface GPUStorageTextureBindingLayout {
	/**
	 * The access mode for this binding, indicating readability and writability.
	 *
	 */
	val access: GPUStorageTextureAccess
	/**
	 * The required format of texture views bound to this binding.
	 *
	 */
	val format: GPUTextureFormat
	/**
	 * Indicates the required dimension for texture views bound to this binding.
	 *
	 */
	val viewDimension: GPUTextureViewDimension
}

/**
 * Describes the layout and resources used to create a GPUBindGroup.
 *
 */
interface GPUBindGroupDescriptor : GPUObjectDescriptorBase {
	/**
	 * The GPUBindGroupLayout the entries of this bind group will conform to.
	 *
	 */
	val layout: GPUBindGroupLayout
	/**
	 * A list of entries describing the resources to expose to the shader for each binding described by the layout.
	 *
	 */
	val entries: List<GPUBindGroupEntry>
}

/**
 * Associates a binding index with the GPU resource supplied for that binding.
 *
 */
interface GPUBindGroupEntry {
	/**
	 * A unique identifier for a resource binding within the GPUBindGroup, corresponding to a GPUBindGroupLayoutEntry.binding and a @binding attribute in the GPUShaderModule.
	 *
	 */
	val binding: GPUIndex32
	/**
	 * The resource to bind, which may be a GPUSampler, GPUTexture, GPUTextureView, GPUBuffer, GPUBufferBinding, or GPUExternalTexture.
	 *
	 */
	val resource: GPUBindingResource
}

/**
 * Lists the GPUBindGroupLayouts used by a pipeline.
 *
 * See [GPUPipelineLayoutDescriptor in the WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpupipelinelayoutdescriptor).
 *
 */
interface GPUPipelineLayoutDescriptor : GPUObjectDescriptorBase {
	/**
	 * A list of optional GPUBindGroupLayouts the pipeline will use. Each element corresponds to a @group attribute in the GPUShaderModule, with the Nth element corresponding with @group(N).
	 *
	 */
	val bindGroupLayouts: List<GPUBindGroupLayout?>
	/**
	 * The size, in bytes, of the immediate data range used by the pipeline.
	 *
	 */
	val immediateSize: GPUSize32
}

/**
 * Describes the shader code and compilation options used to create a GPUShaderModule.
 *
 */
interface GPUShaderModuleDescriptor : GPUObjectDescriptorBase {
	/**
	 * The WGSL source code for the shader module.
	 *
	 * See [GPUShaderModuleDescriptor.code in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpushadermoduledescriptor-code).
	 *
	 */
	val code: String
	/**
	 * A list of GPUShaderModuleCompilationHints. Any hint provided by an application should contain information about one entry point of a pipeline that will eventually be created from the entry point.
	 *
	 */
	val compilationHints: List<GPUShaderModuleCompilationHint>
}

/**
 * A hint associating a shader entry point with the pipeline layout expected to use it.
 *
 */
interface GPUShaderModuleCompilationHint {
	/**
	 * Name of the shader entry point associated with this compilation hint.
	 *
	 */
	val entryPoint: String
	/**
	 * A GPUPipelineLayout that the GPUShaderModule may be used with in a future createComputePipeline() or createRenderPipeline() call. If set to "auto" the layout will be the default pipeline layout for the entry point associated with this hint will be used.
	 *
	 */
	val layout: GPUPipelineLayout?
}

/**
 * Common label field shared by render and compute pipeline descriptors.
 *
 */
interface GPUPipelineDescriptorBase : GPUObjectDescriptorBase {
	/**
	 * The GPUPipelineLayout for this pipeline, or "auto" to generate the pipeline layout automatically. Note: If "auto" is used the pipeline cannot share GPUBindGroups with any other pipelines.
	 *
	 */
	val layout: GPUPipelineLayout?
}

/**
 * A GPUProgrammableStage describes the entry point in the user-provided GPUShaderModule that controls one of the programmable stages of a pipeline. Entry point names follow the rules defined in WGSL identifier comparison.
 *
 */
interface GPUProgrammableStage {
	/**
	 * The GPUShaderModule containing the code that this programmable stage will execute.
	 *
	 */
	val module: GPUShaderModule
	/**
	 * The name of the function in module that this stage will use to perform its work. NOTE: Since the entryPoint dictionary member is not required, methods which consume a GPUProgrammableStage must use the "get the entry point" algorithm to determine which entry point it refers to.
	 *
	 */
	val entryPoint: String?
	/**
	 * Specifies the values of pipeline-overridable constants in the shader module module. Each such pipeline-overridable constant is uniquely identified by a single pipeline-overridable constant identifier string, representing the pipeline constant ID of the constant if its declaration specifies one, and otherwise the constant’s identifier name.
	 *
	 */
	val constants: Map<String, GPUPipelineConstantValue>
}

/**
 * Describes the layout and compute shader stage used to create a GPUComputePipeline.
 *
 */
interface GPUComputePipelineDescriptor : GPUPipelineDescriptorBase {
	/**
	 * Describes the compute shader entry point of the pipeline.
	 *
	 */
	val compute: GPUProgrammableStage
}

/**
 * Describes the layout and programmable stages and fixed-function state of a GPURenderPipeline.
 *
 */
interface GPURenderPipelineDescriptor : GPUPipelineDescriptorBase {
	/**
	 * Describes the vertex shader entry point of the pipeline and its input buffer layouts.
	 *
	 */
	val vertex: GPUVertexState
	/**
	 * Describes the primitive-related properties of the pipeline.
	 *
	 */
	val primitive: GPUPrimitiveState
	/**
	 * Describes the optional depth-stencil properties, including the testing, operations, and bias.
	 *
	 */
	val depthStencil: GPUDepthStencilState?
	/**
	 * Describes the multi-sampling properties of the pipeline.
	 *
	 */
	val multisample: GPUMultisampleState
	/**
	 * Describes the fragment shader entry point of the pipeline and its output colors. If not provided, the § 23.2.8 No Color Output mode is enabled.
	 *
	 */
	val fragment: GPUFragmentState?
}

/**
 * Describes primitive topology, strip format, front face, and culling for a render pipeline.
 *
 */
interface GPUPrimitiveState {
	/**
	 * The type of primitive to be constructed from the vertex inputs.
	 *
	 */
	val topology: GPUPrimitiveTopology
	/**
	 * For pipelines with strip topologies ("line-strip" or "triangle-strip"), this determines the index buffer format and primitive restart value ("uint16"/0xFFFF or "uint32"/0xFFFFFFFF). It is not allowed on pipelines with non-strip topologies. Note: Some implementations require knowledge of the primitive restart value to compile pipeline state objects.
	 *
	 */
	val stripIndexFormat: GPUIndexFormat?
	/**
	 * Defines which polygons are considered front-facing.
	 *
	 */
	val frontFace: GPUFrontFace
	/**
	 * Defines which polygon orientation will be culled, if any.
	 *
	 */
	val cullMode: GPUCullMode
	/**
	 * If true, indicates that depth clipping is disabled. Requires the "depth-clip-control" feature to be enabled.
	 *
	 */
	val unclippedDepth: Boolean
}

/**
 * Describes sample count, sample mask, and alpha-to-coverage behavior.
 *
 */
interface GPUMultisampleState {
	/**
	 * Number of samples per pixel. This GPURenderPipeline will be compatible only with attachment textures (colorAttachments and depthStencilAttachment) with matching sampleCounts.
	 *
	 */
	val count: GPUSize32
	/**
	 * Mask determining which samples are written to.
	 *
	 * See [GPUMultisampleState.mask in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpumultisamplestate-mask).
	 *
	 */
	val mask: GPUSampleMask
	/**
	 * When true indicates that a fragment’s alpha channel should be used to generate a sample coverage mask.
	 *
	 */
	val alphaToCoverageEnabled: Boolean
}

/**
 * Describes the fragment shader stage and its color targets.
 *
 */
interface GPUFragmentState : GPUProgrammableStage {
	/**
	 * A list of GPUColorTargetState defining the formats and behaviors of the color targets this pipeline writes to.
	 *
	 */
	val targets: List<GPUColorTargetState?>
}

/**
 * Describes the format, write mask, and optional blending for a fragment color target.
 *
 */
interface GPUColorTargetState {
	/**
	 * The GPUTextureFormat of this color target. The pipeline will only be compatible with GPURenderPassEncoders which use a GPUTextureView of this format in the corresponding color attachment.
	 *
	 */
	val format: GPUTextureFormat
	/**
	 * The blending behavior for this color target. If left undefined, disables blending for this color target.
	 *
	 */
	val blend: GPUBlendState?
	/**
	 * Bitmask controlling which channels are are written to when drawing to this color target.
	 *
	 */
	val writeMask: GPUColorWrite
}

/**
 * Describes the color and alpha blend components applied to a color target.
 *
 */
interface GPUBlendState {
	/**
	 * Defines the blending behavior of the corresponding render target for color channels.
	 *
	 */
	val color: GPUBlendComponent
	/**
	 * Defines the blending behavior of the corresponding render target for the alpha channel.
	 *
	 */
	val alpha: GPUBlendComponent
}

/**
 * Describes one color or alpha blend component's operation and factors.
 *
 */
interface GPUBlendComponent {
	/**
	 * Defines the GPUBlendOperation used to calculate the values written to the target attachment components.
	 *
	 */
	val operation: GPUBlendOperation
	/**
	 * Defines the GPUBlendFactor operation to be performed on values from the fragment shader.
	 *
	 */
	val srcFactor: GPUBlendFactor
	/**
	 * Defines the GPUBlendFactor operation to be performed on values from the target attachment.
	 *
	 */
	val dstFactor: GPUBlendFactor
}

/**
 * Describes depth testing, stencil testing, and depth bias for a render pipeline.
 *
 */
interface GPUDepthStencilState {
	/**
	 * The format of depthStencilAttachment this GPURenderPipeline will be compatible with.
	 *
	 */
	val format: GPUTextureFormat
	/**
	 * Indicates if this GPURenderPipeline can modify depthStencilAttachment depth values.
	 *
	 */
	val depthWriteEnabled: Boolean?
	/**
	 * The comparison operation used to test fragment depths against depthStencilAttachment depth values.
	 *
	 */
	val depthCompare: GPUCompareFunction?
	/**
	 * Defines how stencil comparisons and operations are performed for front-facing primitives.
	 *
	 */
	val stencilFront: GPUStencilFaceState
	/**
	 * Defines how stencil comparisons and operations are performed for back-facing primitives.
	 *
	 */
	val stencilBack: GPUStencilFaceState
	/**
	 * Bitmask controlling which depthStencilAttachment stencil value bits are read when performing stencil comparison tests.
	 *
	 */
	val stencilReadMask: GPUStencilValue
	/**
	 * Bitmask controlling which depthStencilAttachment stencil value bits are written to when performing stencil operations.
	 *
	 */
	val stencilWriteMask: GPUStencilValue
	/**
	 * Constant depth bias added to each triangle fragment. See biased fragment depth for details.
	 *
	 */
	val depthBias: GPUDepthBias
	/**
	 * Depth bias that scales with the triangle fragment’s slope. See biased fragment depth for details.
	 *
	 */
	val depthBiasSlopeScale: Float
	/**
	 * The maximum depth bias of a triangle fragment. See biased fragment depth for details.
	 *
	 */
	val depthBiasClamp: Float
}

/**
 * Describes stencil comparison and the operations applied to a stencil face.
 *
 */
interface GPUStencilFaceState {
	/**
	 * The GPUCompareFunction used when testing the [[stencilReference]] value against the fragment’s depthStencilAttachment stencil values.
	 *
	 */
	val compare: GPUCompareFunction
	/**
	 * The GPUStencilOperation performed if the fragment stencil comparison test described by compare fails.
	 *
	 */
	val failOp: GPUStencilOperation
	/**
	 * The GPUStencilOperation performed if the fragment depth comparison described by depthCompare fails.
	 *
	 */
	val depthFailOp: GPUStencilOperation
	/**
	 * The GPUStencilOperation performed if the fragment stencil comparison test described by compare passes.
	 *
	 */
	val passOp: GPUStencilOperation
}

/**
 * Describes the vertex shader stage and vertex buffer layouts for a render pipeline.
 *
 */
interface GPUVertexState : GPUProgrammableStage {
	/**
	 * A list of GPUVertexBufferLayouts, each defining the layout of vertex attribute data in a vertex buffer used by this pipeline.
	 *
	 */
	val buffers: List<GPUVertexBufferLayout?>
}

/**
 * Describes the stride, step mode, and attributes of a vertex buffer.
 *
 */
interface GPUVertexBufferLayout {
	/**
	 * The stride, in bytes, between elements of this array.
	 *
	 */
	val arrayStride: GPUSize64
	/**
	 * Whether each element of this array represents per-vertex data or per-instance data
	 *
	 */
	val stepMode: GPUVertexStepMode
	/**
	 * An array defining the layout of the vertex attributes within each element.
	 *
	 */
	val attributes: List<GPUVertexAttribute>
}

/**
 * Describes one vertex attribute's format and byte offset in a vertex buffer.
 *
 */
interface GPUVertexAttribute {
	/**
	 * The GPUVertexFormat of the attribute.
	 *
	 * See [GPUVertexAttribute.format in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuvertexattribute-format).
	 *
	 */
	val format: GPUVertexFormat
	/**
	 * The offset, in bytes, from the beginning of the element to the data for the attribute.
	 *
	 */
	val offset: GPUSize64
	/**
	 * The numeric location associated with this attribute, which will correspond with a "@location" attribute declared in the vertex.module.
	 *
	 */
	val shaderLocation: GPUIndex32
}

/**
 * "GPUTexelCopyBufferLayout" describes the "layout" of texels in a "buffer" of bytes (GPUBuffer or AllowSharedBufferSource) in a "texel copy" operation.
 *
 */
interface GPUTexelCopyBufferLayout {
	/**
	 * The offset, in bytes, from the beginning of the texel data source (such as a GPUTexelCopyBufferInfo.buffer) to the start of the texel data within that source.
	 *
	 */
	val offset: GPUSize64
	/**
	 * The stride, in bytes, between the beginning of each texel block row and the subsequent texel block row. Required if there are multiple texel block rows (i.e. the copy height or depth is more than one block).
	 *
	 */
	val bytesPerRow: GPUSize32?
	/**
	 * Number of texel block rows per single texel image of the texture. rowsPerImage × bytesPerRow is the stride, in bytes, between the beginning of each texel image of data and the subsequent texel image. Required if there are multiple texel images (i.e. the copy depth is more than one).
	 *
	 */
	val rowsPerImage: GPUSize32?
}

/**
 * "GPUTexelCopyBufferInfo" describes the "info" (GPUBuffer and GPUTexelCopyBufferLayout) about a "buffer" source or destination of a "texel copy" operation. Together with the copySize, it describes the footprint of a region of texels in a GPUBuffer.
 *
 */
interface GPUTexelCopyBufferInfo : GPUTexelCopyBufferLayout {
	/**
	 * A buffer which either contains texel data to be copied or will store the texel data being copied, depending on the method it is being passed to.
	 *
	 */
	val buffer: GPUBuffer
}

/**
 * "GPUTexelCopyTextureInfo" describes the "info" (GPUTexture, etc.) about a "texture" source or destination of a "texel copy" operation. Together with the copySize, it describes a sub-region of a texture (spanning one or more contiguous texture subresources at the same mip-map level).
 *
 */
interface GPUTexelCopyTextureInfo {
	/**
	 * Texture to copy to/from.
	 *
	 * See [GPUTexelCopyTextureInfo.texture in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexelcopytextureinfo-texture).
	 *
	 */
	val texture: GPUTexture
	/**
	 * Mip-map level of the texture to copy to/from.
	 *
	 * See [GPUTexelCopyTextureInfo.mipLevel in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gputexelcopytextureinfo-miplevel).
	 *
	 */
	val mipLevel: GPUIntegerCoordinate
	/**
	 * Defines the origin of the copy - the minimum corner of the texture sub-region to copy to/from. Together with copySize, defines the full copy sub-region.
	 *
	 */
	val origin: GPUOrigin3D
	/**
	 * Defines which aspects of the texture to copy to/from.
	 *
	 */
	val aspect: GPUTextureAspect
}

/**
 * Options used when finishing a GPUCommandEncoder to create a GPUCommandBuffer.
 *
 */
interface GPUCommandBufferDescriptor : GPUObjectDescriptorBase
/**
 * Options used to create a GPUCommandEncoder.
 *
 * See [GPUCommandEncoderDescriptor in the WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpucommandencoderdescriptor).
 *
 */
interface GPUCommandEncoderDescriptor : GPUObjectDescriptorBase
/**
 * Selects query slots for compute-pass beginning and end timestamps.
 *
 */
interface GPUComputePassTimestampWrites {
	/**
	 * The GPUQuerySet, of type "timestamp", that the query results will be written to.
	 *
	 */
	val querySet: GPUQuerySet
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the beginning of the compute pass will be written.
	 *
	 */
	val beginningOfPassWriteIndex: GPUSize32?
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the end of the compute pass will be written.
	 *
	 */
	val endOfPassWriteIndex: GPUSize32?
}

/**
 * Options for beginning a compute pass, including optional timestamp writes.
 *
 */
interface GPUComputePassDescriptor : GPUObjectDescriptorBase {
	/**
	 * Defines which timestamp values will be written for this pass, and where to write them to.
	 *
	 */
	val timestampWrites: GPUComputePassTimestampWrites?
}

/**
 * Selects query slots for render-pass beginning, end, and optional per-stage timestamps.
 *
 */
interface GPURenderPassTimestampWrites {
	/**
	 * The GPUQuerySet, of type "timestamp", that the query results will be written to.
	 *
	 */
	val querySet: GPUQuerySet
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the beginning of the render pass will be written.
	 *
	 */
	val beginningOfPassWriteIndex: GPUSize32?
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the end of the render pass will be written.
	 *
	 */
	val endOfPassWriteIndex: GPUSize32?
}

/**
 * Describes the color, depth/stencil, and optional timestamp attachments for a render pass.
 *
 */
interface GPURenderPassDescriptor : GPUObjectDescriptorBase {
	/**
	 * The set of GPURenderPassColorAttachment values in this sequence defines which color attachments will be output to when executing this render pass. Due to usage compatibility, no color attachment may alias another attachment or any resource used inside the render pass.
	 *
	 */
	val colorAttachments: List<GPURenderPassColorAttachment?>
	/**
	 * The GPURenderPassDepthStencilAttachment value that defines the depth/stencil attachment that will be output to and tested against when executing this render pass. Due to usage compatibility, no writable depth/stencil attachment may alias another attachment or any resource used inside the render pass.
	 *
	 */
	val depthStencilAttachment: GPURenderPassDepthStencilAttachment?
	/**
	 * The GPUQuerySet value defines where the occlusion query results will be stored for this pass.
	 *
	 */
	val occlusionQuerySet: GPUQuerySet?
	/**
	 * Defines which timestamp values will be written for this pass, and where to write them to.
	 *
	 */
	val timestampWrites: GPURenderPassTimestampWrites?
	/**
	 * The maximum number of draw calls that will be done in the render pass. Used by some implementations to size work injected before the render pass. Keeping the default value is a good default, unless it is known that more draw calls will be done.
	 *
	 */
	val maxDrawCount: GPUSize64
}

/**
 * Describes a color attachment and its load, store, and resolve operations for a render pass.
 *
 */
interface GPURenderPassColorAttachment {
	/**
	 * Describes the texture subresource that will be output to for this color attachment. The subresource is determined by calling get as texture view(view).
	 *
	 */
	val view: GPUTextureOrGPUTextureView
	/**
	 * Indicates the depth slice index of "3d" view that will be output to for this color attachment.
	 *
	 */
	val depthSlice: GPUIntegerCoordinate?
	/**
	 * Describes the texture subresource that will receive the resolved output for this color attachment if view is multisampled. The subresource is determined by calling get as texture view(resolveTarget).
	 *
	 */
	val resolveTarget: GPUTextureOrGPUTextureView?
	/**
	 * Indicates the value to clear view to prior to executing the render pass. If not provided, defaults to {r: 0, g: 0, b: 0, a: 0}. Ignored if loadOp is not "clear". The components of clearValue are all double values. They are converted to a texel value of texture format matching the render attachment. If conversion fails, a validation error is generated.
	 *
	 */
	val clearValue: GPUColor?
	/**
	 * Indicates the load operation to perform on view prior to executing the render pass. Note: It is recommended to prefer clearing; see "clear" for details.
	 *
	 */
	val loadOp: GPULoadOp
	/**
	 * The store operation to perform on view after executing the render pass.
	 *
	 */
	val storeOp: GPUStoreOp
}

/**
 * A value that is either a GPUTexture or a GPUTextureView.
 *
 */
sealed interface GPUTextureOrGPUTextureView
/**
 * Describes the depth and stencil operations for a render pass attachment.
 *
 */
interface GPURenderPassDepthStencilAttachment {
	/**
	 * Describes the texture subresource that will be output to and read from for this depth/stencil attachment. The subresource is determined by calling get as texture view(view).
	 *
	 */
	val view: GPUTextureOrGPUTextureView
	/**
	 * Indicates the value to clear view’s depth component to prior to executing the render pass. Ignored if depthLoadOp is not "clear". Must be between 0.0 and 1.0, inclusive.
	 *
	 */
	val depthClearValue: Float?
	/**
	 * Indicates the load operation to perform on view’s depth component prior to executing the render pass. Note: It is recommended to prefer clearing; see "clear" for details.
	 *
	 */
	val depthLoadOp: GPULoadOp?
	/**
	 * The store operation to perform on view’s depth component after executing the render pass.
	 *
	 */
	val depthStoreOp: GPUStoreOp?
	/**
	 * Indicates that the depth component of view is read only.
	 *
	 */
	val depthReadOnly: Boolean
	/**
	 * Indicates the value to clear view’s stencil component to prior to executing the render pass. Ignored if stencilLoadOp is not "clear". The value will be converted to the type of the stencil aspect of view by taking the same number of LSBs as the number of bits in the stencil aspect of one texel of view.
	 *
	 */
	val stencilClearValue: GPUStencilValue
	/**
	 * Indicates the load operation to perform on view’s stencil component prior to executing the render pass. Note: It is recommended to prefer clearing; see "clear" for details.
	 *
	 */
	val stencilLoadOp: GPULoadOp?
	/**
	 * The store operation to perform on view’s stencil component after executing the render pass.
	 *
	 */
	val stencilStoreOp: GPUStoreOp?
	/**
	 * Indicates that the stencil component of view is read only.
	 *
	 */
	val stencilReadOnly: Boolean
}

/**
 * Describes the attachment formats and sample count used by a render pass or render bundle.
 *
 */
interface GPURenderPassLayout : GPUObjectDescriptorBase {
	/**
	 * A list of the GPUTextureFormats of the color attachments for this pass or bundle.
	 *
	 */
	val colorFormats: List<GPUTextureFormat?>
	/**
	 * The GPUTextureFormat of the depth/stencil attachment for this pass or bundle.
	 *
	 */
	val depthStencilFormat: GPUTextureFormat?
	/**
	 * Number of samples per pixel in the attachments for this pass or bundle.
	 *
	 */
	val sampleCount: GPUSize32
}

/**
 * Options used when finishing a GPURenderBundleEncoder to create a GPURenderBundle.
 *
 */
interface GPURenderBundleDescriptor : GPUObjectDescriptorBase
/**
 * Options used to create a GPURenderBundleEncoder.
 *
 * See [GPURenderBundleEncoderDescriptor in the WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpurenderbundleencoderdescriptor).
 *
 */
interface GPURenderBundleEncoderDescriptor : GPURenderPassLayout {
	/**
	 * If true, indicates that the render bundle does not modify the depth component of the GPURenderPassDepthStencilAttachment of any render pass the render bundle is executed in. See read-only depth-stencil.
	 *
	 */
	val depthReadOnly: Boolean
	/**
	 * If true, indicates that the render bundle does not modify the stencil component of the GPURenderPassDepthStencilAttachment of any render pass the render bundle is executed in. See read-only depth-stencil.
	 *
	 */
	val stencilReadOnly: Boolean
}

/**
 * GPUQueueDescriptor describes a queue request.
 *
 * See [GPUQueueDescriptor in the WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuqueuedescriptor).
 *
 */
interface GPUQueueDescriptor : GPUObjectDescriptorBase
/**
 * Describes the query type and number of queries in a GPUQuerySet.
 *
 */
interface GPUQuerySetDescriptor : GPUObjectDescriptorBase {
	/**
	 * The type of queries managed by GPUQuerySet.
	 *
	 * See [GPUQuerySetDescriptor.type in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuquerysetdescriptor-type).
	 *
	 */
	val type: GPUQueryType
	/**
	 * The number of queries managed by GPUQuerySet.
	 *
	 * See [GPUQuerySetDescriptor.count in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuquerysetdescriptor-count).
	 *
	 */
	val count: GPUSize32
}

/**
 * Callback invoked when a GPUDevice reports an error that is not captured by an error scope.
 *
 */
fun interface GPUUncapturedErrorCallback {
	/**
	 * Receives an uncaptured GPU error.
	 *
	 */
	fun onUncapturedError(error: GPUError)
}

interface GPURequiredLimits {
	val maxTextureDimension1D: UInt?
	val maxTextureDimension2D: UInt?
	val maxTextureDimension3D: UInt?
	val maxTextureArrayLayers: UInt?
	val maxBindGroups: UInt?
	val maxBindGroupsPlusVertexBuffers: UInt?
	val maxImmediateSize: UInt?
	val maxBindingsPerBindGroup: UInt?
	val maxDynamicUniformBuffersPerPipelineLayout: UInt?
	val maxDynamicStorageBuffersPerPipelineLayout: UInt?
	val maxSampledTexturesPerShaderStage: UInt?
	val maxSamplersPerShaderStage: UInt?
	val maxStorageBuffersPerShaderStage: UInt?
	val maxStorageBuffersInVertexStage: UInt?
	val maxStorageBuffersInFragmentStage: UInt?
	val maxStorageTexturesPerShaderStage: UInt?
	val maxStorageTexturesInVertexStage: UInt?
	val maxStorageTexturesInFragmentStage: UInt?
	val maxUniformBuffersPerShaderStage: UInt?
	val maxUniformBufferBindingSize: ULong?
	val maxStorageBufferBindingSize: ULong?
	val minUniformBufferOffsetAlignment: UInt?
	val minStorageBufferOffsetAlignment: UInt?
	val maxVertexBuffers: UInt?
	val maxBufferSize: ULong?
	val maxVertexAttributes: UInt?
	val maxVertexBufferArrayStride: UInt?
	val maxInterStageShaderVariables: UInt?
	val maxColorAttachments: UInt?
	val maxColorAttachmentBytesPerSample: UInt?
	val maxComputeWorkgroupStorageSize: UInt?
	val maxComputeInvocationsPerWorkgroup: UInt?
	val maxComputeWorkgroupSizeX: UInt?
	val maxComputeWorkgroupSizeY: UInt?
	val maxComputeWorkgroupSizeZ: UInt?
	val maxComputeWorkgroupsPerDimension: UInt?
}
