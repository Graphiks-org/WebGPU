@file:Suppress("unused")
// This file has been generated DO NO EDIT
package io.ygdrasil.webgpu

/**
 * Represents a color in the RGBA format, which can be either a sequence of four `Double` values or a [GPUColorDict]. This interface provides access to the red, green, blue, and alpha channel values.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpucolor).
 * 
 */
data class Color(
	/**
	 * The red channel value of the color. This value is a `Double` representing the intensity of the red component in the RGBA color model.
	 * 
	 * **Type**: Double
	 * 
	 */
	override val r: Double,
	/**
	 * The green channel value of the color. This value is a `Double` representing the intensity of the green component in the RGBA color model.
	 * 
	 * **Type**: Double
	 * 
	 */
	override val g: Double,
	/**
	 * The blue channel value of the color. This value is a `Double` representing the intensity of the blue component in the RGBA color model.
	 * 
	 * **Type**: Double
	 * 
	 */
	override val b: Double,
	/**
	 * The alpha channel value of the color. This value is a `Double` representing the opacity of the color, where 0.0 means fully transparent and 1.0 means fully opaque.
	 * 
	 * **Type**: Double
	 * 
	 */
	override val a: Double
): GPUColor

/**
 * Represents a 2D origin point in GPU coordinates. This interface can be used to specify the starting point for various GPU operations, such as texture sampling or buffer updates.
 * 
 * The `GPUOrigin2D` type is defined as either a sequence of two values or a dictionary with `x` and `y` properties. This allows for flexible initialization and usage in different contexts.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuorigin2d).
 * 
 */
data class Origin2D(
	/**
	 * The x-coordinate of the origin point. This value is of type `GPUIntegerCoordinate`.
	 * 
	 * - **Type**: `GPUIntegerCoordinate`
	 * - **Default Value**: 0
	 * 
	 * When using a sequence to represent `GPUOrigin2D`, this property refers to the first item in the sequence. If the sequence does not contain an item, the default value of 0 is used.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuorigin2ddict-x).
	 * 
	 */
	override val x: GPUIntegerCoordinate = 0u,
	/**
	 * The y-coordinate of the origin point. This value is of type `GPUIntegerCoordinate`.
	 * 
	 * - **Type**: `GPUIntegerCoordinate`
	 * - **Default Value**: 0
	 * 
	 * When using a sequence to represent `GPUOrigin2D`, this property refers to the second item in the sequence. If the sequence does not contain an item, the default value of 0 is used.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuorigin2ddict-y).
	 * 
	 */
	override val y: GPUIntegerCoordinate = 0u
): GPUOrigin2D

/**
 * Represents a 3D origin point in GPU coordinates. This interface can be used to specify the starting point for various GPU operations, such as texture sampling or buffer updates.
 * 
 * The `GPUOrigin3D` type can be either a sequence of three [GPUIntegerCoordinate] values or an instance of [GPUOrigin3DDict]. When accessed, the properties `x`, `y`, and `z` will refer to the corresponding values in the sequence or dictionary.
 * 
 * For more details, see the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuorigin3d).
 * 
 */
data class Origin3D(
	/**
	 * The x-coordinate of the 3D origin point. This value is either the first item in a sequence of [GPUIntegerCoordinate] values or the `x` property of a [GPUOrigin3DDict].
	 * 
	 * **Type:** [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate)
	 * 
	 * **Default Value:** 0
	 * 
	 */
	override val x: GPUIntegerCoordinate = 0u,
	/**
	 * The y-coordinate of the 3D origin point. This value is either the second item in a sequence of [GPUIntegerCoordinate] values or the `y` property of a [GPUOrigin3DDict].
	 * 
	 * **Type:** [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate)
	 * 
	 * **Default Value:** 0
	 * 
	 */
	override val y: GPUIntegerCoordinate = 0u,
	/**
	 * The z-coordinate of the 3D origin point. This value is either the third item in a sequence of [GPUIntegerCoordinate] values or the `z` property of a [GPUOrigin3DDict].
	 * 
	 * **Type:** [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate)
	 * 
	 * **Default Value:** 0
	 * 
	 */
	override val z: GPUIntegerCoordinate = 0u
): GPUOrigin3D

/**
 * Represents a 3-dimensional extent, which defines the size of a texture or other GPU resources. This interface can be used to specify dimensions in three axes: width, height, and depth or array layers.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuextent3d).
 * 
 * @see [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict)
 * 
 */
