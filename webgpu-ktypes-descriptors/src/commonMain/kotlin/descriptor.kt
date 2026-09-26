@file:Suppress("unused")
// This file has been generated DO NO EDIT
package io.ygdrasil.webgpu

/**
 * An RGBA color represented by four components.
 *
 */
data class Color(
	/**
	 * The red channel value.
	 *
	 */
	override val r: Double,
	/**
	 * The green channel value.
	 *
	 */
	override val g: Double,
	/**
	 * The blue channel value.
	 *
	 */
	override val b: Double,
	/**
	 * The alpha channel value.
	 *
	 */
	override val a: Double
): GPUColor

/**
 * A two-dimensional origin represented by x and y coordinates.
 *
 */
data class Origin2D(
	/**
	 * The X coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	override val x: GPUIntegerCoordinate = 0u,
	/**
	 * The Y coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	override val y: GPUIntegerCoordinate = 0u
): GPUOrigin2D

/**
 * A three-dimensional origin represented by x, y, and z coordinates.
 *
 */
data class Origin3D(
	/**
	 * The X coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	override val x: GPUIntegerCoordinate = 0u,
	/**
	 * The Y coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	override val y: GPUIntegerCoordinate = 0u,
	/**
	 * The Z coordinate of the origin; defaults to 0 when the origin is specified as a dictionary.
	 *
	 */
	override val z: GPUIntegerCoordinate = 0u
): GPUOrigin3D

/**
 * A three-dimensional extent represented by width, height, and depth or array-layer count.
 *
 */
data class Extent3D(
	/**
	 * The width of the extent.
	 *
	 */
	override val width: GPUIntegerCoordinate,
	/**
	 * The height of the extent.
	 *
	 */
	override val height: GPUIntegerCoordinate = 1u,
	/**
	 * The depth of the extent or the number of array layers it contains. If used with a GPUTexture with a GPUTextureDimension of "3d" defines the depth of the texture. If used with a GPUTexture with a GPUTextureDimension of "2d" defines the number of array layers in the texture.
	 *
	 */
	override val depthOrArrayLayers: GPUIntegerCoordinate = 1u
): GPUExtent3D

/**
 * Common label field shared by descriptors that create WebGPU objects.
 *
 */
data class ObjectDescriptorBase(
	/**
	 * The initial value of GPUObjectBase.label.
	 *
	 */
	override val label: String = ""
): GPUObjectDescriptorBase

/**
 * Options that constrain which GPU adapter is selected.
 *
 */
data class RequestAdapterOptions(
	/**
	 * Requests an adapter that supports at least a particular set of capabilities. This influences the [[default feature level]] of devices created from this adapter. The capabilities for each level are defined below, and the exact steps are defined in requestAdapter() and "a new device". If the implementation or system does not support all of the capabilities in the requested feature level, requestAdapter() will return null.
	 *
	 */
	override val featureLevel: String = "core",
	/**
	 * Optionally provides a hint indicating what class of adapter should be selected from the system’s available adapters. The value of this hint may influence which adapter is chosen, but it must not influence whether an adapter is returned or not.
	 *
	 */
	override val powerPreference: GPUPowerPreference? = null,
	/**
	 * When set to true indicates that only a fallback adapter may be returned. If the user agent does not support a fallback adapter, will cause requestAdapter() to resolve to null. Note: requestAdapter() may still return a fallback adapter if forceFallbackAdapter is set to false and either no other appropriate adapter is available or the user agent chooses to return a fallback adapter. Developers that wish to prevent their applications from running on fallback adapters should check the info.isFallbackAdapter attribute prior to requesting a GPUDevice.
	 *
	 */
	override val forceFallbackAdapter: Boolean = false,
	/**
	 * When set to true indicates that the best adapter for rendering to a WebXR session must be returned. If the user agent or system does not support WebXR sessions then adapter selection may ignore this value. Note: If xrCompatible is not set to true when the adapter is requested, GPUDevices created from the adapter cannot be used to render for WebXR sessions.
	 *
	 */
	override val xrCompatible: Boolean = false
): GPURequestAdapterOptions

/**
 * GPUDeviceDescriptor describes a device request.
 *
 */
data class DeviceDescriptor(
	/**
	 * Specifies the features that are required by the device request. The request will fail if the adapter cannot provide these features. Exactly the specified set of features, and no more or less, will be allowed in validation of API calls on the resulting device.
	 *
	 */
	override val requiredFeatures: List<GPUFeatureName> = emptyList(),
	/**
	 * Specifies the limits that are required by the device request. The request will fail if the adapter cannot provide these limits. Each key with a non-undefined value must be the name of a member of supported limits.
	 *
	 */
	override val requiredLimits: GPUSupportedLimits? = null,
	/**
	 * The descriptor for the default GPUQueue.
	 *
	 */
	override val defaultQueue: GPUQueueDescriptor = QueueDescriptor(),
	override val label: String = "",
	/**
	 * Callback invoked for errors that are not captured by an error scope.
	 *
	 */
	override val onUncapturedError: GPUUncapturedErrorCallback? = null
): GPUDeviceDescriptor

/**
 * Describes a GPUBuffer's size, usage, and optional mapped-at-creation state.
 *
 */
data class BufferDescriptor(
	/**
	 * The size of the buffer in bytes.
	 *
	 */
	override val size: GPUSize64,
	/**
	 * The allowed usages for the buffer.
	 *
	 */
	override val usage: GPUBufferUsage,
	/**
	 * If true creates the buffer in an already mapped state, allowing getMappedRange() to be called immediately. It is valid to set mappedAtCreation to true even if usage does not contain MAP_READ or MAP_WRITE. This can be used to set the buffer’s initial data. Guarantees that even if the buffer creation eventually fails, it will still appear as if the mapped range can be written/read to until it is unmapped.
	 *
	 */
	override val mappedAtCreation: Boolean = false,
	override val label: String = ""
): GPUBufferDescriptor

/**
 * Describes a GPUTexture's size, format, usage, and optional view formats.
 *
 */
data class TextureDescriptor(
	/**
	 * The width, height, and depth or layer count of the texture.
	 *
	 */
	override val size: GPUExtent3D,
	/**
	 * The format of the texture.
	 *
	 */
	override val format: GPUTextureFormat,
	/**
	 * The allowed usages for the texture.
	 *
	 */
	override val usage: GPUTextureUsage,
	/**
	 * The number of mip levels the texture will contain.
	 *
	 */
	override val mipLevelCount: GPUIntegerCoordinate = 1u,
	/**
	 * The sample count of the texture. A sampleCount > 1 indicates a multisampled texture.
	 *
	 */
	override val sampleCount: GPUSize32 = 1u,
	/**
	 * Whether the texture is one-dimensional, an array of two-dimensional layers, or three-dimensional.
	 *
	 */
	override val dimension: GPUTextureDimension = GPUTextureDimension.TwoD,
	/**
	 * Specifies what view format values will be allowed when calling createView() on this texture (in addition to the texture’s actual format). Formats in this list must be texture view format compatible with the texture format.
	 *
	 */
	override val viewFormats: List<GPUTextureFormat> = emptyList(),
	/**
	 * On devices with "core-features-and-limits", this is ignored, and there is no such restriction.
	 *
	 */
	override val textureBindingViewDimension: GPUTextureViewDimension? = null,
	override val label: String = ""
): GPUTextureDescriptor

/**
 * Describes the format, dimension, mip levels, array layers, and aspect of a GPUTextureView.
 *
 */
data class TextureViewDescriptor(
	/**
	 * The format of the texture view. Must be either the format of the texture or one of the viewFormats specified during its creation.
	 *
	 */
	override val format: GPUTextureFormat? = null,
	/**
	 * The dimension to view the texture as.
	 *
	 */
	override val dimension: GPUTextureViewDimension? = null,
	/**
	 * The allowed usage(s) for the texture view. Must be a subset of the usage flags of the texture. If 0, defaults to the full set of usage flags of the texture. Note: If the view’s format doesn’t support all of the texture’s usages, the default will fail, and the view’s usage must be specified explicitly.
	 *
	 */
	override val usage: GPUTextureUsage = GPUTextureUsage.None,
	/**
	 * Which aspect(s) of the texture are accessible to the texture view.
	 *
	 */
	override val aspect: GPUTextureAspect = GPUTextureAspect.All,
	/**
	 * The first (most detailed) mipmap level accessible to the texture view.
	 *
	 */
	override val baseMipLevel: GPUIntegerCoordinate = 0u,
	/**
	 * How many mipmap levels, starting with baseMipLevel, are accessible to the texture view.
	 *
	 */
	override val mipLevelCount: GPUIntegerCoordinate? = null,
	/**
	 * The index of the first array layer accessible to the texture view.
	 *
	 */
	override val baseArrayLayer: GPUIntegerCoordinate = 0u,
	/**
	 * How many array layers, starting with baseArrayLayer, are accessible to the texture view.
	 *
	 */
	override val arrayLayerCount: GPUIntegerCoordinate? = null,
	/**
	 * A string of length four, with each character mapping to the texture view’s red/green/blue/alpha channels, respectively. "r": Take its value from the red channel of the texture.
	 *
	 */
	override val swizzle: GPUTextureSwizzle = GPUTextureSwizzle(),
	override val label: String = ""
): GPUTextureViewDescriptor

/**
 * Describes the address modes, filtering modes, and level-of-detail limits for a GPUSampler.
 *
 */
data class SamplerDescriptor(
	/**
	 * Specifies the address modes for the texture width, height, and depth coordinates, respectively.
	 *
	 */
	override val addressModeU: GPUAddressMode = GPUAddressMode.ClampToEdge,
	/**
	 * Specifies the address modes for the texture width, height, and depth coordinates, respectively.
	 *
	 */
	override val addressModeV: GPUAddressMode = GPUAddressMode.ClampToEdge,
	/**
	 * Specifies the address modes for the texture width, height, and depth coordinates, respectively.
	 *
	 */
	override val addressModeW: GPUAddressMode = GPUAddressMode.ClampToEdge,
	/**
	 * Specifies the sampling behavior when the sampled area is smaller than or equal to one texel.
	 *
	 */
	override val magFilter: GPUFilterMode = GPUFilterMode.Nearest,
	/**
	 * Specifies the sampling behavior when the sampled area is larger than one texel.
	 *
	 */
	override val minFilter: GPUFilterMode = GPUFilterMode.Nearest,
	/**
	 * Specifies behavior for sampling between mipmap levels.
	 *
	 */
	override val mipmapFilter: GPUMipmapFilterMode = GPUMipmapFilterMode.Nearest,
	/**
	 * Specifies the minimum and maximum levels of detail, respectively, used internally when sampling a texture.
	 *
	 */
	override val lodMinClamp: Float = 0f,
	/**
	 * Specifies the minimum and maximum levels of detail, respectively, used internally when sampling a texture.
	 *
	 */
	override val lodMaxClamp: Float = 32f,
	/**
	 * When provided the sampler will be a comparison sampler with the specified GPUCompareFunction. Note: Comparison samplers may use filtering, but the sampling results will be implementation-dependent and may differ from the normal filtering rules.
	 *
	 */
	override val compare: GPUCompareFunction? = null,
	/**
	 * Specifies the maximum anisotropy value clamp used by the sampler. Anisotropic filtering is enabled when maxAnisotropy is > 1 and the implementation supports it. Anisotropic filtering improves the image quality of textures sampled at oblique viewing angles. Higher maxAnisotropy values indicate the maximum ratio of anisotropy supported when filtering.
	 *
	 */
	override val maxAnisotropy: UShort = 1u,
	override val label: String = ""
): GPUSamplerDescriptor

/**
 * Describes the entries that define a GPUBindGroupLayout.
 *
 */
data class BindGroupLayoutDescriptor(
	/**
	 * A list of entries describing the shader resource bindings for a bind group.
	 *
	 */
	override val entries: List<GPUBindGroupLayoutEntry>,
	override val label: String = ""
): GPUBindGroupLayoutDescriptor

/**
 * Describes one binding's index, shader visibility, and resource-specific layout.
 *
 */
data class BindGroupLayoutEntry(
	/**
	 * A unique identifier for a resource binding within the GPUBindGroupLayout, corresponding to a GPUBindGroupEntry.binding and a @binding attribute in the GPUShaderModule.
	 *
	 */
	override val binding: GPUIndex32,
	/**
	 * A bitset of the members of GPUShaderStage. Each set bit indicates that a GPUBindGroupLayoutEntry’s resource will be accessible from the associated shader stage.
	 *
	 */
	override val visibility: GPUShaderStage,
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	override val buffer: GPUBufferBindingLayout? = null,
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	override val sampler: GPUSamplerBindingLayout? = null,
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	override val texture: GPUTextureBindingLayout? = null,
	/**
	 * Exactly one of these members must be set, indicating the binding type. The contents of the member specify options specific to that type. The corresponding resource in createBindGroup() requires the corresponding binding resource type for this binding.
	 *
	 */
	override val storageTexture: GPUStorageTextureBindingLayout? = null
): GPUBindGroupLayoutEntry

/**
 * Describes the type, dynamic-offset behavior, and minimum size of a buffer binding.
 *
 */
data class BufferBindingLayout(
	/**
	 * Indicates the type required for buffers bound to this binding.
	 *
	 */
	override val type: GPUBufferBindingType = GPUBufferBindingType.Uniform,
	/**
	 * Indicates whether this binding requires a dynamic offset.
	 *
	 */
	override val hasDynamicOffset: Boolean = false,
	/**
	 * Indicates the minimum size of a buffer binding used with this bind point. Bindings are always validated against this size in createBindGroup().
	 *
	 */
	override val minBindingSize: GPUSize64 = 0u
): GPUBufferBindingLayout

/**
 * Describes the sampler binding type expected by a shader.
 *
 */
data class SamplerBindingLayout(
	/**
	 * Indicates the required type of a sampler bound to this binding.
	 *
	 */
	override val type: GPUSamplerBindingType = GPUSamplerBindingType.Filtering
): GPUSamplerBindingLayout

/**
 * Describes the sample type, view dimension, and multisampling of a texture binding.
 *
 */
data class TextureBindingLayout(
	/**
	 * Indicates the type required for texture views bound to this binding.
	 *
	 */
	override val sampleType: GPUTextureSampleType = GPUTextureSampleType.Float,
	/**
	 * Indicates the required dimension for texture views bound to this binding.
	 *
	 */
	override val viewDimension: GPUTextureViewDimension = GPUTextureViewDimension.TwoD,
	/**
	 * Indicates whether or not texture views bound to this binding must be multisampled.
	 *
	 */
	override val multisampled: Boolean = false
): GPUTextureBindingLayout

/**
 * Describes the access mode, format, and view dimension of a storage texture binding.
 *
 */
data class StorageTextureBindingLayout(
	/**
	 * The required format of texture views bound to this binding.
	 *
	 */
	override val format: GPUTextureFormat,
	/**
	 * The access mode for this binding, indicating readability and writability.
	 *
	 */
	override val access: GPUStorageTextureAccess = GPUStorageTextureAccess.WriteOnly,
	/**
	 * Indicates the required dimension for texture views bound to this binding.
	 *
	 */
	override val viewDimension: GPUTextureViewDimension = GPUTextureViewDimension.TwoD
): GPUStorageTextureBindingLayout

/**
 * Describes the layout and resources used to create a GPUBindGroup.
 *
 */
data class BindGroupDescriptor(
	/**
	 * The GPUBindGroupLayout the entries of this bind group will conform to.
	 *
	 */
	override val layout: GPUBindGroupLayout,
	/**
	 * A list of entries describing the resources to expose to the shader for each binding described by the layout.
	 *
	 */
	override val entries: List<GPUBindGroupEntry>,
	override val label: String = ""
): GPUBindGroupDescriptor

/**
 * Associates a binding index with the GPU resource supplied for that binding.
 *
 */
data class BindGroupEntry(
	/**
	 * A unique identifier for a resource binding within the GPUBindGroup, corresponding to a GPUBindGroupLayoutEntry.binding and a @binding attribute in the GPUShaderModule.
	 *
	 */
	override val binding: GPUIndex32,
	/**
	 * The resource to bind, which may be a GPUSampler, GPUTexture, GPUTextureView, GPUBuffer, GPUBufferBinding, or GPUExternalTexture.
	 *
	 */
	override val resource: GPUBindingResource
): GPUBindGroupEntry

/**
 * A buffer resource and the byte range exposed to a shader through a bind group.
 *
 */
data class BufferBinding(
	/**
	 * The GPUBuffer to bind.
	 *
	 */
	override val buffer: GPUBuffer,
	/**
	 * The offset, in bytes, from the beginning of buffer to the beginning of the range exposed to the shader by the buffer binding.
	 *
	 */
	override val offset: GPUSize64 = 0u,
	/**
	 * The size, in bytes, of the buffer binding. If not provided, specifies the range starting at offset and ending at the end of buffer.
	 *
	 */
	override val size: GPUSize64? = null
): GPUBufferBinding

/**
 * Lists the GPUBindGroupLayouts used by a pipeline.
 *
 */
data class PipelineLayoutDescriptor(
	/**
	 * A list of optional GPUBindGroupLayouts the pipeline will use. Each element corresponds to a @group attribute in the GPUShaderModule, with the Nth element corresponding with @group(N).
	 *
	 */
	override val bindGroupLayouts: List<GPUBindGroupLayout>,
	/**
	 * The size, in bytes, of the immediate data range used by the pipeline.
	 *
	 */
	override val immediateSize: GPUSize32 = 0u,
	override val label: String = ""
): GPUPipelineLayoutDescriptor

/**
 * Describes the shader code and compilation options used to create a GPUShaderModule.
 *
 */
data class ShaderModuleDescriptor(
	/**
	 * The WGSL source code for the shader module.
	 *
	 */
	override val code: String,
	/**
	 * A list of GPUShaderModuleCompilationHints. Any hint provided by an application should contain information about one entry point of a pipeline that will eventually be created from the entry point.
	 *
	 */
	override val compilationHints: List<GPUShaderModuleCompilationHint> = emptyList(),
	override val label: String = ""
): GPUShaderModuleDescriptor

/**
 * A hint associating a shader entry point with the pipeline layout expected to use it.
 *
 */
data class ShaderModuleCompilationHint(
	/**
	 * Name of the shader entry point associated with this compilation hint.
	 *
	 */
	override val entryPoint: String,
	/**
	 * A GPUPipelineLayout that the GPUShaderModule may be used with in a future createComputePipeline() or createRenderPipeline() call. If set to "auto" the layout will be the default pipeline layout for the entry point associated with this hint will be used.
	 *
	 */
	override val layout: GPUPipelineLayout? = null
): GPUShaderModuleCompilationHint

/**
 * Common label field shared by render and compute pipeline descriptors.
 *
 */
data class PipelineDescriptorBase(
	/**
	 * The GPUPipelineLayout for this pipeline, or "auto" to generate the pipeline layout automatically. Note: If "auto" is used the pipeline cannot share GPUBindGroups with any other pipelines.
	 *
	 */
	override val layout: GPUPipelineLayout? = null,
	override val label: String = ""
): GPUPipelineDescriptorBase

/**
 * A GPUProgrammableStage describes the entry point in the user-provided GPUShaderModule that controls one of the programmable stages of a pipeline. Entry point names follow the rules defined in WGSL identifier comparison.
 *
 */
data class ProgrammableStage(
	/**
	 * The GPUShaderModule containing the code that this programmable stage will execute.
	 *
	 */
	override val module: GPUShaderModule,
	/**
	 * The name of the function in module that this stage will use to perform its work. NOTE: Since the entryPoint dictionary member is not required, methods which consume a GPUProgrammableStage must use the "get the entry point" algorithm to determine which entry point it refers to.
	 *
	 */
	override val entryPoint: String? = null,
	/**
	 * Specifies the values of pipeline-overridable constants in the shader module module. Each such pipeline-overridable constant is uniquely identified by a single pipeline-overridable constant identifier string, representing the pipeline constant ID of the constant if its declaration specifies one, and otherwise the constant’s identifier name.
	 *
	 */
	override val constants: Map<String, GPUPipelineConstantValue> = emptyMap()
): GPUProgrammableStage

/**
 * Describes the layout and compute shader stage used to create a GPUComputePipeline.
 *
 */
data class ComputePipelineDescriptor(
	/**
	 * Describes the compute shader entry point of the pipeline.
	 *
	 */
	override val compute: GPUProgrammableStage,
	override val layout: GPUPipelineLayout? = null,
	override val label: String = ""
): GPUComputePipelineDescriptor

/**
 * Describes the layout and programmable stages and fixed-function state of a GPURenderPipeline.
 *
 */
data class RenderPipelineDescriptor(
	/**
	 * Describes the vertex shader entry point of the pipeline and its input buffer layouts.
	 *
	 */
	override val vertex: GPUVertexState,
	/**
	 * Describes the primitive-related properties of the pipeline.
	 *
	 */
	override val primitive: GPUPrimitiveState = PrimitiveState(),
	/**
	 * Describes the optional depth-stencil properties, including the testing, operations, and bias.
	 *
	 */
	override val depthStencil: GPUDepthStencilState? = null,
	/**
	 * Describes the multi-sampling properties of the pipeline.
	 *
	 */
	override val multisample: GPUMultisampleState = MultisampleState(),
	/**
	 * Describes the fragment shader entry point of the pipeline and its output colors. If not provided, the § 23.2.8 No Color Output mode is enabled.
	 *
	 */
	override val fragment: GPUFragmentState? = null,
	override val layout: GPUPipelineLayout? = null,
	override val label: String = ""
): GPURenderPipelineDescriptor

/**
 * Describes primitive topology, strip format, front face, and culling for a render pipeline.
 *
 */
data class PrimitiveState(
	/**
	 * The type of primitive to be constructed from the vertex inputs.
	 *
	 */
	override val topology: GPUPrimitiveTopology = GPUPrimitiveTopology.TriangleList,
	/**
	 * For pipelines with strip topologies ("line-strip" or "triangle-strip"), this determines the index buffer format and primitive restart value ("uint16"/0xFFFF or "uint32"/0xFFFFFFFF). It is not allowed on pipelines with non-strip topologies. Note: Some implementations require knowledge of the primitive restart value to compile pipeline state objects.
	 *
	 */
	override val stripIndexFormat: GPUIndexFormat? = null,
	/**
	 * Defines which polygons are considered front-facing.
	 *
	 */
	override val frontFace: GPUFrontFace = GPUFrontFace.CCW,
	/**
	 * Defines which polygon orientation will be culled, if any.
	 *
	 */
	override val cullMode: GPUCullMode = GPUCullMode.None,
	/**
	 * If true, indicates that depth clipping is disabled. Requires the "depth-clip-control" feature to be enabled.
	 *
	 */
	override val unclippedDepth: Boolean = false
): GPUPrimitiveState

/**
 * Describes sample count, sample mask, and alpha-to-coverage behavior.
 *
 */
data class MultisampleState(
	/**
	 * Number of samples per pixel. This GPURenderPipeline will be compatible only with attachment textures (colorAttachments and depthStencilAttachment) with matching sampleCounts.
	 *
	 */
	override val count: GPUSize32 = 1u,
	/**
	 * Mask determining which samples are written to.
	 *
	 */
	override val mask: GPUSampleMask = 0xFFFFFFFFu,
	/**
	 * When true indicates that a fragment’s alpha channel should be used to generate a sample coverage mask.
	 *
	 */
	override val alphaToCoverageEnabled: Boolean = false
): GPUMultisampleState

/**
 * Describes the fragment shader stage and its color targets.
 *
 */
data class FragmentState(
	/**
	 * A list of GPUColorTargetState defining the formats and behaviors of the color targets this pipeline writes to.
	 *
	 */
	override val targets: List<GPUColorTargetState>,
	override val module: GPUShaderModule,
	override val entryPoint: String? = null,
	override val constants: Map<String, GPUPipelineConstantValue> = emptyMap()
): GPUFragmentState

/**
 * Describes the format, write mask, and optional blending for a fragment color target.
 *
 */
data class ColorTargetState(
	/**
	 * The GPUTextureFormat of this color target. The pipeline will only be compatible with GPURenderPassEncoders which use a GPUTextureView of this format in the corresponding color attachment.
	 *
	 */
	override val format: GPUTextureFormat,
	/**
	 * The blending behavior for this color target. If left undefined, disables blending for this color target.
	 *
	 */
	override val blend: GPUBlendState? = null,
	/**
	 * Bitmask controlling which channels are are written to when drawing to this color target.
	 *
	 */
	override val writeMask: GPUColorWrite = GPUColorWrite.All
): GPUColorTargetState

/**
 * Describes the color and alpha blend components applied to a color target.
 *
 */
data class BlendState(
	/**
	 * Defines the blending behavior of the corresponding render target for color channels.
	 *
	 */
	override val color: GPUBlendComponent,
	/**
	 * Defines the blending behavior of the corresponding render target for the alpha channel.
	 *
	 */
	override val alpha: GPUBlendComponent
): GPUBlendState

/**
 * Describes one color or alpha blend component's operation and factors.
 *
 */
data class BlendComponent(
	/**
	 * Defines the GPUBlendOperation used to calculate the values written to the target attachment components.
	 *
	 */
	override val operation: GPUBlendOperation = GPUBlendOperation.Add,
	/**
	 * Defines the GPUBlendFactor operation to be performed on values from the fragment shader.
	 *
	 */
	override val srcFactor: GPUBlendFactor = GPUBlendFactor.One,
	/**
	 * Defines the GPUBlendFactor operation to be performed on values from the target attachment.
	 *
	 */
	override val dstFactor: GPUBlendFactor = GPUBlendFactor.Zero
): GPUBlendComponent

/**
 * Describes depth testing, stencil testing, and depth bias for a render pipeline.
 *
 */
data class DepthStencilState(
	/**
	 * The format of depthStencilAttachment this GPURenderPipeline will be compatible with.
	 *
	 */
	override val format: GPUTextureFormat,
	/**
	 * Indicates if this GPURenderPipeline can modify depthStencilAttachment depth values.
	 *
	 */
	override val depthWriteEnabled: Boolean? = null,
	/**
	 * The comparison operation used to test fragment depths against depthStencilAttachment depth values.
	 *
	 */
	override val depthCompare: GPUCompareFunction? = null,
	/**
	 * Defines how stencil comparisons and operations are performed for front-facing primitives.
	 *
	 */
	override val stencilFront: GPUStencilFaceState = StencilFaceState(),
	/**
	 * Defines how stencil comparisons and operations are performed for back-facing primitives.
	 *
	 */
	override val stencilBack: GPUStencilFaceState = StencilFaceState(),
	/**
	 * Bitmask controlling which depthStencilAttachment stencil value bits are read when performing stencil comparison tests.
	 *
	 */
	override val stencilReadMask: GPUStencilValue = 0xFFFFFFFFu,
	/**
	 * Bitmask controlling which depthStencilAttachment stencil value bits are written to when performing stencil operations.
	 *
	 */
	override val stencilWriteMask: GPUStencilValue = 0xFFFFFFFFu,
	/**
	 * Constant depth bias added to each triangle fragment. See biased fragment depth for details.
	 *
	 */
	override val depthBias: GPUDepthBias = 0,
	/**
	 * Depth bias that scales with the triangle fragment’s slope. See biased fragment depth for details.
	 *
	 */
	override val depthBiasSlopeScale: Float = 0f,
	/**
	 * The maximum depth bias of a triangle fragment. See biased fragment depth for details.
	 *
	 */
	override val depthBiasClamp: Float = 0f
): GPUDepthStencilState

/**
 * Describes stencil comparison and the operations applied to a stencil face.
 *
 */
data class StencilFaceState(
	/**
	 * The GPUCompareFunction used when testing the [[stencilReference]] value against the fragment’s depthStencilAttachment stencil values.
	 *
	 */
	override val compare: GPUCompareFunction = GPUCompareFunction.Always,
	/**
	 * The GPUStencilOperation performed if the fragment stencil comparison test described by compare fails.
	 *
	 */
	override val failOp: GPUStencilOperation = GPUStencilOperation.Keep,
	/**
	 * The GPUStencilOperation performed if the fragment depth comparison described by depthCompare fails.
	 *
	 */
	override val depthFailOp: GPUStencilOperation = GPUStencilOperation.Keep,
	/**
	 * The GPUStencilOperation performed if the fragment stencil comparison test described by compare passes.
	 *
	 */
	override val passOp: GPUStencilOperation = GPUStencilOperation.Keep
): GPUStencilFaceState

/**
 * Describes the vertex shader stage and vertex buffer layouts for a render pipeline.
 *
 */
data class VertexState(
	override val module: GPUShaderModule,
	/**
	 * A list of GPUVertexBufferLayouts, each defining the layout of vertex attribute data in a vertex buffer used by this pipeline.
	 *
	 */
	override val buffers: List<GPUVertexBufferLayout> = emptyList(),
	override val entryPoint: String? = null,
	override val constants: Map<String, GPUPipelineConstantValue> = emptyMap()
): GPUVertexState

/**
 * Describes the stride, step mode, and attributes of a vertex buffer.
 *
 */
data class VertexBufferLayout(
	/**
	 * The stride, in bytes, between elements of this array.
	 *
	 */
	override val arrayStride: GPUSize64,
	/**
	 * An array defining the layout of the vertex attributes within each element.
	 *
	 */
	override val attributes: List<GPUVertexAttribute>,
	/**
	 * Whether each element of this array represents per-vertex data or per-instance data
	 *
	 */
	override val stepMode: GPUVertexStepMode = GPUVertexStepMode.Vertex
): GPUVertexBufferLayout

/**
 * Describes one vertex attribute's format and byte offset in a vertex buffer.
 *
 */
data class VertexAttribute(
	/**
	 * The GPUVertexFormat of the attribute.
	 *
	 */
	override val format: GPUVertexFormat,
	/**
	 * The offset, in bytes, from the beginning of the element to the data for the attribute.
	 *
	 */
	override val offset: GPUSize64,
	/**
	 * The numeric location associated with this attribute, which will correspond with a "@location" attribute declared in the vertex.module.
	 *
	 */
	override val shaderLocation: GPUIndex32
): GPUVertexAttribute

/**
 * "GPUTexelCopyBufferLayout" describes the "layout" of texels in a "buffer" of bytes (GPUBuffer or AllowSharedBufferSource) in a "texel copy" operation.
 *
 */
data class TexelCopyBufferLayout(
	/**
	 * The offset, in bytes, from the beginning of the texel data source (such as a GPUTexelCopyBufferInfo.buffer) to the start of the texel data within that source.
	 *
	 */
	override val offset: GPUSize64 = 0u,
	/**
	 * The stride, in bytes, between the beginning of each texel block row and the subsequent texel block row. Required if there are multiple texel block rows (i.e. the copy height or depth is more than one block).
	 *
	 */
	override val bytesPerRow: GPUSize32? = null,
	/**
	 * Number of texel block rows per single texel image of the texture. rowsPerImage × bytesPerRow is the stride, in bytes, between the beginning of each texel image of data and the subsequent texel image. Required if there are multiple texel images (i.e. the copy depth is more than one).
	 *
	 */
	override val rowsPerImage: GPUSize32? = null
): GPUTexelCopyBufferLayout

/**
 * "GPUTexelCopyBufferInfo" describes the "info" (GPUBuffer and GPUTexelCopyBufferLayout) about a "buffer" source or destination of a "texel copy" operation. Together with the copySize, it describes the footprint of a region of texels in a GPUBuffer.
 *
 */
data class TexelCopyBufferInfo(
	/**
	 * A buffer which either contains texel data to be copied or will store the texel data being copied, depending on the method it is being passed to.
	 *
	 */
	override val buffer: GPUBuffer,
	override val offset: GPUSize64 = 0u,
	override val bytesPerRow: GPUSize32? = null,
	override val rowsPerImage: GPUSize32? = null
): GPUTexelCopyBufferInfo

/**
 * "GPUTexelCopyTextureInfo" describes the "info" (GPUTexture, etc.) about a "texture" source or destination of a "texel copy" operation. Together with the copySize, it describes a sub-region of a texture (spanning one or more contiguous texture subresources at the same mip-map level).
 *
 */
data class TexelCopyTextureInfo(
	/**
	 * Texture to copy to/from.
	 *
	 */
	override val texture: GPUTexture,
	/**
	 * Mip-map level of the texture to copy to/from.
	 *
	 */
	override val mipLevel: GPUIntegerCoordinate = 0u,
	/**
	 * Defines the origin of the copy - the minimum corner of the texture sub-region to copy to/from. Together with copySize, defines the full copy sub-region.
	 *
	 */
	override val origin: GPUOrigin3D = Origin3D(),
	/**
	 * Defines which aspects of the texture to copy to/from.
	 *
	 */
	override val aspect: GPUTextureAspect = GPUTextureAspect.All
): GPUTexelCopyTextureInfo

/**
 * Options used when finishing a GPUCommandEncoder to create a GPUCommandBuffer.
 *
 */
data class CommandBufferDescriptor(
	override val label: String = ""
): GPUCommandBufferDescriptor

/**
 * Options used to create a GPUCommandEncoder.
 *
 */
data class CommandEncoderDescriptor(
	override val label: String = ""
): GPUCommandEncoderDescriptor

/**
 * Selects query slots for compute-pass beginning and end timestamps.
 *
 */
data class ComputePassTimestampWrites(
	/**
	 * The GPUQuerySet, of type "timestamp", that the query results will be written to.
	 *
	 */
	override val querySet: GPUQuerySet,
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the beginning of the compute pass will be written.
	 *
	 */
	override val beginningOfPassWriteIndex: GPUSize32? = null,
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the end of the compute pass will be written.
	 *
	 */
	override val endOfPassWriteIndex: GPUSize32? = null
): GPUComputePassTimestampWrites

/**
 * Options for beginning a compute pass, including optional timestamp writes.
 *
 */
data class ComputePassDescriptor(
	/**
	 * Defines which timestamp values will be written for this pass, and where to write them to.
	 *
	 */
	override val timestampWrites: GPUComputePassTimestampWrites? = null,
	override val label: String = ""
): GPUComputePassDescriptor

/**
 * Selects query slots for render-pass beginning, end, and optional per-stage timestamps.
 *
 */
data class RenderPassTimestampWrites(
	/**
	 * The GPUQuerySet, of type "timestamp", that the query results will be written to.
	 *
	 */
	override val querySet: GPUQuerySet,
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the beginning of the render pass will be written.
	 *
	 */
	override val beginningOfPassWriteIndex: GPUSize32? = null,
	/**
	 * If defined, indicates the query index in querySet into which the timestamp at the end of the render pass will be written.
	 *
	 */
	override val endOfPassWriteIndex: GPUSize32? = null
): GPURenderPassTimestampWrites

/**
 * Describes the color, depth/stencil, and optional timestamp attachments for a render pass.
 *
 */
data class RenderPassDescriptor(
	/**
	 * The set of GPURenderPassColorAttachment values in this sequence defines which color attachments will be output to when executing this render pass. Due to usage compatibility, no color attachment may alias another attachment or any resource used inside the render pass.
	 *
	 */
	override val colorAttachments: List<GPURenderPassColorAttachment>,
	/**
	 * The GPURenderPassDepthStencilAttachment value that defines the depth/stencil attachment that will be output to and tested against when executing this render pass. Due to usage compatibility, no writable depth/stencil attachment may alias another attachment or any resource used inside the render pass.
	 *
	 */
	override val depthStencilAttachment: GPURenderPassDepthStencilAttachment? = null,
	/**
	 * The GPUQuerySet value defines where the occlusion query results will be stored for this pass.
	 *
	 */
	override val occlusionQuerySet: GPUQuerySet? = null,
	/**
	 * Defines which timestamp values will be written for this pass, and where to write them to.
	 *
	 */
	override val timestampWrites: GPURenderPassTimestampWrites? = null,
	/**
	 * The maximum number of draw calls that will be done in the render pass. Used by some implementations to size work injected before the render pass. Keeping the default value is a good default, unless it is known that more draw calls will be done.
	 *
	 */
	override val maxDrawCount: GPUSize64 = 50000000u,
	override val label: String = ""
): GPURenderPassDescriptor

/**
 * Describes a color attachment and its load, store, and resolve operations for a render pass.
 *
 */
data class RenderPassColorAttachment(
	/**
	 * Describes the texture subresource that will be output to for this color attachment. The subresource is determined by calling get as texture view(view).
	 *
	 */
	override val view: GPUTextureOrGPUTextureView,
	/**
	 * Indicates the load operation to perform on view prior to executing the render pass. Note: It is recommended to prefer clearing; see "clear" for details.
	 *
	 */
	override val loadOp: GPULoadOp,
	/**
	 * The store operation to perform on view after executing the render pass.
	 *
	 */
	override val storeOp: GPUStoreOp,
	/**
	 * Indicates the depth slice index of "3d" view that will be output to for this color attachment.
	 *
	 */
	override val depthSlice: GPUIntegerCoordinate? = null,
	/**
	 * Describes the texture subresource that will receive the resolved output for this color attachment if view is multisampled. The subresource is determined by calling get as texture view(resolveTarget).
	 *
	 */
	override val resolveTarget: GPUTextureOrGPUTextureView? = null,
	/**
	 * Indicates the value to clear view to prior to executing the render pass. If not provided, defaults to {r: 0, g: 0, b: 0, a: 0}. Ignored if loadOp is not "clear". The components of clearValue are all double values. They are converted to a texel value of texture format matching the render attachment. If conversion fails, a validation error is generated.
	 *
	 */
	override val clearValue: GPUColor? = null
): GPURenderPassColorAttachment

/**
 * Describes the depth and stencil operations for a render pass attachment.
 *
 */
data class RenderPassDepthStencilAttachment(
	/**
	 * Describes the texture subresource that will be output to and read from for this depth/stencil attachment. The subresource is determined by calling get as texture view(view).
	 *
	 */
	override val view: GPUTextureOrGPUTextureView,
	/**
	 * Indicates the value to clear view’s depth component to prior to executing the render pass. Ignored if depthLoadOp is not "clear". Must be between 0.0 and 1.0, inclusive.
	 *
	 */
	override val depthClearValue: Float? = null,
	/**
	 * Indicates the load operation to perform on view’s depth component prior to executing the render pass. Note: It is recommended to prefer clearing; see "clear" for details.
	 *
	 */
	override val depthLoadOp: GPULoadOp? = null,
	/**
	 * The store operation to perform on view’s depth component after executing the render pass.
	 *
	 */
	override val depthStoreOp: GPUStoreOp? = null,
	/**
	 * Indicates that the depth component of view is read only.
	 *
	 */
	override val depthReadOnly: Boolean = false,
	/**
	 * Indicates the value to clear view’s stencil component to prior to executing the render pass. Ignored if stencilLoadOp is not "clear". The value will be converted to the type of the stencil aspect of view by taking the same number of LSBs as the number of bits in the stencil aspect of one texel of view.
	 *
	 */
	override val stencilClearValue: GPUStencilValue = 0u,
	/**
	 * Indicates the load operation to perform on view’s stencil component prior to executing the render pass. Note: It is recommended to prefer clearing; see "clear" for details.
	 *
	 */
	override val stencilLoadOp: GPULoadOp? = null,
	/**
	 * The store operation to perform on view’s stencil component after executing the render pass.
	 *
	 */
	override val stencilStoreOp: GPUStoreOp? = null,
	/**
	 * Indicates that the stencil component of view is read only.
	 *
	 */
	override val stencilReadOnly: Boolean = false
): GPURenderPassDepthStencilAttachment

/**
 * Describes the attachment formats and sample count used by a render pass or render bundle.
 *
 */
data class RenderPassLayout(
	/**
	 * A list of the GPUTextureFormats of the color attachments for this pass or bundle.
	 *
	 */
	override val colorFormats: List<GPUTextureFormat>,
	/**
	 * The GPUTextureFormat of the depth/stencil attachment for this pass or bundle.
	 *
	 */
	override val depthStencilFormat: GPUTextureFormat? = null,
	/**
	 * Number of samples per pixel in the attachments for this pass or bundle.
	 *
	 */
	override val sampleCount: GPUSize32 = 1u,
	override val label: String = ""
): GPURenderPassLayout

/**
 * Options used when finishing a GPURenderBundleEncoder to create a GPURenderBundle.
 *
 */
data class RenderBundleDescriptor(
	override val label: String = ""
): GPURenderBundleDescriptor

/**
 * Options used to create a GPURenderBundleEncoder.
 *
 */
data class RenderBundleEncoderDescriptor(
	override val colorFormats: List<GPUTextureFormat>,
	/**
	 * If true, indicates that the render bundle does not modify the depth component of the GPURenderPassDepthStencilAttachment of any render pass the render bundle is executed in. See read-only depth-stencil.
	 *
	 */
	override val depthReadOnly: Boolean = false,
	/**
	 * If true, indicates that the render bundle does not modify the stencil component of the GPURenderPassDepthStencilAttachment of any render pass the render bundle is executed in. See read-only depth-stencil.
	 *
	 */
	override val stencilReadOnly: Boolean = false,
	override val depthStencilFormat: GPUTextureFormat? = null,
	override val sampleCount: GPUSize32 = 1u,
	override val label: String = ""
): GPURenderBundleEncoderDescriptor

/**
 * GPUQueueDescriptor describes a queue request.
 *
 */
data class QueueDescriptor(
	override val label: String = ""
): GPUQueueDescriptor

/**
 * Describes the query type and number of queries in a GPUQuerySet.
 *
 */
data class QuerySetDescriptor(
	/**
	 * The type of queries managed by GPUQuerySet.
	 *
	 */
	override val type: GPUQueryType,
	/**
	 * The number of queries managed by GPUQuerySet.
	 *
	 */
	override val count: GPUSize32,
	override val label: String = ""
): GPUQuerySetDescriptor