data class Extent3D(
	/**
	 * The width of the extent.
	 * 
	 * This property corresponds to the `width` field in the [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict).
	 * 
	 * @return The width as a [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	override val width: GPUIntegerCoordinate,
	/**
	 * The height of the extent.
	 * 
	 * This property corresponds to the `height` field in the [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict), which defaults to 1 if not specified.
	 * 
	 * @return The height as a [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	override val height: GPUIntegerCoordinate = 1u,
	/**
	 * The depth of the extent or the number of array layers it contains.
	 * 
	 * This property corresponds to the `depthOrArrayLayers` field in the [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict), which defaults to 1 if not specified. If used with a [GPUTexture](https://www.w3.org/TR/webgpu/#gputexture) with a dimension of `"3d"`, it defines the depth of the texture. If used with a `GPUTexture` with a dimension of `"2d"`, it defines the number of array layers in the texture.
	 * 
	 * @return The depth or number of array layers as a [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	override val depthOrArrayLayers: GPUIntegerCoordinate = 1u
): GPUExtent3D

/**
 * Represents the base descriptor for GPU objects. This interface is used to provide a common structure for labeling GPU objects, which can be helpful for debugging and identification purposes.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase).
 * 
 */
data class ObjectDescriptorBase(
	/**
	 * A string that labels the GPU object. This label can be used for debugging purposes to identify the object.
	 * 
	 * **Type**: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/)
	 * 
	 * **Default Value**: An empty string (`""`)
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuobjectdescriptorbase-label).
	 * 
	 */
	override val label: String = ""
): GPUObjectDescriptorBase

/**
 * The `GPURequestAdapterOptions` interface provides hints to the user agent indicating what configuration is suitable for the application. This interface allows developers to specify preferences and constraints for the GPU adapter selection process.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpurequestadapteroptions).
 * 
 */
data class RequestAdapterOptions(
	/**
	 * The `featureLevel` property specifies the feature level for the adapter request. This string value influences which features of the GPU are enabled or restricted.
	 * 
	 * **Allowed Values:**
	 * - "core": No effect.
	 * - "compatibility": Reserved for future use to opt into additional validation restrictions. Applications should not use this value at this time.
	 * 
	 * **Default Value:** "core"
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurequestadapteroptions-featurelevel).
	 * 
	 */
	override val featureLevel: String = "core",
	/**
	 * The `powerPreference` property provides a hint indicating what class of adapter should be selected from the system’s available adapters. This value can influence which GPU is used in a multi-GPU system, affecting power consumption and performance.
	 * 
	 * **Allowed Values:**
	 * - null: Provides no hint to the user agent.
	 * - "low-power": Indicates a request to prioritize power savings over performance.
	 * - "high-performance": Indicates a request to prioritize performance over power consumption.
	 * 
	 * **Default Value:** null
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurequestadapteroptions-powerpreference).
	 * 
	 */
	override val powerPreference: GPUPowerPreference? = null,
	/**
	 * The `forceFallbackAdapter` property indicates whether only a fallback adapter may be returned. If set to `true`, the user agent will return a fallback adapter if available, or `null` if not supported.
	 * 
	 * **Default Value:** false
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurequestadapteroptions-forcefallbackadapter).
	 * 
	 */
	override val forceFallbackAdapter: Boolean = false,
	/**
	 * The `xrCompatible` property indicates whether the best adapter for rendering to a WebXR session must be returned. If set to `true`, the user agent will prioritize adapters suitable for WebXR rendering.
	 * 
	 * **Default Value:** false
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurequestadapteroptions-xrcompatible).
	 * 
	 */
	override val xrCompatible: Boolean = false
): GPURequestAdapterOptions

/**
 * The `GPUDeviceDescriptor` interface describes a device request. It specifies the features, limits, and default queue descriptor required by the GPU device.
 * 
 * This interface is used to configure the creation of a [GPUDevice] object, which represents a GPU adapter and provides methods for creating GPU resources such as buffers, textures, and pipelines.
 * 
 * For more details, refer to the [WebGPU specification on `GPUDeviceDescriptor`](https://www.w3.org/TR/webgpu/#gpudevicedescriptor).
 * 
 */
data class DeviceDescriptor(
	/**
	 * Specifies the features that are required by the device request. The request will fail if the adapter cannot provide these features.
	 * 
	 * Exactly the specified set of features, and no more or less, will be allowed in validation of API calls on the resulting device.
	 * 
	 * **Type:** `List<GPUFeatureName>`
	 * 
	 * **Default Value:** An empty list
	 * 
	 * For more details, refer to the [WebGPU specification on `requiredFeatures`](https://www.w3.org/TR/webgpu/#dom-gpudevicedescriptor-requiredfeatures).
	 * 
	 */
	override val requiredFeatures: List<GPUFeatureName> = emptyList(),
	/**
	 * Specifies the limits that are required by the device request. The request will fail if the adapter cannot provide these limits.
	 * 
	 * Each key with a non-`undefined` value must be the name of a member of [supported limits](https://www.w3.org/TR/webgpu/#supported-limits).
	 * 
	 * API calls on the resulting device perform validation according to the exact limits of the device (not the adapter; see [§ 3.6.2 Limits](https://www.w3.org/TR/webgpu/#limits)).
	 * 
	 * **Type:** `GPUSupportedLimits?`
	 * 
	 * **Default Value:** An empty map
	 * 
	 * For more details, refer to the [WebGPU specification on `requiredLimits`](https://www.w3.org/TR/webgpu/#dom-gpudevicedescriptor-requiredlimits).
	 * 
	 */
	override val requiredLimits: GPUSupportedLimits? = null,
	/**
	 * The descriptor for the default [GPUQueue].
	 * 
	 * **Type:** `GPUQueueDescriptor`
	 * 
	 * **Default Value:** An empty `GPUQueueDescriptor`
	 * 
	 * For more details, refer to the [WebGPU specification on `defaultQueue`](https://www.w3.org/TR/webgpu/#dom-gpudevicedescriptor-defaultqueue).
	 * 
	 */
	override val defaultQueue: GPUQueueDescriptor = QueueDescriptor(),
	override val label: String = "",
	/**
	 * Describes the `onUncapturedError` member of `GPUDeviceDescriptor`.
	 * 
	 */
	override val onUncapturedError: GPUUncapturedErrorCallback? = null
): GPUDeviceDescriptor

/**
 * Represents a descriptor for creating GPU buffers. This interface extends [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#gpuobjectdescriptorbase) and is used to specify the properties of a buffer, such as its size, usage flags, and whether it should be mapped at creation.
 * 
 * For more details, refer to the [WebGPU specification on GPUBufferDescriptor](https://www.w3.org/TR/webgpu/#gpubufferdescriptor).
 * 
 */
data class BufferDescriptor(
	/**
	 * The size of the buffer in bytes. This value must be a multiple of 4 and greater than or equal to 4.
	 * 
	 * **Type:** [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	override val size: GPUSize64,
	/**
	 * Specifies the allowed usages for the buffer. This is a bitmask of [GPUBufferUsageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gpubufferusageflags) that indicates how the buffer will be used.
	 * 
	 * **Type:** [GPUBufferUsageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gpubufferusageflags)
	 * 
	 */
	override val usage: GPUBufferUsage,
	/**
	 * Indicates whether the buffer should be created in an already mapped state. If `true`, the buffer can be immediately accessed using [getMappedRange()](https://www.w3.org/TR/webgpu/#dom-gpubuffer-getmappedrange). This is useful for setting the buffer's initial data.
	 * 
	 * **Type:** Boolean
	 * 
	 * **Default Value:** `false`
	 * 
	 */
	override val mappedAtCreation: Boolean = false,
	override val label: String = ""
): GPUBufferDescriptor

/**
 * Represents a descriptor for creating GPU textures. This interface extends [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#gpuobjectdescriptorbase) and defines the properties required to specify the characteristics of a texture.
 * 
 * For more details, refer to the [WebGPU specification on GPUTextureDescriptor](https://www.w3.org/TR/webgpu/#gputexturedescriptor).
 * 
 */
data class TextureDescriptor(
	/**
	 * Specifies the size of the texture in 3D space. This is a required property.
	 * 
	 * **Type:** [GPUExtent3D]
	 * 
	 */
	override val size: GPUExtent3D,
	/**
	 * Specifies the format of the texture data. This is a required property.
	 * 
	 * **Type:** [GPUTextureFormat]
	 * 
	 */
	override val format: GPUTextureFormat,
	/**
	 * Specifies the usage flags for the texture. This is a required property.
	 * 
	 * **Type:** [GPUTextureUsageFlags]
	 * 
	 */
	override val usage: GPUTextureUsage,
	/**
	 * Specifies the number of mipmap levels in the texture. The default value is 1.
	 * 
	 * **Type:** [GPUIntegerCoordinate]
	 * 
	 */
	override val mipLevelCount: GPUIntegerCoordinate = 1u,
	/**
	 * Specifies the number of samples for multisampling. The default value is 1.
	 * 
	 * **Type:** [GPUSize32]
	 * 
	 */
	override val sampleCount: GPUSize32 = 1u,
	/**
	 * Specifies the dimension of the texture. The default value is "2d".
	 * 
	 * **Type:** [GPUTextureDimension]
	 * 
	 */
	override val dimension: GPUTextureDimension = GPUTextureDimension.TwoD,
	/**
	 * Specifies a list of formats that can be used to create views of the texture. The default value is an empty list.
	 * 
	 * **Type:** List<[GPUTextureFormat]>
	 * 
	 */
	override val viewFormats: List<GPUTextureFormat> = emptyList(),
	override val label: String = ""
): GPUTextureDescriptor

/**
 * The `GPUTextureViewDescriptor` interface defines a set of properties that describe how to create a view on a texture. This descriptor is used when creating a [GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview) object, which represents a specific way to access the data in a texture.
 * 
 * A texture view allows for different formats, dimensions, and usages of the underlying texture data. This is particularly useful for scenarios where you need to access the same texture data in multiple ways without duplicating the actual texture data.
 * 
 */
data class TextureViewDescriptor(
	/**
	 * `format` specifies the format of the texture view. This must be either the `format` of the texture or one of the `viewFormats` specified during its creation.
	 * 
	 * See also: [WebGPU Specification - GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat).
	 * 
	 */
	override val format: GPUTextureFormat? = null,
	/**
	 * `dimension` specifies the dimension to view the texture as. This property determines how the texture will be interpreted in terms of its dimensionality (e.g., 1D, 2D, or 3D).
	 * 
	 * See also: [WebGPU Specification - GPUTextureViewDimension](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	override val dimension: GPUTextureViewDimension? = null,
	/**
	 * `usage` specifies the allowed usages for the texture view. This must be a subset of the `usage` flags of the texture. If set to 0, it defaults to the full set of usage flags of the texture.
	 * 
	 * See also: [WebGPU Specification - GPUTextureUsageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gputextureusageflags).
	 * 
	 */
	override val usage: GPUTextureUsage = GPUTextureUsage.None,
	/**
	 * `aspect` specifies which aspects of the texture are accessible to the texture view. This property determines whether the view can access color, depth, stencil, or all aspects of the texture.
	 * 
	 * See also: [WebGPU Specification - GPUTextureAspect](https://www.w3.org/TR/webgpu/#enumdef-gputextureaspect).
	 * 
	 */
	override val aspect: GPUTextureAspect = GPUTextureAspect.All,
	/**
	 * `baseMipLevel` specifies the first (most detailed) mipmap level accessible to the texture view. This property defines the starting point for mipmap levels that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	override val baseMipLevel: GPUIntegerCoordinate = 0u,
	/**
	 * `mipLevelCount` specifies how many mipmap levels, starting with `baseMipLevel`, are accessible to the texture view. This property defines the range of mipmap levels that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	override val mipLevelCount: GPUIntegerCoordinate? = null,
	/**
	 * `baseArrayLayer` specifies the index of the first array layer accessible to the texture view. This property defines the starting point for array layers that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	override val baseArrayLayer: GPUIntegerCoordinate = 0u,
	/**
	 * `arrayLayerCount` specifies how many array layers, starting with `baseArrayLayer`, are accessible to the texture view. This property defines the range of array layers that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	override val arrayLayerCount: GPUIntegerCoordinate? = null,
	override val label: String = ""
): GPUTextureViewDescriptor

/**
 * The `GPUSamplerDescriptor` interface defines the properties of a sampler used in WebGPU. This descriptor specifies how textures are sampled during rendering, including addressing modes, filtering modes, and level-of-detail (LOD) clamping. It is part of the GPUObjectDescriptorBase hierarchy and is used to create `GPUSampler` objects.
 * 
 * For more details, refer to the [WebGPU specification on GPUSamplerDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpusamplerdescriptor).
 * 
 */
data class SamplerDescriptor(
	/**
	 * Specifies the addressing mode for the U coordinate of the texture. This determines how texture coordinates are handled when they extend beyond the bounds of the texture. The default value is `GPUAddressMode.CLAMP_TO_EDGE`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUAddressMode](https://www.w3.org/TR/webgpu/#enumdef-gpuaddressmode).
	 * 
	 */
	override val addressModeU: GPUAddressMode = GPUAddressMode.ClampToEdge,
	/**
	 * Specifies the addressing mode for the V coordinate of the texture. This determines how texture coordinates are handled when they extend beyond the bounds of the texture. The default value is `GPUAddressMode.CLAMP_TO_EDGE`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUAddressMode](https://www.w3.org/TR/webgpu/#enumdef-gpuaddressmode).
	 * 
	 */
	override val addressModeV: GPUAddressMode = GPUAddressMode.ClampToEdge,
	/**
	 * Specifies the addressing mode for the W coordinate of the texture. This determines how texture coordinates are handled when they extend beyond the bounds of the texture. The default value is `GPUAddressMode.CLAMP_TO_EDGE`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUAddressMode](https://www.w3.org/TR/webgpu/#enumdef-gpuaddressmode).
	 * 
	 */
	override val addressModeW: GPUAddressMode = GPUAddressMode.ClampToEdge,
	/**
	 * Specifies the filtering mode used when the sampled area is smaller than or equal to one texel. The default value is `GPUFilterMode.NEAREST`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUFilterMode](https://www.w3.org/TR/webgpu/#enumdef-gpufiltermode).
	 * 
	 */
	override val magFilter: GPUFilterMode = GPUFilterMode.Nearest,
	/**
	 * Specifies the filtering mode used when the sampled area is larger than one texel. The default value is `GPUFilterMode.NEAREST`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUFilterMode](https://www.w3.org/TR/webgpu/#enumdef-gpufiltermode).
	 * 
	 */
	override val minFilter: GPUFilterMode = GPUFilterMode.Nearest,
	/**
	 * Specifies the filtering mode used when sampling between mipmap levels. The default value is `GPUMipmapFilterMode.NEAREST`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUMipmapFilterMode](https://www.w3.org/TR/webgpu/#enumdef-gpumipmapfiltermode).
	 * 
	 */
	override val mipmapFilter: GPUMipmapFilterMode = GPUMipmapFilterMode.Nearest,
	/**
	 * Specifies the minimum level of detail (LOD) used internally when sampling a texture. The default value is `0.0`.
	 * 
	 * For more details, refer to the [WebGPU specification on levels of detail](https://www.w3.org/TR/webgpu/#levels-of-detail).
	 * 
	 */
	override val lodMinClamp: Float = 0f,
	/**
	 * Specifies the maximum level of detail (LOD) used internally when sampling a texture. The default value is `32.0`.
	 * 
	 * For more details, refer to the [WebGPU specification on levels of detail](https://www.w3.org/TR/webgpu/#levels-of-detail).
	 * 
	 */
	override val lodMaxClamp: Float = 32f,
	/**
	 * Specifies the comparison function used by a comparison sampler. When provided, the sampler will be a comparison sampler with the specified `GPUCompareFunction`. Comparison samplers may use filtering, but the sampling results will be implementation-dependent and may differ from the normal filtering rules.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUCompareFunction](https://www.w3.org/TR/webgpu/#enumdef-gpucomparefunction).
	 * 
	 */
	override val compare: GPUCompareFunction? = null,
	/**
	 * Specifies the maximum anisotropy value clamp used by the sampler. Anisotropic filtering is enabled when `maxAnisotropy` is greater than 1 and the implementation supports it. The default value is `1`.
	 * 
	 * For more details, refer to the [WebGPU specification on anisotropic filtering](https://www.w3.org/TR/webgpu/#dom-gpusamplerdescriptor-maxanisotropy).
	 * 
	 */
	override val maxAnisotropy: UShort = 1u,
	override val label: String = ""
): GPUSamplerDescriptor

/**
 * Represents a descriptor for creating a GPUBindGroupLayout. This interface extends GPUObjectDescriptorBase and is used to define the layout of bind groups in WebGPU.
 * 
 * A `GPUBindGroupLayoutDescriptor` specifies a list of entries that describe shader resource bindings. Each entry defines how resources are bound to shaders, including buffers, samplers, textures, and external textures.
 * 
 */
data class BindGroupLayoutDescriptor(
	/**
	 * A required list of GPUBindGroupLayoutEntry objects that define the shader resource bindings for a bind group.
	 * 
	 * Each entry in this list describes a single shader resource binding to be included in a `GPUBindGroupLayout`. The entries specify how resources are bound to shaders, including buffers, samplers, textures, and external textures.
	 * 
	 * **Type**: List<GPUBindGroupLayoutEntry>
	 * 
	 * **See also**:
	 * - GPUBindGroupLayoutEntry
	 * 
	 */
	override val entries: List<GPUBindGroupLayoutEntry>,
	override val label: String = ""
): GPUBindGroupLayoutDescriptor

/**
 * Represents a binding layout entry for a GPU bind group. This interface defines the structure of individual bindings within a [GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#gpubindgrouplayout).
 * 
 * A `GPUBindGroupLayoutEntry` specifies how resources are bound to shader stages, including buffers, samplers, textures, and storage textures. Only one type of binding (buffer, sampler, texture, storageTexture) can be defined for any given entry.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUBindGroupLayoutEntry](https://www.w3.org/TR/webgpu/#dictdef-gpubindgrouplayoutentry)
 * 
 */
data class BindGroupLayoutEntry(
	/**
	 * A unique identifier for a resource binding within the [GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#gpubindgrouplayout). This ID corresponds to a `GPUBindGroupEntry.binding` and a `@binding` attribute in the [GPUShaderModule](https://www.w3.org/TR/webgpu/#gpushadermodule).
	 * 
	 * **Type:** [GPUIndex32](https://www.w3.org/TR/webgpu/#typedefdef-gpuindex32)
	 * 
	 */
	override val binding: GPUIndex32,
	/**
	 * A bitset of the members of [GPUShaderStage](https://www.w3.org/TR/webgpu/#namespacedef-gpushaderstage). Each set bit indicates that a `GPUBindGroupLayoutEntry`'s resource will be accessible from the associated shader stage.
	 * 
	 * **Type:** [GPUShaderStageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gpushaderstageflags)
	 * 
	 */
	override val visibility: GPUShaderStage,
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUBufferBinding](https://www.w3.org/TR/webgpu/#dictdef-gpubufferbinding).
	 * 
	 * **Type:** [GPUBufferBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gpubufferbindinglayout)
	 * 
	 */
	override val buffer: GPUBufferBindingLayout? = null,
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUSampler](https://www.w3.org/TR/webgpu/#gpusampler).
	 * 
	 * **Type:** [GPUSamplerBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gpusamplerbindinglayout)
	 * 
	 */
	override val sampler: GPUSamplerBindingLayout? = null,
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview).
	 * 
	 * **Type:** [GPUTextureBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gputexturebindinglayout)
	 * 
	 */
	override val texture: GPUTextureBindingLayout? = null,
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview).
	 * 
	 * **Type:** [GPUStorageTextureBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gpustoragetexturebindinglayout)
	 * 
	 */
	override val storageTexture: GPUStorageTextureBindingLayout? = null
): GPUBindGroupLayoutEntry

/**
 * Represents a layout for buffer bindings in WebGPU. This interface defines the properties required to specify how buffers should be bound to binding points in shaders.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpubufferbindinglayout-dictionary).
 * 
 */
data class BufferBindingLayout(
	/**
	 * Specifies the type required for buffers bound to this binding point. This property determines how the buffer will be used in the shader.
	 * 
	 * **Type:** [GPUBufferBindingType](https://www.w3.org/TR/webgpu/#enumdef-gpubufferbindingtype)
	 * 
	 * **Default Value:** "uniform"
	 * 
	 */
	override val type: GPUBufferBindingType = GPUBufferBindingType.Uniform,
	/**
	 * Indicates whether this binding requires a dynamic offset. A dynamic offset allows for more flexible buffer binding, enabling the use of different buffer sizes at runtime.
	 * 
	 * **Type:** Boolean
	 * 
	 * **Default Value:** false
	 * 
	 */
	override val hasDynamicOffset: Boolean = false,
	/**
	 * Specifies the minimum size of a buffer binding used with this bind point. This value is used to validate that buffers bound to this layout meet the required size constraints.
	 * 
	 * **Type:** GPUSize64
	 * 
	 * **Default Value:** 0
	 * 
	 * **Behavior:**
	 * - If `minBindingSize` is not `0`, pipeline creation validates that this value is greater than or equal to the minimum buffer binding size of the variable.
	 * - If `minBindingSize` is `0`, it is ignored during pipeline creation, and draw/dispatch commands validate that each binding in the [GPUBindGroup](https://www.w3.org/TR/webgpu/#gpubindgroup) satisfies the minimum buffer binding size of the variable.
	 * 
	 */
	override val minBindingSize: GPUSize64 = 0u
): GPUBufferBindingLayout

/**
 * Represents a binding layout for samplers in WebGPU. This interface defines the type of sampler that can be bound to a specific binding.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpusamplerbindinglayout).
 * 
 */
data class SamplerBindingLayout(
	/**
	 * Specifies the type of sampler that can be bound to this binding layout. This is an enumeration value that indicates whether the sampler is used for filtering, non-filtering, or comparison operations.
	 * 
	 * **Type**: [GPUSamplerBindingType]
	 * 
	 * **Default Value**: "filtering"
	 * 
	 * This property determines how the sampler will be utilized in the shader. For example, a filtering sampler might be used for texture sampling with mipmapping, while a non-filtering sampler might be used for shadow mapping.
	 * 
	 */
	override val type: GPUSamplerBindingType = GPUSamplerBindingType.Filtering
): GPUSamplerBindingLayout

/**
 * Represents the layout for a GPU texture binding. This interface defines the required properties for specifying how textures should be bound in a GPU pipeline.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gputexturebindinglayout).
 * 
 */
data class TextureBindingLayout(
	/**
	 * Specifies the type required for texture views bound to this binding. This property determines how the texture data should be sampled.
	 * 
	 * **Default Value**: "float"
	 * 
	 * **Possible Values**:
	 * - `GPUTextureSampleType.FLOAT`
	 * - `GPUTextureSampleType.UNFILTERABLE_FLOAT`
	 * - `GPUTextureSampleType.DEPTH`
	 * - `GPUTextureSampleType.SINT`
	 * - `GPUTextureSampleType.UINT`
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gputexturesampletype).
	 * 
	 */
	override val sampleType: GPUTextureSampleType = GPUTextureSampleType.Float,
	/**
	 * Specifies the required dimension for texture views bound to this binding. This property defines the dimensionality of the texture view.
	 * 
	 * **Default Value**: "2d"
	 * 
	 * **Possible Values**:
	 * - `GPUTextureViewDimension._1D`
	 * - `GPUTextureViewDimension._2D`
	 * - `GPUTextureViewDimension._2D_ARRAY`
	 * - `GPUTextureViewDimension._3D`
	 * - `GPUTextureViewDimension.CUBE`
	 * - `GPUTextureViewDimension.CUBE_ARRAY`
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	override val viewDimension: GPUTextureViewDimension = GPUTextureViewDimension.TwoD,
	/**
	 * Indicates whether texture views bound to this binding must be multisampled. This property is used to specify if the texture should support multisampling.
	 * 
	 * **Default Value**: `false`
	 * 
	 * **Type**: `Boolean`
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gputexturebindinglayout).
	 * 
	 */
	override val multisampled: Boolean = false
): GPUTextureBindingLayout

/**
 * Represents the layout configuration for a storage texture binding in WebGPU. This interface defines how textures are accessed and used within shaders, specifying the access mode, format, and view dimension.
 * 
 * For more details, refer to the [WebGPU specification on GPUStorageTextureBindingLayout](https://www.w3.org/TR/webgpu/#dictdef-gpustoragetexturebindinglayout).
 * 
 */
data class StorageTextureBindingLayout(
	/**
	 * Specifies the required format of texture views bound to this binding. This property is mandatory and defines how the texture data is interpreted.
	 * 
	 * **Type:** [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat)
	 * 
	 */
	override val format: GPUTextureFormat,
	/**
	 * Specifies the access mode for this binding, indicating whether the texture is readable, writable, or both. This property defaults to `GPUStorageTextureAccess.WriteOnly`.
	 * 
	 * **Type:** [GPUStorageTextureAccess](https://www.w3.org/TR/webgpu/#enumdef-gpustoragetextureaccess)
	 * 
	 * **Default Value:** `GPUStorageTextureAccess.WriteOnly`
	 * 
	 */
	override val access: GPUStorageTextureAccess = GPUStorageTextureAccess.WriteOnly,
	/**
	 * Specifies the required dimension for texture views bound to this binding. This property defaults to `GPUTextureViewDimension.D2`.
	 * 
	 * **Type:** [GPUTextureViewDimension](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension)
	 * 
	 * **Default Value:** `GPUTextureViewDimension.D2`
	 * 
	 */
	override val viewDimension: GPUTextureViewDimension = GPUTextureViewDimension.TwoD
): GPUStorageTextureBindingLayout

/**
 * The `GPUBindGroupDescriptor` interface represents a descriptor for creating bind groups in WebGPU. It extends the `GPUObjectDescriptorBase` and is used to specify the layout and entries of a bind group.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpubindgroupdescriptor).
 * 
 */
data class BindGroupDescriptor(
	/**
	 * The `layout` property specifies the `GPUBindGroupLayout` that the entries of this bind group will conform to. This layout defines how resources are bound and accessed in shaders.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubindgroupdescriptor-layout).
	 * 
	 */
	override val layout: GPUBindGroupLayout,
	/**
	 * The `entries` property is a list of `GPUBindGroupEntry` objects that describe the resources to expose to the shader for each binding described by the `layout`. Each entry specifies how a particular resource should be bound.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubindgroupdescriptor-entries).
	 * 
	 */
	override val entries: List<GPUBindGroupEntry>,
	override val label: String = ""
): GPUBindGroupDescriptor

/**
 * Represents a single resource to be bound in a [GPUBindGroup]. This interface is used to describe the binding of resources such as samplers, texture views, external textures, or buffer bindings within a bind group.
 * 
 * For more details, refer to the [WebGPU specification on GPUBindGroupEntry](https://www.w3.org/TR/webgpu/#dictdef-gpubindgroupentry).
 * 
 */
data class BindGroupEntry(
	/**
	 * A unique identifier for a resource binding within the [GPUBindGroup]. This identifier corresponds to a `GPUBindGroupLayoutEntry.binding` and a `@binding` attribute in the [GPUShaderModule].
	 * 
	 * **Type:** [GPUIndex32](TYPE_MAPPING.md)
	 * 
	 */
	override val binding: GPUIndex32,
	/**
	 * The resource to bind, which can be one of the following types:
	 * - [GPUSampler]
	 * - [GPUTextureView]
	 * - [GPUExternalTexture]
	 * - [GPUBufferBinding]
	 * 
	 * **Type:** [GPUBindingResource](TYPE_MAPPING.md)
	 * 
	 */
	override val resource: GPUBindingResource
): GPUBindGroupEntry

/**
 * The `GPUBufferBinding` interface describes a buffer and an optional range to bind as a resource. This is used in the context of WebGPU to specify how buffers should be bound for shader access.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpubufferbinding).
 * 
 */
data class BufferBinding(
	/**
	 * The `buffer` property specifies the `GPUBuffer` to bind. This buffer will be exposed to shaders as a resource.
	 * 
	 * **Type**: [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer)
	 * 
	 */
	override val buffer: GPUBuffer,
	/**
	 * The `offset` property specifies the offset, in bytes, from the beginning of the `buffer` to the start of the range exposed to the shader by the buffer binding. This value defaults to 0 if not specified.
	 * 
	 * **Type**: [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	override val offset: GPUSize64 = 0u,
	/**
	 * The `size` property specifies the size, in bytes, of the buffer binding. If not provided, it specifies the range starting at `offset` and ending at the end of the `buffer`.
	 * 
	 * **Type**: [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	override val size: GPUSize64? = null
): GPUBufferBinding

/**
 * The `GPUPipelineLayoutDescriptor` interface defines all the [GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#dictdef-gpubindgrouplayout)s used by a pipeline. This descriptor is essential for configuring the layout of bind groups in a GPU pipeline, ensuring that shader modules can access resources correctly.
 * 
 * **Inheritance**: This interface inherits from [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase).
 * 
 * In this example, `bindGroupLayout1` and `bindGroupLayout2` are instances of [GPUBindGroupLayout]. The `bindGroupLayouts` list defines the layout of bind groups that the pipeline will use.
 * 
 */
data class PipelineLayoutDescriptor(
	/**
	 * A list of optional [GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#dictdef-gpubindgrouplayout)s that the pipeline will use. Each element in this list corresponds to a `@group` attribute in the [GPUShaderModule], with the `N`th element corresponding to `@group(N)`.
	 * 
	 * **Type**: `List<GPUBindGroupLayout?>`
	 * 
	 * **Details**:
	 * - This list defines the layout of bind groups that the pipeline will use.
	 * - Each [GPUBindGroupLayout] in this list must match the corresponding `@group` attribute in the shader module.
	 * 
	 * In this example, `bindGroupLayout1` and `bindGroupLayout2` are instances of [GPUBindGroupLayout]. The `bindGroupLayouts` list defines the layout of bind groups that the pipeline will use.
	 * 
	 */
	override val bindGroupLayouts: List<GPUBindGroupLayout>,
	override val label: String = ""
): GPUPipelineLayoutDescriptor

/**
 * Represents a descriptor for creating a [GPUShaderModule] in WebGPU. This interface extends `GPUObjectDescriptorBase` and is used to specify the WGSL source code and compilation hints required to create a shader module. The shader module is a compiled version of the shader code that can be used in rendering or compute pipelines.
 * 
 * **See also:**
 * - [W3C WebGPU Specification: GPUShaderModuleDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpushadermoduledescriptor)
 * 
 */
data class ShaderModuleDescriptor(
	/**
	 * The WGSL source code for the shader module. This string contains the shader program written in the WebGPU Shading Language (WGSL). The shader code defines the vertex and fragment shaders or compute shaders that will be used in the rendering or compute pipeline.
	 * 
	 * **Type:** `String`
	 * 
	 * **See also:**
	 * - [W3C WebGPU Specification: GPUShaderModuleDescriptor.code](https://www.w3.org/TR/webgpu/#dom-gpushadermoduledescriptor-code)
	 * 
	 */
	override val code: String,
	/**
	 * A list of `GPUShaderModuleCompilationHint` objects that provide additional information to the compiler about the shader module. These hints can include details about entry points, resource bindings, and other compilation-specific information. Providing these hints can improve performance by allowing the compiler to perform more optimizations during the creation of the shader module.
	 * 
	 * **Type:** `List<GPUShaderModuleCompilationHint>`
	 * 
	 * **Default Value:** An empty list (`[]`)
	 * 
	 * **See also:**
	 * - [W3C WebGPU Specification: GPUShaderModuleDescriptor.compilationHints](https://www.w3.org/TR/webgpu/#dom-gpushadermoduledescriptor-compilationhints)
	 * 
	 */
	override val compilationHints: List<GPUShaderModuleCompilationHint> = emptyList(),
	override val label: String = ""
): GPUShaderModuleDescriptor

/**
 * Represents a hint for compiling a GPUShaderModule. This interface provides information about the entry point and layout that may be used with the shader module in future pipeline creation calls.
 * 
 * For more details, refer to the WebGPU specification on Shader Module Compilation Information: https://www.w3.org/TR/webgpu/#shader-module-compilation-information.
 * 
 */
data class ShaderModuleCompilationHint(
	/**
	 * The entry point of the shader module. This is a required field and must be specified.
	 * 
	 * Type: String (https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/).
	 * 
	 */
	override val entryPoint: String,
	/**
	 * A GPUPipelineLayout that the shader module may be used with in future pipeline creation calls. If set to null, the default pipeline layout for the entry point associated with this hint will be used.
	 * 
	 * Type: GPUPipelineLayout or GPUAutoLayoutMode.
	 * 
	 */
	override val layout: GPUPipelineLayout? = null
): GPUShaderModuleCompilationHint

/**
 * Represents the base descriptor for a GPU pipeline. This interface extends [GPUObjectDescriptorBase] and is used to define the layout of a GPU pipeline.
 * 
 * The `layout` property specifies either a [GPUPipelineLayout] or an automatic layout mode (`"auto"`). When `"auto"` is specified, the pipeline layout is generated automatically.
 * 
 */
data class PipelineDescriptorBase(
	/**
	 * Specifies the layout for this pipeline. This can be either a [GPUPipelineLayout] object or the string `"auto"` to generate the pipeline layout automatically.
	 * 
	 * **Type**: [GPUPipelineLayout] or `String`
	 * 
	 * **Default**: None
	 * 
	 * **Behavior**:
	 * - If a [GPUPipelineLayout] is provided, it defines the specific layout for the pipeline.
	 * - If `"auto"` is specified, the pipeline layout is generated automatically. However, this means that the pipeline cannot share [GPUBindGroup]s with any other pipelines.
	 * 
	 * **See also**: [GPUAutoLayoutMode], [GPUBindGroup]
	 * 
	 */
	override val layout: GPUPipelineLayout? = null,
	override val label: String = ""
): GPUPipelineDescriptorBase

/**
 * Represents a programmable stage in a GPU pipeline. This interface describes the entry point in a user-provided [GPUShaderModule] that controls one of the programmable stages of a pipeline.
 * Entry point names follow the rules defined in [WGSL identifier comparison](https://gpuweb.github.io/gpuweb/wgsl/#identifier-comparison).
 * 
 */
data class ProgrammableStage(
	/**
	 * The shader module containing the entry point for this programmable stage. This is a required field and must be provided when creating an instance of [GPUProgrammableStage].
	 * 
	 */
	override val module: GPUShaderModule,
	/**
	 * The name of the entry point in the shader module. This is an optional field and can be null if not specified.
	 * Entry point names must follow the rules defined in [WGSL identifier comparison](https://gpuweb.github.io/gpuweb/wgsl/#identifier-comparison).
	 * 
	 */
	override val entryPoint: String? = null,
	/**
	 * A map of constant values that can be passed to the shader module. The keys are strings representing the names of the constants, and the values are of type [GPUPipelineConstantValue].
	 * This field is optional and defaults to an empty map if not provided.
	 * 
	 */
	override val constants: Map<String, GPUPipelineConstantValue> = emptyMap()
): GPUProgrammableStage

/**
 * Represents a descriptor for creating a compute pipeline in WebGPU. This interface extends [GPUPipelineDescriptorBase] and is used to define the configuration for a compute pipeline, which executes compute shaders.
 * 
 * A compute pipeline is responsible for performing general-purpose computations on the GPU. It does not render graphics but can be used for tasks such as data processing, simulations, and other parallel computations.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpucomputepipelinedescriptor).
 * 
 */
data class ComputePipelineDescriptor(
	/**
	 * Specifies the compute shader stage for the pipeline. This member is required and must be set to a valid [GPUProgrammableStage] object that describes the compute shader entry point.
	 * 
	 * The compute shader is responsible for executing the compute operations defined in the shader code. It does not produce visual output but can perform parallel computations on data.
	 * 
	 * **Type:** [GPUProgrammableStage]
	 * 
	 */
	override val compute: GPUProgrammableStage,
	override val layout: GPUPipelineLayout? = null,
	override val label: String = ""
): GPUComputePipelineDescriptor

/**
 * The `GPURenderPipelineDescriptor` interface extends `GPUPipelineDescriptorBase` and defines the configuration for a render pipeline in WebGPU. It specifies the vertex, primitive, depth-stencil, multisample, and fragment states required to create a render pipeline. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dictdef-gpurenderpipelinedescriptor).
 * 
 */
data class RenderPipelineDescriptor(
	/**
	 * `vertex` of type `GPUVertexState`. Describes the vertex shader entry point of the pipeline and its input buffer layouts. This is a required field. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-vertex).
	 * 
	 */
	override val vertex: GPUVertexState,
	/**
	 * `primitive` of type `GPUPrimitiveState`, defaulting to `{}` if not provided. Describes the primitive-related properties of the pipeline, such as topology and strip index format. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-primitive).
	 * 
	 */
	override val primitive: GPUPrimitiveState = PrimitiveState(),
	/**
	 * `depthStencil` of type `GPUDepthStencilState?`. Describes the optional depth-stencil properties, including testing, operations, and bias. This field is nullable. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-depthstencil).
	 * 
	 */
	override val depthStencil: GPUDepthStencilState? = null,
	/**
	 * `multisample` of type `GPUMultisampleState`, defaulting to `{}` if not provided. Describes the multi-sampling properties of the pipeline, such as count and mask. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-multisample).
	 * 
	 */
	override val multisample: GPUMultisampleState = MultisampleState(),
	/**
	 * `fragment` of type `GPUFragmentState?`. Describes the fragment shader entry point of the pipeline and its output colors. If not provided, the [no color output mode](https://www.w3.org/TR/webgpu/#no-color-output) is enabled. This field is nullable. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-fragment).
	 * 
	 */
	override val fragment: GPUFragmentState? = null,
	override val layout: GPUPipelineLayout? = null,
	override val label: String = ""
): GPURenderPipelineDescriptor

/**
 * Represents the state of a primitive in WebGPU, defining how primitives are rendered. This interface is used to configure various aspects of primitive rendering such as topology, strip index format, front face orientation, cull mode, and depth clipping behavior.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuprimitivestate).
 * 
 */
data class PrimitiveState(
	/**
	 * Specifies the type of primitive topology used for rendering. This determines how vertices are interpreted when drawing primitives.
	 * 
	 * **Default Value:** `GPUPrimitiveTopology.TriangleList`
	 * 
	 * **See Also:**
	 * - [GPUPrimitiveTopology](https://www.w3.org/TR/webgpu/#enumdef-gpuprimitivetopology)
	 * 
	 */
	override val topology: GPUPrimitiveTopology = GPUPrimitiveTopology.TriangleList,
	/**
	 * Specifies the format of the strip index buffer, if used. This is relevant when rendering primitives that use strip indexing.
	 * 
	 * **Default Value:** `null`
	 * 
	 * **See Also:**
	 * - [GPUIndexFormat](https://www.w3.org/TR/webgpu/#enumdef-gpuindexformat)
	 * 
	 */
	override val stripIndexFormat: GPUIndexFormat? = null,
	/**
	 * Specifies the orientation of the front face of primitives. This determines which side of a triangle is considered the front face for culling and other operations.
	 * 
	 * **Default Value:** `GPUFrontFace.CCW`
	 * 
	 * **See Also:**
	 * - [GPUFrontFace](https://www.w3.org/TR/webgpu/#enumdef-gpufrontface)
	 * 
	 */
	override val frontFace: GPUFrontFace = GPUFrontFace.CCW,
	/**
	 * Specifies the culling mode for primitives. This determines which faces of a primitive are discarded during rendering.
	 * 
	 * **Default Value:** `GPUCullMode.None`
	 * 
	 * **See Also:**
	 * - [GPUCullMode](https://www.w3.org/TR/webgpu/#enumdef-gpucullmode)
	 * 
	 */
	override val cullMode: GPUCullMode = GPUCullMode.None,
	/**
	 * Specifies whether depth values are clipped or unclipped. This feature requires the `"depth-clip-control"` feature to be enabled.
	 * 
	 * **Default Value:** `false`
	 * 
	 * **See Also:**
	 * - [WebGPU Features](https://www.w3.org/TR/webgpu/#features)
	 * 
	 */
	override val unclippedDepth: Boolean = false
): GPUPrimitiveState

/**
 * Represents the multisampling state used by a [GPURenderPipeline](https://www.w3.org/TR/webgpu/#gpurenderpipeline) to interact with render pass attachments that support multisampling.
 * 
 * This interface defines how many samples per pixel are used, which samples are written to, and whether alpha-to-coverage is enabled. The multisample state is crucial for rendering high-quality images by reducing aliasing artifacts.
 * 
 * **See also:**
 * - [GPUMultisampleState dictionary in the WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpumultisamplestate)
 * 
 */
data class MultisampleState(
	/**
	 * Specifies the number of samples per pixel. This value determines the level of multisampling used during rendering.
	 * 
	 * **Type:** [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32)
	 * 
	 * **Default Value:** 1
	 * 
	 * **Constraints:**
	 * - Must be either 1 or 4.
	 * - If `alphaToCoverageEnabled` is `true`, `count` must be greater than 1.
	 * 
	 * **See also:**
	 * - [count member in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpumultisamplestate-count)
	 * 
	 */
	override val count: GPUSize32 = 1u,
	/**
	 * Determines which samples are written to during rendering. This mask allows for selective sampling, which can be useful for optimizing performance or achieving specific visual effects.
	 * 
	 * **Type:** [GPUSampleMask](https://www.w3.org/TR/webgpu/#typedefdef-gpusamplemask)
	 * 
	 * **Default Value:** 0xFFFFFFFF
	 * 
	 * **See also:**
	 * - [mask member in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpumultisamplestate-mask)
	 * 
	 */
	override val mask: GPUSampleMask = 0xFFFFFFFFu,
	/**
	 * When set to `true`, enables alpha-to-coverage, which uses the fragment's alpha channel to generate a sample coverage mask. This can improve the quality of antialiased edges.
	 * 
	 * **Type:** Boolean
	 * 
	 * **Default Value:** false
	 * 
	 * **Constraints:**
	 * - If `alphaToCoverageEnabled` is `true`, `count` must be greater than 1.
	 * 
	 * **See also:**
	 * - [alphaToCoverageEnabled member in the WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpumultisamplestate-alphatocoverageenabled)
	 * 
	 */
	override val alphaToCoverageEnabled: Boolean = false
): GPUMultisampleState

/**
 * Represents a fragment state in WebGPU, which is a type of programmable stage that defines how fragments are processed during rendering. This interface extends [GPUProgrammableStage] and includes specific configurations for color targets.
 * 
 * The `GPUFragmentState` interface is used to configure the fragment shader stage of a GPU pipeline, specifying how the colors are written to the render target. This is crucial for defining the visual output of a rendering operation.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUProgrammableStage](https://www.w3.org/TR/webgpu/#gpuprogrammablestage)
 * 
 */
data class FragmentState(
	/**
	 * A list of [GPUColorTargetState] objects that define the formats and behaviors of the color targets this pipeline writes to. Each `GPUColorTargetState` in the list specifies how a particular color target should be handled during rendering.
	 * 
	 * **Type:** List<[GPUColorTargetState]?>
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUFragmentState](https://www.w3.org/TR/webgpu/#dictdef-gpufragmentstate)
	 * 
	 */
	override val targets: List<GPUColorTargetState>,
	override val module: GPUShaderModule,
	override val entryPoint: String? = null,
	override val constants: Map<String, GPUPipelineConstantValue> = emptyMap()
): GPUFragmentState

/**
 * Represents the state of a color target in a GPU render pipeline. This interface defines the format, blending behavior, and write mask for a color attachment in a render pass.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpucolortargetstate).
 * 
 */
data class ColorTargetState(
	/**
	 * The format of this color target. The pipeline will only be compatible with render pass encoders which use a texture view of this format in the corresponding color attachment.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolortargetstate-format).
	 * 
	 */
	override val format: GPUTextureFormat,
	/**
	 * The blending behavior for this color target. If left undefined, disables blending for this color target.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolortargetstate-blend).
	 * 
	 */
	override val blend: GPUBlendState? = null,
	/**
	 * Bitmask controlling which channels are written to when drawing to this color target. Defaults to `0xF` (all channels).
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolortargetstate-writemask).
	 * 
	 */
	override val writeMask: GPUColorWrite = GPUColorWrite.All
): GPUColorTargetState

/**
 * Represents the blend state used in rendering operations, defining how colors and alpha values are blended.
 * 
 * This interface is part of the WebGPU API and corresponds to the `GPUBlendState` dictionary defined in the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpublendstate).
 * 
 * The `GPUBlendState` interface includes two properties: `color` and `alpha`, both of type `GPUBlendComponent`. These properties specify the blending behavior for color channels and alpha channels, respectively.
 * 
 */
data class BlendState(
	/**
	 * Defines the blending behavior of the corresponding render target for color channels.
	 * 
	 * This property is of type `GPUBlendComponent` and specifies how the color channels are blended during rendering. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpublendstate-color).
	 * 
	 */
	override val color: GPUBlendComponent,
	/**
	 * Defines the blending behavior of the corresponding render target for the alpha channel.
	 * 
	 * This property is of type `GPUBlendComponent` and specifies how the alpha channel is blended during rendering. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpublendstate-alpha).
	 * 
	 */
	override val alpha: GPUBlendComponent
): GPUBlendState

/**
 * Represents a blend component used in blending operations for color or alpha components of a fragment. This interface defines how the source and destination colors are combined during rendering.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpublendcomponent).
 * 
 */
data class BlendComponent(
	/**
	 * Defines the [GPUBlendOperation] used to calculate the values written to the target attachment components.
	 * 
	 * This property specifies the blending operation to be performed. The default value is `add`.
	 * 
	 * **Type:** [GPUBlendOperation]
	 * 
	 * **Default Value:** "add"
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpublendcomponent-operation).
	 * 
	 */
	override val operation: GPUBlendOperation = GPUBlendOperation.Add,
	/**
	 * Defines the [GPUBlendFactor] operation to be performed on values from the fragment shader.
	 * 
	 * This property specifies the blending factor for the source color. The default value is `one`.
	 * 
	 * **Type:** [GPUBlendFactor]
	 * 
	 * **Default Value:** "one"
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpublendcomponent-srcfactor).
	 * 
	 */
	override val srcFactor: GPUBlendFactor = GPUBlendFactor.One,
	/**
	 * Defines the [GPUBlendFactor] operation to be performed on values from the target attachment.
	 * 
	 * This property specifies the blending factor for the destination color. The default value is `zero`.
	 * 
	 * **Type:** [GPUBlendFactor]
	 * 
	 * **Default Value:** "zero"
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpublendcomponent-dstfactor).
	 * 
	 */
	override val dstFactor: GPUBlendFactor = GPUBlendFactor.Zero
): GPUBlendComponent

/**
 * Represents the depth and stencil state configuration for a GPU render pipeline. This interface defines various properties that control how depth and stencil tests are performed during rendering.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#depth-stencil-state).
 * 
 */
data class DepthStencilState(
	/**
	 * Specifies the format of the depth/stencil texture. This property determines how depth and stencil values are stored in the texture.
	 * 
	 * **Type:** [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat)
	 * 
	 */
	override val format: GPUTextureFormat,
	/**
	 * Indicates whether depth values are written to the depth buffer. When set to `true`, depth values are written; when set to `false` or `null`, depth values are not written.
	 * 
	 * **Type:** Boolean?
	 * 
	 */
	override val depthWriteEnabled: Boolean? = null,
	/**
	 * Specifies the comparison function used for depth tests. This property determines how the current depth value is compared to the stored depth value.
	 * 
	 * **Type:** [GPUCompareFunction](https://www.w3.org/TR/webgpu/#enumdef-gpucomparefunction)
	 * 
	 */
	override val depthCompare: GPUCompareFunction? = null,
	/**
	 * Defines the stencil state for front-facing primitives. This property configures how stencil tests are performed for front-facing geometry.
	 * 
	 * **Type:** [GPUStencilFaceState](https://www.w3.org/TR/webgpu/#dictdef-gpustencilfacestate)
	 * 
	 */
	override val stencilFront: GPUStencilFaceState = StencilFaceState(),
	/**
	 * Defines the stencil state for back-facing primitives. This property configures how stencil tests are performed for back-facing geometry.
	 * 
	 * **Type:** [GPUStencilFaceState](https://www.w3.org/TR/webgpu/#dictdef-gpustencilfacestate)
	 * 
	 */
	override val stencilBack: GPUStencilFaceState = StencilFaceState(),
	/**
	 * Specifies the mask used for reading stencil values. This property determines which bits of the stencil value are considered during read operations.
	 * 
	 * **Type:** [GPUStencilValue](https://www.w3.org/TR/webgpu/#typedefdef-gpustencilvalue)
	 * 
	 * **Default Value:** `0xFFFFFFFF`
	 * 
	 */
	override val stencilReadMask: GPUStencilValue = 0xFFFFFFFFu,
	/**
	 * Specifies the mask used for writing stencil values. This property determines which bits of the stencil value are modified during write operations.
	 * 
	 * **Type:** [GPUStencilValue](https://www.w3.org/TR/webgpu/#typedefdef-gpustencilvalue)
	 * 
	 * **Default Value:** `0xFFFFFFFF`
	 * 
	 */
	override val stencilWriteMask: GPUStencilValue = 0xFFFFFFFFu,
	/**
	 * Specifies the depth bias value. This property is used to adjust the depth values for polygon offset.
	 * 
	 * **Type:** [GPUDepthBias](https://www.w3.org/TR/webgpu/#typedefdef-gpudepthbias)
	 * 
	 * **Default Value:** `0`
	 * 
	 */
	override val depthBias: GPUDepthBias = 0,
	/**
	 * Specifies the slope scale factor for depth bias. This property is used to adjust the depth bias based on the slope of the polygon.
	 * 
	 * **Type:** Float
	 * 
	 * **Default Value:** `0.0`
	 * 
	 */
	override val depthBiasSlopeScale: Float = 0f,
	/**
	 * Specifies the clamp value for depth bias. This property limits the maximum depth bias that can be applied.
	 * 
	 * **Type:** Float
	 * 
	 * **Default Value:** `0.0`
	 * 
	 */
	override val depthBiasClamp: Float = 0f
): GPUDepthStencilState

/**
 * Represents a set of stencil face state parameters that define how stencil tests and operations are performed. This interface is used to configure the behavior of the stencil buffer for front or back-facing triangles in a render pipeline.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpustencilfacestate).
 * 
 */
data class StencilFaceState(
	/**
	 * Specifies the comparison function used for stencil tests. This determines how the current stencil value is compared to the reference value.
	 * 
	 * **Default Value:** `GPUCompareFunction.Always`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.compare](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-compare)
	 * 
	 */
	override val compare: GPUCompareFunction = GPUCompareFunction.Always,
	/**
	 * Specifies the operation to perform when the stencil test fails. This defines what action to take if the comparison function does not pass.
	 * 
	 * **Default Value:** `GPUStencilOperation.Keep`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.failOp](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-failop)
	 * 
	 */
	override val failOp: GPUStencilOperation = GPUStencilOperation.Keep,
	/**
	 * Specifies the operation to perform when the stencil test passes but the depth test fails. This defines what action to take if the comparison function passes but the depth test does not.
	 * 
	 * **Default Value:** `GPUStencilOperation.Keep`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.depthFailOp](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-depthfailop)
	 * 
	 */
	override val depthFailOp: GPUStencilOperation = GPUStencilOperation.Keep,
	/**
	 * Specifies the operation to perform when both the stencil test and the depth test pass. This defines what action to take if both tests are successful.
	 * 
	 * **Default Value:** `GPUStencilOperation.Keep`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.passOp](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-passop)
	 * 
	 */
	override val passOp: GPUStencilOperation = GPUStencilOperation.Keep
): GPUStencilFaceState

/**
 * Represents a vertex state in the WebGPU API, defining how vertex data is laid out and processed. This interface extends [GPUProgrammableStage], allowing it to be used as part of a render pipeline.
 * 
 * A `GPUVertexState` object specifies the layout of vertex attribute data in vertex buffers. Each buffer's layout is defined by a list of `GPUVertexBufferLayout` objects, which describe the structure and stride of the vertex data.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#vertex-state).
 * 
 */
data class VertexState(
	override val module: GPUShaderModule,
	/**
	 * A list of `GPUVertexBufferLayout` objects that define the layout of vertex attribute data in each vertex buffer used by this pipeline.
	 * 
	 * Each `GPUVertexBufferLayout` specifies how the vertex data is structured, including the stride between elements and the attributes that describe the members of the structure. This allows the GPU to correctly interpret the vertex data during rendering.
	 * 
	 * **Type**: List<[GPUVertexBufferLayout]>
	 * 
	 * **Default Value**: An empty list
	 * 
	 * For more information, see the [W3C WebGPU specification on GPUVertexBufferLayout](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexbufferlayout).
	 * 
	 */
	override val buffers: List<GPUVertexBufferLayout> = emptyList(),
	override val entryPoint: String? = null,
	override val constants: Map<String, GPUPipelineConstantValue> = emptyMap()
): GPUVertexState

/**
 * Represents the layout of a vertex buffer in WebGPU. This interface defines how vertices are structured and accessed, including the stride between elements, the step mode (whether data is per-vertex or per-instance), and the attributes that describe the vertex data.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexbufferlayout).
 * 
 */
data class VertexBufferLayout(
	/**
	 * The stride, in bytes, between elements of this array. This value specifies how much memory is allocated for each vertex or instance in the buffer.
	 * 
	 * **Type:** [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	override val arrayStride: GPUSize64,
	/**
	 * An array defining the layout of the vertex attributes within each element. This sequence describes how the vertex data is structured and accessed.
	 * 
	 * **Type:** List<[GPUVertexAttribute](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexattribute)>
	 * 
	 */
	override val attributes: List<GPUVertexAttribute>,
	/**
	 * Specifies whether each element of this array represents per-vertex data or per-instance data. The default value is `GPUVertexStepMode.VERTEX`.
	 * 
	 * **Type:** [GPUVertexStepMode](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexstepmode)
	 * 
	 */
	override val stepMode: GPUVertexStepMode = GPUVertexStepMode.Vertex
): GPUVertexBufferLayout

/**
 * Represents a vertex attribute in the WebGPU API. This interface defines the format, offset, and shader location of a vertex attribute.
 * 
 * A `GPUVertexAttribute` is used to describe how data from a vertex buffer should be interpreted by the GPU. It specifies the format of the data (e.g., float32, uint32), the byte offset within the vertex buffer where the data starts, and the shader location that corresponds to this attribute.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexattribute).
 * 
 */
data class VertexAttribute(
	/**
	 * The format of the vertex attribute. This specifies how the data should be interpreted by the GPU.
	 * 
	 * **Type:** [GPUVertexFormat]
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuvertexattribute-format).
	 * 
	 */
	override val format: GPUVertexFormat,
	/**
	 * The offset, in bytes, from the beginning of the vertex buffer element to the data for this attribute.
	 * 
	 * **Type:** [GPUSize64]
	 * 
	 * This value must be a multiple of the minimum of 4 and the byte size of the format specified by `format`. It defines where within the vertex buffer the data for this attribute begins.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuvertexattribute-offset).
	 * 
	 */
	override val offset: GPUSize64,
	/**
	 * The numeric location associated with this attribute. This corresponds to a `@location` attribute declared in the vertex module of the shader.
	 * 
	 * **Type:** [GPUIndex32]
	 * 
	 * This value must be less than the maximum number of vertex attributes supported by the device, as specified by `device.limits.maxVertexAttributes`.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuvertexattribute-shaderlocation).
	 * 
	 */
	override val shaderLocation: GPUIndex32
): GPUVertexAttribute

/**
 * The `GPUTexelCopyBufferLayout` interface describes the layout of texels in a buffer of bytes during a texel copy operation. This interface is used to define how data is organized in a [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) or an [AllowSharedBufferSource](https://webidl.spec.whatwg.org/#AllowSharedBufferSource) when performing texel copy operations.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gputexelcopybufferlayout).
 * 
 */
data class TexelCopyBufferLayout(
	/**
	 * The `offset` property specifies the starting offset in bytes from the beginning of the buffer where the texel data begins. This value is of type [GPUSize64], which represents a 64-bit unsigned integer.
	 * 
	 * **Default Value:** `0`
	 * 
	 */
	override val offset: GPUSize64 = 0u,
	/**
	 * The `bytesPerRow` property specifies the number of bytes per row in the texel data. This value is of type [GPUSize32], which represents a 32-bit unsigned integer.
	 * 
	 */
	override val bytesPerRow: GPUSize32? = null,
	/**
	 * The `rowsPerImage` property specifies the number of rows per image in the texel data. This value is of type [GPUSize32], which represents a 32-bit unsigned integer.
	 * 
	 */
	override val rowsPerImage: GPUSize32? = null
): GPUTexelCopyBufferLayout

/**
 * The `GPUTexelCopyBufferInfo` interface describes the information about a buffer source or destination of a texel copy operation. This includes details such as the buffer itself and its layout.
 * 
 * Together with the `copySize`, it defines the footprint of a region of texels in a [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer). This interface is essential for operations that involve copying texel data between buffers and textures.
 * 
 * For more details, refer to the [WebGPU specification on GPUTexelCopyBufferInfo](https://www.w3.org/TR/webgpu/#gputexelcopybufferinfo).
 * 
 */
data class TexelCopyBufferInfo(
	/**
	 * The `buffer` property represents a buffer that either contains texel data to be copied or will store the texel data being copied, depending on the method it is being passed to.
	 * 
	 * This property is of type [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) and must be a valid GPU buffer. The validity of this buffer is checked during the validation process of `GPUTexelCopyBufferInfo`.
	 * 
	 */
	override val buffer: GPUBuffer,
	override val offset: GPUSize64 = 0u,
	override val bytesPerRow: GPUSize32? = null,
	override val rowsPerImage: GPUSize32? = null
): GPUTexelCopyBufferInfo

/**
 * Represents the information about a texture source or destination for a texel copy operation. This interface describes the sub-region of a texture that spans one or more contiguous texture subresources at the same mip-map level.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gputexelcopytextureinfo).
 * 
 */
data class TexelCopyTextureInfo(
	/**
	 * The texture to copy to or from. This is a required field and must be specified.
	 * 
	 * **Type:** GPUTexture
	 * 
	 */
	override val texture: GPUTexture,
	/**
	 * The mip-map level of the texture to copy to or from. This field defaults to `0` if not specified.
	 * 
	 * **Type:** GPUIntegerCoordinate
	 * 
	 * **Default Value:** 0
	 * 
	 */
	override val mipLevel: GPUIntegerCoordinate = 0u,
	/**
	 * Defines the origin of the copy, which is the minimum corner of the texture sub-region to copy to or from. Together with `copySize`, this defines the full copy sub-region. This field defaults to `{}` if not specified.
	 * 
	 * **Type:** GPUOrigin3D
	 * 
	 * **Default Value:** `{}` (an empty GPUOrigin3D object)
	 * 
	 */
	override val origin: GPUOrigin3D = Origin3D(),
	/**
	 * Defines which aspects of the texture to copy to or from. This field defaults to `all` if not specified.
	 * 
	 * **Type:** GPUTextureAspect
	 * 
	 * **Default Value:** GPUTextureAspect.ALL
	 * 
	 */
	override val aspect: GPUTextureAspect = GPUTextureAspect.All
): GPUTexelCopyTextureInfo

/**
 * The `GPUCommandBufferDescriptor` interface represents a descriptor for creating command buffers in WebGPU. This interface inherits from [GPUObjectDescriptorBase], providing a base set of properties and methods that are common to all GPU object descriptors.
 * 
 * A command buffer is a sequence of commands that can be submitted to the GPU for execution. The `GPUCommandBufferDescriptor` specifies the configuration options for creating these command buffers, such as label and usage flags.
 * 
 * This interface is used when calling [GPUDevice.createCommandBuffer] to create a new command buffer with the specified configuration.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUCommandBufferDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpucommandbufferdescriptor)
 * 
 */
data class CommandBufferDescriptor(
	override val label: String = ""
): GPUCommandBufferDescriptor

/**
 * The `GPUCommandEncoderDescriptor` interface represents a descriptor used to create a [GPUCommandEncoder](https://www.w3.org/TR/webgpu/#gpucommandencoder) object. This descriptor inherits from the base descriptor interface [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase).
 * 
 * The `GPUCommandEncoderDescriptor` is used to specify configuration options for the command encoder, such as label and device.
 * 
 * **See Also:**
 * - [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase)
 * 
 */
data class CommandEncoderDescriptor(
	override val label: String = ""
): GPUCommandEncoderDescriptor

/**
 * Represents a dictionary that specifies the query set and indices where timestamps will be written during a compute pass. This interface is used to measure the duration of compute passes by recording timestamps at the beginning and end of the pass.
 * 
 * For more details, refer to the [WebGPU specification on GPUComputePassTimestampWrites](https://www.w3.org/TR/webgpu/#dictdef-gpucomputepasstimestampwrites).
 * 
 */
data class ComputePassTimestampWrites(
	/**
	 * The `GPUQuerySet` of type "timestamp" that the query results will be written to. This set contains the queries where the timestamps will be recorded.
	 * 
	 * **Type**: [GPUQuerySet](https://www.w3.org/TR/webgpu/#gpuqueryset)
	 * 
	 */
	override val querySet: GPUQuerySet,
	/**
	 * If defined, indicates the query index in `querySet` into which the timestamp at the beginning of the compute pass will be written. This value is of type [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32).
	 * 
	 * **Type**: GPUSize32?
	 * 
	 */
	override val beginningOfPassWriteIndex: GPUSize32? = null,
	/**
	 * If defined, indicates the query index in `querySet` into which the timestamp at the end of the compute pass will be written. This value is of type [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32).
	 * 
	 * **Type**: GPUSize32?
	 * 
	 */
	override val endOfPassWriteIndex: GPUSize32? = null
): GPUComputePassTimestampWrites

/**
 * Represents a descriptor for configuring a compute pass in WebGPU. This interface extends [GPUObjectDescriptorBase] and is used to specify the details of a compute pass, including timestamp writes.
 * 
 * For more information, see the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpucomputepassdescriptor).
 * 
 */
data class ComputePassDescriptor(
	/**
	 * Defines which timestamp values will be written for this pass and where to write them.
	 * 
	 * This property is of type [GPUComputePassTimestampWrites].
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucomputepassdescriptor-timestampwrites).
	 * 
	 */
	override val timestampWrites: GPUComputePassTimestampWrites? = null,
	override val label: String = ""
): GPUComputePassDescriptor

/**
 * Represents a dictionary that specifies the query set and indices where timestamps will be written during a render pass. This interface is used to capture timing information at the beginning and end of a render pass.
 * 
 * For more details, refer to the [WebGPU specification on GPURenderPassTimestampWrites](https://www.w3.org/TR/webgpu/#dom-gpurenderpasstimestampwrites).
 * 
 */
data class RenderPassTimestampWrites(
	/**
	 * The GPUQuerySet of type `timestamp` that the query results will be written to.
	 * 
	 * This property is required and specifies the query set where the timestamps will be recorded.
	 * 
	 */
	override val querySet: GPUQuerySet,
	/**
	 * An optional index in the [querySet] that indicates where the timestamp at the beginning of the render pass will be written.
	 * 
	 * If defined, this property specifies the exact query index within the `querySet` where the start timestamp of the render pass will be recorded.
	 * 
	 */
	override val beginningOfPassWriteIndex: GPUSize32? = null,
	/**
	 * An optional index in the [querySet] that indicates where the timestamp at the end of the render pass will be written.
	 * 
	 * If defined, this property specifies the exact query index within the `querySet` where the end timestamp of the render pass will be recorded.
	 * 
	 */
	override val endOfPassWriteIndex: GPUSize32? = null
): GPURenderPassTimestampWrites

/**
 * The `GPURenderPassDescriptor` interface defines the configuration for a render pass in WebGPU. It specifies the color attachments, depth/stencil attachment, occlusion query set, timestamp writes, and maximum draw count for the render pass.
 * 
 * This descriptor is used to configure the rendering process by specifying how different types of data will be handled during the render pass. The `colorAttachments` property defines which color buffers will receive the output from the render pass. The `depthStencilAttachment` specifies the depth/stencil buffer that will be used for depth testing and stencil operations. The `occlusionQuerySet` allows for occlusion queries to be performed, and the `timestampWrites` can be used to write timestamps during the render pass.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpurenderpassdescriptor).
 * 
 */
data class RenderPassDescriptor(
	/**
	 * The `colorAttachments` property is a list of `GPURenderPassColorAttachment` objects that define the color attachments for the render pass. Each attachment specifies how the output from the render pass will be written to a particular color buffer.
	 * 
	 * Due to usage compatibility, no color attachment may alias another attachment or any resource used inside the render pass.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdescriptor-colorattachments).
	 * 
	 */
	override val colorAttachments: List<GPURenderPassColorAttachment>,
	/**
	 * The `depthStencilAttachment` property specifies a `GPURenderPassDepthStencilAttachment` object that defines the depth/stencil attachment for the render pass. This attachment is used for depth testing and stencil operations during the rendering process.
	 * 
	 * Due to usage compatibility, no writable depth/stencil attachment may alias another attachment or any resource used inside the render pass.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdescriptor-depthstencilattachment).
	 * 
	 */
	override val depthStencilAttachment: GPURenderPassDepthStencilAttachment? = null,
	/**
	 * The `occlusionQuerySet` property specifies a `GPUQuerySet` object that defines where the occlusion query results will be stored for this render pass. Occlusion queries are used to determine whether certain pixels were rendered during the pass.
	 * 
	 */
	override val occlusionQuerySet: GPUQuerySet? = null,
	/**
	 * The `timestampWrites` property allows you to specify a list of `GPUQuerySet` objects that define where timestamp query results will be stored for this render pass. Timestamps are used to measure the time taken by different parts of the rendering process.
	 * 
	 */
	override val timestampWrites: GPURenderPassTimestampWrites? = null,
	/**
	 * The `maxDrawCount` property specifies the maximum number of draw calls that can be made during the render pass. This is useful for optimizing performance and managing resources efficiently.
	 * 
	 * Setting an appropriate value for `maxDrawCount` helps in preventing resource exhaustion and ensures smooth rendering.
	 * 
	 */
	override val maxDrawCount: GPUSize64 = 50000000u,
	override val label: String = ""
): GPURenderPassDescriptor

/**
 * Represents a color attachment for a render pass in the WebGPU API. This interface defines the properties required to configure how colors are rendered and stored during a rendering operation.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpurenderpasscolorattachment).
 * 
 */
data class RenderPassColorAttachment(
	/**
	 * A GPUTextureView describing the texture subresource that will be output to for this color attachment.
	 * 
	 * This property is required and must be a valid renderable texture view. The format of the view must be a color renderable format.
	 * 
	 */
	override val view: GPUTextureView,
	/**
	 * Indicates the load operation to perform on the GPUTextureView prior to executing the render pass.
	 * 
	 * This property is required and specifies how the contents of the view should be handled before rendering. It can be one of the following values: "clear", "load", or "dont-care".
	 * 
	 */
	override val loadOp: GPULoadOp,
	/**
	 * The store operation to perform on the GPUTextureView after executing the render pass.
	 * 
	 * This property is required and specifies how the contents of the view should be handled after rendering. It can be one of the following values: "store", or "dont-store"
	 * 
	 */
	override val storeOp: GPUStoreOp,
	/**
	 * Indicates the depth slice index of the GPUTextureView that will be output to for this color attachment when the view's dimension is "3d".
	 * 
	 * This property is optional and must only be provided if the GPUTextureView's dimension is "3d"
	 * 
	 */
	override val depthSlice: GPUIntegerCoordinate? = null,
	/**
	 * A GPUTextureView describing the texture subresource that will receive the resolved output for this color attachment if the GPUTextureView is multisampled.
	 * 
	 * This property is optional and must only be provided if the GPUTextureView's sample count is greater than 1. The resolve target must have a sample count of 1.
	 * 
	 */
	override val resolveTarget: GPUTextureView? = null,
	/**
	 * Indicates the value to clear the GPUTextureView to prior to executing the render pass.
	 * 
	 * This property is optional and defaults to {r: 0, g: 0, b: 0, a: 0} if not provided. It is ignored if the loadOp is not "clear". The components of clearValue are converted to a texel value of the texture format matching the render attachment.
	 * 
	 */
	override val clearValue: GPUColor? = null
): GPURenderPassColorAttachment

/**
 * The `GPURenderPassDepthStencilAttachment` interface represents a depth/stencil attachment for a render pass. It specifies the texture view and various operations to be performed on the depth and stencil components of that view during the render pass.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#gpurenderpassdepthstencilattachment).
 * 
 */
data class RenderPassDepthStencilAttachment(
	/**
	 * A `GPUTextureView` describing the texture subresource that will be output to and read from for this depth/stencil attachment.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-view).
	 * 
	 */
	override val view: GPUTextureView,
	/**
	 * Indicates the value to clear the `view`'s depth component to prior to executing the render pass. This value is ignored if `depthLoadOp` is not set to `GPULoadOp.CLEAR`. The value must be between 0.0 and 1.0, inclusive.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthclearvalue).
	 * 
	 */
	override val depthClearValue: Float? = null,
	/**
	 * Indicates the load operation to perform on the `view`'s depth component prior to executing the render pass. It is recommended to prefer clearing; see `GPULoadOp.CLEAR` for details.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthloadop).
	 * 
	 */
	override val depthLoadOp: GPULoadOp? = null,
	/**
	 * The store operation to perform on the `view`'s depth component after executing the render pass.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthstoreop).
	 * 
	 */
	override val depthStoreOp: GPUStoreOp? = null,
	/**
	 * Indicates that the depth component of the `view` is read-only. Defaults to `false`.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthreadonly).
	 * 
	 */
	override val depthReadOnly: Boolean = false,
	/**
	 * Indicates the value to clear the `view`'s stencil component to prior to executing the render pass. This value is ignored if `stencilLoadOp` is not set to `GPULoadOp.CLEAR`. The value will be converted to the type of the stencil aspect of the `view` by taking the same number of least significant bits (LSBs) as the number of bits in the stencil aspect of one texel of the `view`.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilclearvalue).
	 * 
	 */
	override val stencilClearValue: GPUStencilValue = 0u,
	/**
	 * Indicates the load operation to perform on the `view`'s stencil component prior to executing the render pass. It is recommended to prefer clearing; see `GPULoadOp.CLEAR` for details.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilloadop).
	 * 
	 */
	override val stencilLoadOp: GPULoadOp? = null,
	/**
	 * The store operation to perform on the `view`'s stencil component after executing the render pass.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilstoreop).
	 * 
	 */
	override val stencilStoreOp: GPUStoreOp? = null,
	/**
	 * Indicates that the stencil component of the `view` is read-only. Defaults to `false`.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilreadonly).
	 * 
	 */
	override val stencilReadOnly: Boolean = false
): GPURenderPassDepthStencilAttachment

/**
 * Represents the layout of a render pass, specifying the formats and sample counts for color and depth/stencil attachments.
 * 
 * This interface is used to define the configuration of a render pass, which includes the formats of the color attachments and the optional depth/stencil attachment. It also specifies the number of samples per pixel in the attachments.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#gpurenderpasslayout).
 * 
 */
data class RenderPassLayout(
	/**
	 * A list of the [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat)s of the color attachments for this pass or bundle.
	 * 
	 * This property specifies the formats of the color attachments that will be used in the render pass. Each format corresponds to a texture view that will be rendered to during the pass.
	 * 
	 */
	override val colorFormats: List<GPUTextureFormat>,
	/**
	 * The [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat) of the depth/stencil attachment for this pass or bundle.
	 * 
	 * This property specifies the format of the depth/stencil attachment that will be used in the render pass. It is optional and can be `null` if no depth/stencil attachment is required.
	 * 
	 */
	override val depthStencilFormat: GPUTextureFormat? = null,
	/**
	 * Number of samples per pixel in the attachments for this pass or bundle.
	 * 
	 * This property specifies the number of samples per pixel for multisampling. The default value is `1`, which means no multisampling.
	 * 
	 */
	override val sampleCount: GPUSize32 = 1u,
	override val label: String = ""
): GPURenderPassLayout

/**
 * Represents a descriptor for creating a [GPURenderBundle]. This interface inherits from [GPUObjectDescriptorBase], which provides common properties and methods for GPU objects.
 * 
 * The `GPURenderBundleDescriptor` is used to specify the configuration options when creating a render bundle. A render bundle encapsulates a sequence of rendering commands that can be executed multiple times with different parameters, improving performance by reducing the overhead of command encoding.
 * 
 */
data class RenderBundleDescriptor(
	override val label: String = ""
): GPURenderBundleDescriptor

/**
 * Represents a descriptor for creating a GPURenderBundleEncoder. This interface extends the GPURenderPassLayout and is used to specify whether the depth or stencil components of a render pass are read-only.
 * 
 * @see [WebGPU Specification - GPURenderBundleEncoderDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpurenderbundleencoderdescriptor)
 * 
 */
data class RenderBundleEncoderDescriptor(
	override val colorFormats: List<GPUTextureFormat>,
	/**
	 * Indicates whether the render bundle modifies the depth component of the GPURenderPassDepthStencilAttachment in any render pass it is executed in.
	 * 
	 * If set to true, the depth component is read-only. This can be useful for optimizing performance by avoiding unnecessary writes to the depth buffer.
	 * 
	 * @return A Boolean value indicating whether the depth component is read-only.
	 * @default false
	 * 
	 */
	override val depthReadOnly: Boolean = false,
	/**
	 * Indicates whether the render bundle modifies the stencil component of the GPURenderPassDepthStencilAttachment in any render pass it is executed in.
	 * 
	 * If set to true, the stencil component is read-only. This can be useful for optimizing performance by avoiding unnecessary writes to the stencil buffer.
	 * 
	 * @return A Boolean value indicating whether the stencil component is read-only.
	 * @default false
	 * 
	 */
	override val stencilReadOnly: Boolean = false,
	override val depthStencilFormat: GPUTextureFormat? = null,
	override val sampleCount: GPUSize32 = 1u,
	override val label: String = ""
): GPURenderBundleEncoderDescriptor

/**
 * The `GPUQueueDescriptor` interface describes a queue request in the WebGPU API. This dictionary inherits from [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase), which means it includes all properties and methods defined by that base class.
 * 
 * The `GPUQueueDescriptor` is used to configure and create GPU queues, which are responsible for submitting commands to the GPU. This interface does not define any additional properties beyond those inherited from [GPUObjectDescriptorBase].
 * 
 * **See Also:**
 * - [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase) for inherited properties and methods.
 * 
 */
data class QueueDescriptor(
	override val label: String = ""
): GPUQueueDescriptor

/**
 * Represents a descriptor for creating a [GPUQuerySet](https://www.w3.org/TR/webgpu/#gpuqueryset) object. This interface extends [GPUObjectDescriptorBase], providing the necessary configuration parameters to define the type and count of queries managed by the query set.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUQuerySetDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpuquerysetdescriptor)
 * 
 */
data class QuerySetDescriptor(
	/**
	 * Specifies the type of queries managed by the [GPUQuerySet]. This property is required and must be set to one of the values defined in the [GPUQueryType](https://www.w3.org/TR/webgpu/#enumdef-gpuquerytype) enum.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: type](https://www.w3.org/TR/webgpu/#dom-gpuquerysetdescriptor-type)
	 * 
	 */
	override val type: GPUQueryType,
	/**
	 * Specifies the number of queries managed by the [GPUQuerySet]. This property is required and must be set to a valid [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32) value.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: count](https://www.w3.org/TR/webgpu/#dom-gpuquerysetdescriptor-count)
	 * 
	 */
	override val count: GPUSize32,
	override val label: String = ""
): GPUQuerySetDescriptor
