@file:Suppress("unused")
// This file has been generated DO NO EDIT
package io.ygdrasil.webgpu

/**
 * Represents a binding resource in the WebGPU API. This sealed interface can be one of several types:
 * - [GPUSampler]
 * - [GPUTextureView]
 * - [GPUBufferBinding]
 * - [GPUExternalTexture]
 * 
 * This interface is used to specify the type of resource that can be bound in a bind group. For more details, refer to the
 * [WebGPU specification on GPUBindingResource](https://www.w3.org/TR/webgpu/#typedefdef-gpubindingresource).
 * 
 */
sealed interface GPUBindingResource
/**
 * The `GPUBuffer` interface represents a block of memory that can be used in GPU operations. Data is stored in linear layout, meaning each byte of the allocation can be addressed by its offset from the start of the buffer, subject to alignment restrictions depending on the operation. Some buffers can be mapped, making the block of memory accessible via an `ArrayBuffer` called its mapping.
 * 
 * Buffers are created via [GPUDevice.createBuffer()](https://www.w3.org/TR/webgpu/#dom-gpudevice-createbuffer). Buffers may be [mappedAtCreation](https://www.w3.org/TR/webgpu/#dom-gpubufferdescriptor-mappedatcreation).
 * 
 * Refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpubuffer) for more details.
 * 
 */
interface GPUBuffer : GPUBindingResource, GPUObjectBase, AutoCloseable {
	/**
	 * The `size` property returns the size of the buffer in bytes. This value is read-only and represents the total allocated memory for this buffer.
	 * 
	 * **Type:** [GPUSize64Out](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64out)
	 * 
	 */
	val size: GPUSize64Out
	/**
	 * The `usage` property specifies how the buffer can be used. This value is read-only and represents a combination of flags indicating the allowed operations on this buffer.
	 * 
	 * **Type:** [GPUBufferUsageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gpuflagsconstant)
	 * 
	 */
	val usage: Set<GPUBufferUsage>
	/**
	 * The `mapState` property indicates the current mapping state of the buffer. This value is read-only and can be one of the following: `unmapped`, `pending`, or `mapped`.
	 * 
	 * **Type:** [GPUBufferMapState](https://www.w3.org/TR/webgpu/#enumdef-gpubuffermapstate)
	 * 
	 */
	val mapState: GPUBufferMapState
	/**
	 * The `mapAsync` function asynchronously maps the buffer into an `ArrayBuffer`. This operation is non-blocking and returns a [Result](https://kotlinlang.org/api/latest/kotlinx-coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-result/) indicating success or failure.
	 * 
	 * **Parameters:**
	 * - `mode`: The mapping mode, which can be either [GPUMapModeRead] or [GPUMapModeWrite].
	 * - `offset`: (Optional) The offset within the buffer to start mapping. Defaults to 0.
	 * - `size`: (Optional) The size of the range to map. If null, maps from the offset to the end of the buffer.
	 * 
	 * **Returns:** A [Result](https://kotlinlang.org/api/latest/kotlinx-coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-result/) indicating success or failure.
	 * 
	 */
	suspend fun mapAsync(mode: GPUMapMode, offset: GPUSize64 = 0u, size: GPUSize64? = null): Result<Unit>
	/**
	 * The `getMappedRange` function returns an `ArrayBuffer` representing the mapped range of the buffer. This method can only be called when the buffer is in the `mapped` state.
	 * 
	 * **Parameters:**
	 * - `offset`: (Optional) The offset within the buffer to start mapping. Defaults to 0.
	 * - `size`: (Optional) The size of the range to map. If null, maps from the offset to the end of the buffer.
	 * 
	 * **Returns:** An [ArrayBuffer](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/ArrayBuffer) containing the mapped data.
	 * 
	 */
	fun getMappedRange(offset: GPUSize64 = 0u, size: GPUSize64? = null): ArrayBuffer
	/**
	 * The `unmap` function unmaps the buffer, making it no longer accessible via an `ArrayBuffer`. This method can only be called when the buffer is in the `mapped` state.
	 * 
	 */
	fun unmap()
}

/**
 * The `GPUBufferBinding` interface describes a buffer and an optional range to bind as a resource. This is used in the context of WebGPU to specify how buffers should be bound for shader access.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpubufferbinding).
 * 
 */
interface GPUBufferBinding : GPUBindingResource {
	/**
	 * The `buffer` property specifies the `GPUBuffer` to bind. This buffer will be exposed to shaders as a resource.
	 * 
	 * **Type**: [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer)
	 * 
	 */
	val buffer: GPUBuffer
	/**
	 * The `offset` property specifies the offset, in bytes, from the beginning of the `buffer` to the start of the range exposed to the shader by the buffer binding. This value defaults to 0 if not specified.
	 * 
	 * **Type**: [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	val offset: GPUSize64
	/**
	 * The `size` property specifies the size, in bytes, of the buffer binding. If not provided, it specifies the range starting at `offset` and ending at the end of the `buffer`.
	 * 
	 * **Type**: [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	val size: GPUSize64?
}

/**
 * /**
 *  * The `GPUSampler` interface encodes transformations and filtering information that can be used in a shader to interpret texture resource data.
 *  * 
 *  * This interface is created via the [GPUDevice.createSampler()] method.
 *  * 
 *  * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpusampler).
 *  */
 * interface GPUSampler : GPUBindingResource, GPUObjectBase, AutoCloseable
 * 
 */
interface GPUSampler : GPUBindingResource, GPUObjectBase, AutoCloseable
/**
 * Represents a texture in the WebGPU API. A texture is composed of 1D, 2D, or 3D arrays of data that can contain multiple values per element to represent things like colors.
 * Textures can be read and written in various ways depending on their usage flags. They are often stored in GPU memory with a layout optimized for multidimensional access.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#texture-interface).
 * 
 */
interface GPUTexture : GPUBindingResource, GPUObjectBase, GPUTextureOrGPUTextureView, AutoCloseable {
	/**
	 * Represents the width of the texture in texels.
	 * 
	 * **Type:** [GPUIntegerCoordinateOut](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinateout)
	 * 
	 */
	val width: GPUIntegerCoordinateOut
	/**
	 * Represents the height of the texture in texels.
	 * 
	 * **Type:** [GPUIntegerCoordinateOut](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinateout)
	 * 
	 */
	val height: GPUIntegerCoordinateOut
	/**
	 * Represents the depth of the texture in texels for 3D textures or the number of array layers for 2D array textures.
	 * 
	 * **Type:** [GPUIntegerCoordinateOut](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinateout)
	 * 
	 */
	val depthOrArrayLayers: GPUIntegerCoordinateOut
	/**
	 * Represents the number of mipmap levels in the texture.
	 * 
	 * **Type:** [GPUIntegerCoordinateOut](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinateout)
	 * 
	 */
	val mipLevelCount: GPUIntegerCoordinateOut
	/**
	 * Represents the number of samples per pixel in the texture.
	 * 
	 * **Type:** [GPUSize32Out](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32out)
	 * 
	 */
	val sampleCount: GPUSize32Out
	/**
	 * Specifies the dimension of the texture (1D, 2D, or 3D).
	 * 
	 * **Type:** [GPUTextureDimension](https://www.w3.org/TR/webgpu/#enumdef-gputexturedimension)
	 * 
	 */
	val dimension: GPUTextureDimension
	/**
	 * Specifies the format of the texture data.
	 * 
	 * **Type:** [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat)
	 * 
	 */
	val format: GPUTextureFormat
	/**
	 * Specifies the usage flags for the texture, indicating how it can be used (e.g., as a render target, sampler, etc.).
	 * 
	 * **Type:** [GPUTextureUsageFlags](https://www.w3.org/TR/webgpu/#namespacedef-gputextureusage)
	 * 
	 */
	val usage: Set<GPUTextureUsage>
	/**
	 * Creates a view of the texture.
	 * 
	 * **Parameters:**
	 * - `descriptor`: An optional [GPUTextureViewDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gputextureviewdescriptor) that specifies the parameters for creating the texture view. If not provided, default values are used.
	 * 
	 * **Return Type:** [GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview)
	 * 
	 */
	fun createView(descriptor: GPUTextureViewDescriptor? = null): GPUTextureView
}

/**
 * A `GPUTextureView` represents a view onto some subset of the texture subresources defined by a particular [GPUTexture]. This interface allows for efficient access and manipulation of specific portions of a texture, enabling optimized rendering and data processing.
 * 
 * The `GPUTextureView` is part of the WebGPU API and is designed to be used in conjunction with other GPU resources such as [GPUBindGroup] and [GPURenderPipeline]. It provides a way to bind specific texture views to shaders, enabling advanced rendering techniques.
 * 
 * This interface inherits from `GPUBindingResource` and `GPUObjectBase`, which means it can be used as a binding resource in various GPU operations. Additionally, it implements the `AutoCloseable` interface, allowing for proper resource management and cleanup.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview)
 * 
 */
interface GPUTextureView : GPUBindingResource, GPUObjectBase, GPUTextureOrGPUTextureView, AutoCloseable
/**
 * Represents a color in the RGBA format, which can be either a sequence of four `Double` values or a [GPUColorDict]. This interface provides access to the red, green, blue, and alpha channel values.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpucolor).
 * 
 */
interface GPUColor {
	/**
	 * The red channel value of the color. This value is a `Double` representing the intensity of the red component in the RGBA color model.
	 * 
	 * **Type**: Double
	 * 
	 */
	val r: Double
	/**
	 * The green channel value of the color. This value is a `Double` representing the intensity of the green component in the RGBA color model.
	 * 
	 * **Type**: Double
	 * 
	 */
	val g: Double
	/**
	 * The blue channel value of the color. This value is a `Double` representing the intensity of the blue component in the RGBA color model.
	 * 
	 * **Type**: Double
	 * 
	 */
	val b: Double
	/**
	 * The alpha channel value of the color. This value is a `Double` representing the opacity of the color, where 0.0 means fully transparent and 1.0 means fully opaque.
	 * 
	 * **Type**: Double
	 * 
	 */
	val a: Double
}

/**
 * Represents a 2D origin point in GPU coordinates. This interface can be used to specify the starting point for various GPU operations, such as texture sampling or buffer updates.
 * 
 * The `GPUOrigin2D` type is defined as either a sequence of two values or a dictionary with `x` and `y` properties. This allows for flexible initialization and usage in different contexts.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuorigin2d).
 * 
 */
interface GPUOrigin2D {
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
	val x: GPUIntegerCoordinate
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
	val y: GPUIntegerCoordinate
}

/**
 * Represents a 3D origin point in GPU coordinates. This interface can be used to specify the starting point for various GPU operations, such as texture sampling or buffer updates.
 * 
 * The `GPUOrigin3D` type can be either a sequence of three [GPUIntegerCoordinate] values or an instance of [GPUOrigin3DDict]. When accessed, the properties `x`, `y`, and `z` will refer to the corresponding values in the sequence or dictionary.
 * 
 * For more details, see the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuorigin3d).
 * 
 */
interface GPUOrigin3D {
	/**
	 * The x-coordinate of the 3D origin point. This value is either the first item in a sequence of [GPUIntegerCoordinate] values or the `x` property of a [GPUOrigin3DDict].
	 * 
	 * **Type:** [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate)
	 * 
	 * **Default Value:** 0
	 * 
	 */
	val x: GPUIntegerCoordinate
	/**
	 * The y-coordinate of the 3D origin point. This value is either the second item in a sequence of [GPUIntegerCoordinate] values or the `y` property of a [GPUOrigin3DDict].
	 * 
	 * **Type:** [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate)
	 * 
	 * **Default Value:** 0
	 * 
	 */
	val y: GPUIntegerCoordinate
	/**
	 * The z-coordinate of the 3D origin point. This value is either the third item in a sequence of [GPUIntegerCoordinate] values or the `z` property of a [GPUOrigin3DDict].
	 * 
	 * **Type:** [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate)
	 * 
	 * **Default Value:** 0
	 * 
	 */
	val z: GPUIntegerCoordinate
}

/**
 * Represents a 3-dimensional extent, which defines the size of a texture or other GPU resources. This interface can be used to specify dimensions in three axes: width, height, and depth or array layers.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuextent3d).
 * 
 * @see [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict)
 * 
 */
interface GPUExtent3D {
	/**
	 * The width of the extent.
	 * 
	 * This property corresponds to the `width` field in the [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict).
	 * 
	 * @return The width as a [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	val width: GPUIntegerCoordinate
	/**
	 * The height of the extent.
	 * 
	 * This property corresponds to the `height` field in the [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict), which defaults to 1 if not specified.
	 * 
	 * @return The height as a [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	val height: GPUIntegerCoordinate
	/**
	 * The depth of the extent or the number of array layers it contains.
	 * 
	 * This property corresponds to the `depthOrArrayLayers` field in the [GPUExtent3DDict](https://www.w3.org/TR/webgpu/#dictdef-gpuextent3ddict), which defaults to 1 if not specified. If used with a [GPUTexture](https://www.w3.org/TR/webgpu/#gputexture) with a dimension of `"3d"`, it defines the depth of the texture. If used with a `GPUTexture` with a dimension of `"2d"`, it defines the number of array layers in the texture.
	 * 
	 * @return The depth or number of array layers as a [GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	val depthOrArrayLayers: GPUIntegerCoordinate
}

/**
 * The `GPUObjectBase` interface is a mixin that provides a common base for all WebGPU objects. It includes properties such as a label, which can be used to identify the object in debugging and error messages.
 * 
 * This interface is fundamental to the WebGPU API as it ensures that all WebGPU objects share a consistent set of properties and behaviors. The `label` property allows developers to assign meaningful names to their WebGPU objects, making it easier to debug and manage them.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuobjectbase).
 * 
 */
interface GPUObjectBase {
	/**
	 * A developer-provided label which is used in an implementation-defined way. It can be utilized by the browser, OS, or other tools to help identify the underlying internal object to the developer.
	 * 
	 * This property is particularly useful for debugging purposes as it allows developers to assign meaningful names to their WebGPU objects. These labels can then be displayed in error messages, console warnings, and various debugging utilities.
	 * 
	 * **Type**: `String`
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuobjectbase-label).
	 * 
	 */
	var label: String
}

/**
 * The `GPUSupportedLimits` interface provides access to the supported limits of a GPU adapter or device.
 * These limits define the maximum capabilities and constraints for various resources and operations,
 * such as texture dimensions, bind groups, buffers, and shader stages. This information is crucial
 * for optimizing performance and ensuring compatibility with the underlying hardware.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#gpusupportedlimits).
 * 
 */
interface GPUSupportedLimits {
	/**
	 * The maximum size for a 1D texture dimension. This value represents the largest allowable width
	 * for a 1D texture in texels.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxTextureDimension1D: UInt
	/**
	 * The maximum size for a 2D texture dimension. This value represents the largest allowable width or height
	 * for a 2D texture in texels.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxTextureDimension2D: UInt
	/**
	 * The maximum size for a 3D texture dimension. This value represents the largest allowable depth
	 * for a 3D texture in texels.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxTextureDimension3D: UInt
	/**
	 * The maximum number of layers in a texture array. This value represents the largest allowable number
	 * of layers for a texture array.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxTextureArrayLayers: UInt
	/**
	 * The maximum number of bind groups that can be used in a single pipeline. This value represents the largest allowable
	 * number of bind groups for a pipeline.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxBindGroups: UInt
	/**
	 * The maximum number of bind groups plus vertex buffers that can be used in a single pipeline. This value represents the largest allowable
	 * combined count of bind groups and vertex buffers for a pipeline.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxBindGroupsPlusVertexBuffers: UInt
	/**
	 * The maximum size, in bytes, of immediate data range used in a pipeline. Note: 64 bytes is the size of a 4×4 matrix of f32 values.
	 * 
	 */
	val maxImmediateSize: UInt
	/**
	 * The maximum number of bindings per bind group. This value represents the largest allowable number of bindings for a single bind group.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxBindingsPerBindGroup: UInt
	/**
	 * The maximum number of dynamic uniform buffers per pipeline layout. This value represents the largest allowable number of dynamic uniform buffers for a single pipeline layout.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxDynamicUniformBuffersPerPipelineLayout: UInt
	/**
	 * The maximum number of dynamic storage buffers per pipeline layout. This value represents the largest allowable number of dynamic storage buffers for a single pipeline layout.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxDynamicStorageBuffersPerPipelineLayout: UInt
	/**
	 * The maximum number of sampled textures per shader stage. This value represents the largest allowable number of sampled textures for a single shader stage.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxSampledTexturesPerShaderStage: UInt
	/**
	 * The maximum number of samplers per shader stage. This value represents the largest allowable number of samplers for a single shader stage.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxSamplersPerShaderStage: UInt
	/**
	 * The maximum number of storage buffers per shader stage. This value represents the largest allowable number of storage buffers for a single shader stage.
	 * 
	 * **Type:** `UInt`
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
	 * The maximum number of storage textures per shader stage. This value represents the largest allowable number of storage textures for a single shader stage.
	 * 
	 * **Type:** `UInt`
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
	 * The maximum number of uniform buffers per shader stage. This value represents the largest allowable number of uniform buffers for a single shader stage.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxUniformBuffersPerShaderStage: UInt
	/**
	 * The maximum size of a uniform buffer binding. This value represents the largest allowable size for a single uniform buffer binding in bytes.
	 * 
	 * **Type:** `ULong`
	 * 
	 */
	val maxUniformBufferBindingSize: ULong
	/**
	 * The maximum size of a storage buffer binding. This value represents the largest allowable size for a single storage buffer binding in bytes.
	 * 
	 * **Type:** `ULong`
	 * 
	 */
	val maxStorageBufferBindingSize: ULong
	/**
	 * The minimum alignment for uniform buffer offsets. This value represents the smallest allowable offset alignment for a uniform buffer in bytes.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val minUniformBufferOffsetAlignment: UInt
	/**
	 * The minimum alignment for storage buffer offsets. This value represents the smallest allowable offset alignment for a storage buffer in bytes.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val minStorageBufferOffsetAlignment: UInt
	/**
	 * The maximum number of vertex buffers that can be used in a single pipeline. This value represents the largest allowable number of vertex buffers for a pipeline.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxVertexBuffers: UInt
	/**
	 * The maximum size of a buffer. This value represents the largest allowable size for a single buffer in bytes.
	 * 
	 * **Type:** `ULong`
	 * 
	 */
	val maxBufferSize: ULong
	/**
	 * The maximum number of vertex attributes that can be used in a single pipeline. This value represents the largest allowable number of vertex attributes for a pipeline.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxVertexAttributes: UInt
	/**
	 * The maximum stride for a vertex buffer array. This value represents the largest allowable stride for a single vertex buffer array in bytes.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxVertexBufferArrayStride: UInt
	/**
	 * The maximum number of inter-stage shader variables that can be used in a single pipeline. This value represents the largest allowable number of inter-stage shader variables for a pipeline.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxInterStageShaderVariables: UInt
	/**
	 * The maximum number of color attachments that can be used in a single render pass. This value represents the largest allowable number of color attachments for a render pass.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxColorAttachments: UInt
	/**
	 * The maximum number of bytes per sample for a color attachment. This value represents the largest allowable number of bytes per sample for a single color attachment.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxColorAttachmentBytesPerSample: UInt
	/**
	 * The maximum size of storage for a compute workgroup. This value represents the largest allowable size for a single compute workgroup's storage in bytes.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxComputeWorkgroupStorageSize: UInt
	/**
	 * The maximum number of compute invocations per workgroup. This value represents the largest allowable number of compute invocations for a single workgroup.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxComputeInvocationsPerWorkgroup: UInt
	/**
	 * The maximum size for the X dimension of a compute workgroup. This value represents the largest allowable size for the X dimension of a single compute workgroup.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxComputeWorkgroupSizeX: UInt
	/**
	 * The maximum size for the Y dimension of a compute workgroup. This value represents the largest allowable size for the Y dimension of a single compute workgroup.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxComputeWorkgroupSizeY: UInt
	/**
	 * The maximum size for the Z dimension of a compute workgroup. This value represents the largest allowable size for the Z dimension of a single compute workgroup.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxComputeWorkgroupSizeZ: UInt
	/**
	 * The maximum number of compute workgroups per dimension. This value represents the largest allowable number of compute workgroups for a single dimension.
	 * 
	 * **Type:** `UInt`
	 * 
	 */
	val maxComputeWorkgroupsPerDimension: UInt
}

/**
 * The `GPUAdapterInfo` interface exposes various identifying information about an adapter. None of the members in `GPUAdapterInfo` are guaranteed to be populated with any particular value; if no value is provided, the attribute will return the empty string " ". It is at the user agent’s discretion which values to reveal, and it is likely that on some devices none of the values will be populated. As such, applications **must** be able to handle any possible `GPUAdapterInfo` values, including the absence of those values.
 * 
 * The `GPUAdapterInfo` for an adapter is exposed via [GPUAdapter.info](https://www.w3.org/TR/webgpu/#dom-gpuadapter-info) and [GPUDevice.adapterInfo](https://www.w3.org/TR/webgpu/#dom-gpudevice-adapterinfo). This info is immutable: for a given adapter, each `GPUAdapterInfo` attribute will return the same value every time it’s accessed.
 * 
 * **Note:** Though the `GPUAdapterInfo` attributes are immutable *once accessed*, an implementation may delay the decision on what to expose for each attribute until the first time it is accessed.
 * 
 */
interface GPUAdapterInfo {
	/**
	 * The `vendor` property returns a string identifying the vendor of the GPU adapter. This value may be an empty string if the user agent chooses not to reveal this information.
	 * 
	 */
	val vendor: String
	/**
	 * The `architecture` property returns a string identifying the architecture of the GPU adapter. This value may be an empty string if the user agent chooses not to reveal this information.
	 * 
	 */
	val architecture: String
	/**
	 * The `device` property returns a string identifying the device name of the GPU adapter. This value may be an empty string if the user agent chooses not to reveal this information.
	 * 
	 */
	val device: String
	/**
	 * The `description` property returns a string providing a description of the GPU adapter. This value may be an empty string if the user agent chooses not to reveal this information.
	 * 
	 */
	val description: String
	/**
	 * The `subgroupMinSize` property returns the minimum size of a subgroup for the GPU adapter. This value is represented as an unsigned integer (`UInt`).
	 * 
	 */
	val subgroupMinSize: UInt
	/**
	 * The `subgroupMaxSize` property returns the maximum size of a subgroup for the GPU adapter. This value is represented as an unsigned integer (`UInt`).
	 * 
	 */
	val subgroupMaxSize: UInt
	/**
	 * The `isFallbackAdapter` property returns a boolean indicating whether the adapter is a fallback adapter. A fallback adapter is used when the preferred adapter is not available.
	 * 
	 */
	val isFallbackAdapter: Boolean
}

/**
 * The `GPUAdapter` interface encapsulates a GPU adapter and describes its capabilities, including supported features and limits. This interface is essential for interacting with the underlying GPU hardware and obtaining a `GPUDevice` to perform rendering operations.
 * 
 * To obtain a `GPUAdapter`, use the `requestAdapter()` method provided by the `GPU` object. The `GPUAdapter` provides read-only access to its features, limits, and information about the adapter.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuadapter).
 * 
 */
interface GPUAdapter : AutoCloseable {
	/**
	 * Represents the set of features supported by the GPU adapter. This property is read-only and provides information about the capabilities of the underlying hardware.
	 * 
	 * The `features` attribute contains a `GPUSupportedFeatures` object, which includes boolean flags indicating whether specific features are supported.
	 * 
	 */
	val features: GPUSupportedFeatures
	/**
	 * Represents the limits imposed by the GPU adapter. This property is read-only and provides information about the constraints of the underlying hardware.
	 * 
	 * The `limits` attribute contains a `GPUSupportedLimits` object, which includes various limit values such as maximum texture dimensions, maximum bind groups, etc.
	 * 
	 */
	val limits: GPUSupportedLimits
	/**
	 * Provides information about the physical adapter underlying this `GPUAdapter`. This property is read-only and returns a `GPUAdapterInfo` object.
	 * 
	 * The `info` attribute contains details such as the name of the adapter, vendor ID, device ID, etc. These values are constant over time for a given `GPUAdapter`.
	 * 
	 */
	val info: GPUAdapterInfo
	/**
	 * Asynchronously requests a `GPUDevice` from the adapter. This method returns a `Result<GPUDevice>`, which resolves to a `GPUDevice` instance if successful.
	 * 
	 * The `descriptor` parameter is optional and allows specifying configuration options for the device, such as enabling specific features or setting default queue properties.
	 * 
	 */
	suspend fun requestDevice(descriptor: GPUDeviceDescriptor? = null): Result<GPUDevice>
}

/**
 * The `GPUDevice` interface encapsulates a GPU device and exposes the functionality of that device. It is the top-level interface through which WebGPU interfaces are created.
 * 
 * To obtain a `GPUDevice`, use the `requestDevice()` method on a `GPUAdapter`.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUDevice](https://www.w3.org/TR/webgpu/#gpudevice)
 * 
 */
interface GPUDevice : GPUObjectBase, AutoCloseable {
	/**
	 * Represents the supported features of the GPU device.
	 * 
	 * **Type:** [GPUSupportedFeatures]
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUSupportedFeatures](https://www.w3.org/TR/webgpu/#gpusupportedfeatures)
	 * 
	 */
	val features: GPUSupportedFeatures
	/**
	 * Represents the supported limits of the GPU device.
	 * 
	 * **Type:** [GPUSupportedLimits]
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUSupportedLimits](https://www.w3.org/TR/webgpu/#gpusupportedlimits)
	 * 
	 */
	val limits: GPUSupportedLimits
	/**
	 * Provides information about the GPU adapter associated with this device.
	 * 
	 * **Type:** [GPUAdapterInfo]
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUAdapterInfo](https://www.w3.org/TR/webgpu/#gpuadapterinfo)
	 * 
	 */
	val adapterInfo: GPUAdapterInfo
	/**
	 * Represents the command queue associated with this device.
	 * 
	 * **Type:** [GPUQueue]
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUQueue](https://www.w3.org/TR/webgpu/#gpuqueue)
	 * 
	 */
	val queue: GPUQueue
	/**
	 * Creates a new buffer object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUBufferDescriptor] that specifies the properties of the buffer to be created.
	 * 
	 * **Returns:** A new [GPUBuffer] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createBuffer](https://www.w3.org/TR/webgpu/#dom-gpudevice-createbuffer)
	 * 
	 */
	fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer
	/**
	 * Creates a new texture object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUTextureDescriptor] that specifies the properties of the texture to be created.
	 * 
	 * **Returns:** A new [GPUTexture] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createTexture](https://www.w3.org/TR/webgpu/#dom-gpudevice-createtexture)
	 * 
	 */
	fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture
	/**
	 * Creates a new sampler object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: An optional [GPUSamplerDescriptor] that specifies the properties of the sampler to be created. If not provided, default values are used.
	 * 
	 * **Returns:** A new [GPUSampler] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createSampler](https://www.w3.org/TR/webgpu/#dom-gpudevice-createsampler)
	 * 
	 */
	fun createSampler(descriptor: GPUSamplerDescriptor? = null): GPUSampler
	/**
	 * Creates a new bind group layout object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUBindGroupLayoutDescriptor] that specifies the properties of the bind group layout to be created.
	 * 
	 * **Returns:** A new [GPUBindGroupLayout] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createBindGroupLayout](https://www.w3.org/TR/webgpu/#dom-gpudevice-createbindgrouplayout)
	 * 
	 */
	fun createBindGroupLayout(descriptor: GPUBindGroupLayoutDescriptor): GPUBindGroupLayout
	/**
	 * Creates a new pipeline layout object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUPipelineLayoutDescriptor] that specifies the properties of the pipeline layout to be created.
	 * 
	 * **Returns:** A new [GPUPipelineLayout] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createPipelineLayout](https://www.w3.org/TR/webgpu/#dom-gpudevice-createpipelinelayout)
	 * 
	 */
	fun createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): GPUPipelineLayout
	/**
	 * Creates a new bind group object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUBindGroupDescriptor] that specifies the properties of the bind group to be created.
	 * 
	 * **Returns:** A new [GPUBindGroup] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createBindGroup](https://www.w3.org/TR/webgpu/#dom-gpudevice-createbindgroup)
	 * 
	 */
	fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup
	/**
	 * Creates a new shader module object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUShaderModuleDescriptor] that specifies the properties of the shader module to be created.
	 * 
	 * **Returns:** A new [GPUShaderModule] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createShaderModule](https://www.w3.org/TR/webgpu/#dom-gpudevice-createshadermodule)
	 * 
	 */
	fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule
	/**
	 * Creates a new compute pipeline object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUComputePipelineDescriptor] that specifies the properties of the compute pipeline to be created.
	 * 
	 * **Returns:** A new [GPUComputePipeline] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createComputePipeline](https://www.w3.org/TR/webgpu/#dom-gpudevice-createcomputepipeline)
	 * 
	 */
	fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): GPUComputePipeline
	/**
	 * Creates a new render pipeline object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPURenderPipelineDescriptor] that specifies the properties of the render pipeline to be created.
	 * 
	 * **Returns:** A new [GPURenderPipeline] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createRenderPipeline](https://www.w3.org/TR/webgpu/#dom-gpudevice-createrenderpipeline)
	 * 
	 */
	fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): GPURenderPipeline
	/**
	 * Asynchronously creates a new compute pipeline object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUComputePipelineDescriptor] that specifies the properties of the compute pipeline to be created.
	 * 
	 * **Returns:** A [Result] containing the newly created [GPUComputePipeline] object or an error if the creation fails.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createComputePipelineAsync](https://www.w3.org/TR/webgpu/#dom-gpudevice-createcomputepipelineasync)
	 * 
	 */
	suspend fun createComputePipelineAsync(descriptor: GPUComputePipelineDescriptor): Result<GPUComputePipeline>
	/**
	 * Asynchronously creates a new render pipeline object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPURenderPipelineDescriptor] that specifies the properties of the render pipeline to be created.
	 * 
	 * **Returns:** A [Result] containing the newly created [GPURenderPipeline] object or an error if the creation fails.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createRenderPipelineAsync](https://www.w3.org/TR/webgpu/#dom-gpudevice-createrenderpipelineasync)
	 * 
	 */
	suspend fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Result<GPURenderPipeline>
	/**
	 * Creates a new command encoder object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: An optional [GPUCommandEncoderDescriptor] that specifies the properties of the command encoder to be created. If not provided, default values are used.
	 * 
	 * **Returns:** A new [GPUCommandEncoder] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createCommandEncoder](https://www.w3.org/TR/webgpu/#dom-gpudevice-createcommandencoder)
	 * 
	 */
	fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor? = null): GPUCommandEncoder
	/**
	 * Creates a new render bundle encoder object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPURenderBundleEncoderDescriptor] that specifies the properties of the render bundle encoder to be created.
	 * 
	 * **Returns:** A new [GPURenderBundleEncoder] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createRenderBundleEncoder](https://www.w3.org/TR/webgpu/#dom-gpudevice-createrenderbundleencoder)
	 * 
	 */
	fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): GPURenderBundleEncoder
	/**
	 * Creates a new query set object based on the provided descriptor.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A [GPUQuerySetDescriptor] that specifies the properties of the query set to be created.
	 * 
	 * **Returns:** A new [GPUQuerySet] object.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: createQuerySet](https://www.w3.org/TR/webgpu/#dom-gpudevice-createqueryset)
	 * 
	 */
	fun createQuerySet(descriptor: GPUQuerySetDescriptor): GPUQuerySet
	/**
	 * Pushes an error scope onto the device's error stack with the specified filter.
	 * 
	 * **Parameters:**
	 * - `filter`: A [GPUErrorFilter] that specifies which errors should be captured within this scope.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: pushErrorScope](https://www.w3.org/TR/webgpu/#dom-gpudevice-pusherrorscope)
	 * 
	 */
	fun pushErrorScope(filter: GPUErrorFilter)
	/**
	 * Pops the top error scope from the device's error stack and returns any captured errors.
	 * 
	 * **Returns:** A [Result] containing a [GPUError] object if an error was captured, or `null` if no error occurred.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: popErrorScope](https://www.w3.org/TR/webgpu/#dom-gpudevice-poperrorscope)
	 * 
	 */
	suspend fun popErrorScope(): Result<GPUError?>
}

/**
 * The `GPUBindGroupLayout` interface defines the structure that specifies how resources are bound in a [GPUBindGroup] and made accessible to shader stages. This layout is crucial for organizing and managing bindings efficiently within the WebGPU pipeline.
 * 
 * **Inheritance:**
 * - Implements `AutoCloseable`
 * - Inherits from [GPUObjectBase]
 * 
 * **Notes:**
 * - The `GPUBindGroupLayout` is immutable once created.
 * - It must be properly closed to free up resources when no longer needed.
 * 
 * **See Also:**
 * - [WebGPU Specification: GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#gpubindgrouplayout)
 * 
 */
interface GPUBindGroupLayout : GPUObjectBase, AutoCloseable
/**
 * A `GPUBindGroup` defines a set of resources to be bound together in a group and specifies how these resources are used in shader stages. This interface is essential for managing the binding of buffers, textures, samplers, and other resources that shaders need during rendering.
 * 
 * The `GPUBindGroup` interface extends [GPUObjectBase], which provides common functionality for GPU objects such as reference counting and lifecycle management. It also implements `AutoCloseable`, allowing for proper resource cleanup when the bind group is no longer needed.
 * 
 * For more details, refer to the [WebGPU specification section on GPUBindGroup](https://www.w3.org/TR/webgpu/#gpubindgroup).
 * 
 * **See Also:**
 * - [GPUObjectBase]
 * 
 */
interface GPUBindGroup : GPUObjectBase, AutoCloseable
/**
 * A [GPUPipelineLayout](https://www.w3.org/TR/webgpu/#gpupipelinelayout) defines the mapping between resources of all [GPUBindGroup](https://www.w3.org/TR/webgpu/#gpubindgroup) objects set up during command encoding in [setBindGroup()](https://www.w3.org/TR/webgpu/#dom-gpubindingcommandsmixin-setbindgroup), and the shaders of the pipeline set by [GPURenderCommandsMixin.setPipeline] or [GPUComputePassEncoder.setPipeline].
 * 
 * This interface extends [GPUObjectBase] and implements [AutoCloseable], allowing for proper resource management.
 * 
 * **See Also:**
 * - [GPUObjectBase]
 * - [AutoCloseable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-auto-closeable/)
 * 
 */
interface GPUPipelineLayout : GPUObjectBase, AutoCloseable
/**
 * The `GPUShaderModule` interface represents a reference to an internal shader module object in the WebGPU API. This interface is used to manage and interact with shader modules, which contain the compiled code for shaders.
 * 
 * A shader module is created from shader source code or precompiled binary data and can be used to create pipeline objects that define how rendering operations are performed.
 * 
 * For more details, refer to the [WebGPU specification on GPUShaderModule](https://www.w3.org/TR/webgpu/#shader-module).
 * 
 * **Inheritance:**
 * - `GPUObjectBase`
 * - `AutoCloseable`
 * 
 */
interface GPUShaderModule : GPUObjectBase, AutoCloseable {
	/**
	 * Retrieves the compilation information for the shader module. This method returns a `Result` object that contains either the `GPUCompilationInfo` or an error indicating why the compilation failed.
	 * 
	 * **Returns:**
	 * - A `Result<GPUCompilationInfo>` containing the compilation information if successful, or an error otherwise.
	 * 
	 */
	suspend fun getCompilationInfo(): Result<GPUCompilationInfo>
}

/**
 * The `GPUCompilationMessage` interface represents an informational, warning, or error message generated by the [GPUShaderModule] compiler. These messages are designed to be human-readable and assist developers in diagnosing issues with their shader code. Each message can correspond to a specific point in the shader code, a substring of the shader code, or may not correspond to any specific point at all.
 * 
 * See also: [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpucompilationmessage)
 * 
 */
interface GPUCompilationMessage {
	/**
	 * A human-readable string that describes the compilation message. This attribute provides detailed information about the issue, warning, or informational note generated during shader compilation.
	 * 
	 * See also: [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpucompilationmessage-message)
	 * 
	 */
	val message: String
	/**
	 * The type of the compilation message, which can be one of the following values from the `GPUCompilationMessageType` enum: "error", "warning", or "info". This attribute helps in categorizing the severity and nature of the message.
	 * 
	 * See also: [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpucompilationmessage-type)
	 * 
	 */
	val type: GPUCompilationMessageType
	/**
	 * The line number within the shader code where the message originates. This attribute is an `ULong` value representing the zero-based index of the line.
	 * 
	 * See also: [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpucompilationmessage-linenum)
	 * 
	 */
	val lineNum: ULong
	/**
	 * The position within the line where the message originates. This attribute is an `ULong` value representing the zero-based index of the character position.
	 * 
	 * See also: [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpucompilationmessage-linepos)
	 * 
	 */
	val linePos: ULong
	/**
	 * The byte offset within the shader code where the message originates. This attribute is an `ULong` value representing the zero-based index of the byte.
	 * 
	 * See also: [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpucompilationmessage-offset)
	 * 
	 */
	val offset: ULong
	/**
	 * The length in bytes of the substring within the shader code that the message refers to. This attribute is an `ULong` value representing the number of bytes.
	 * 
	 * See also: [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpucompilationmessage-length)
	 * 
	 */
	val length: ULong
}

/**
 * Represents the compilation information for a GPU shader module. This interface provides access to messages generated during the compilation process, which can be useful for debugging and optimization.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/).
 * 
 */
interface GPUCompilationInfo {
	/**
	 * A list of [GPUCompilationMessage] objects that contain detailed information about the compilation process. These messages can include warnings and errors that occurred during the compilation of a shader module.
	 * 
	 * **Type**: `List<[GPUCompilationMessage]>`
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpucompilationinfo).
	 * 
	 */
	val messages: List<GPUCompilationMessage>
}

/**
 * The `GPUPipelineBase` interface represents the base class for GPU pipelines in WebGPU. It provides a method to retrieve bind group layouts, which are essential for configuring resources used by shaders.
 * 
 * This interface is part of the WebGPU API and is designed to be implemented by specific pipeline types such as compute pipelines or render pipelines.
 * 
 */
interface GPUPipelineBase {
	/**
	 * Retrieves a `GPUBindGroupLayout` object at the specified index from the pipeline.
	 * 
	 * **Parameters:**
	 * - `index`: A `UInt` representing the index of the bind group layout to retrieve. This value must be within the range of valid indices for the pipeline's bind group layouts.
	 * 
	 * **Returns:**
	 * - A `GPUBindGroupLayout` object that describes the bindings for a specific set of resources used by the shader stages in the pipeline.
	 * 
	 * This method is crucial for setting up resource bindings that shaders will use during execution. The `GPUBindGroupLayout` objects define how resources are bound to the pipeline, including buffers, textures, and samplers.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUPipelineBase](https://www.w3.org/TR/webgpu/#gpupipelinebase)
	 * 
	 */
	fun getBindGroupLayout(index: UInt): GPUBindGroupLayout
}

/**
 * A [GPUComputePipeline](https://www.w3.org/TR/webgpu/#gpucomputepipeline) is a specialized type of pipeline that controls the compute shader stage. It is used within a [GPUComputePassEncoder](https://www.w3.org/TR/webgpu/#gpucomputepassencoder) to execute compute shaders, which are essential for general-purpose computations on the GPU.
 * 
 * This interface extends [GPUObjectBase], [GPUPipelineBase], and implements [AutoCloseable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/-auto-closeable/). It provides methods to manage the lifecycle of compute pipelines, ensuring that resources are properly released when they are no longer needed.
 * 
 * **Important Notes:**
 * - Ensure that the `pipelineDescriptor` is correctly configured with the necessary shader module and other parameters.
 * - Properly manage the lifecycle of the pipeline by closing it when it is no longer needed to avoid memory leaks.
 * 
 * **See Also:**
 * - [GPUComputePassEncoder](https://www.w3.org/TR/webgpu/#gpucomputepassencoder)
 * - [GPUPipelineBase]
 * 
 */
interface GPUComputePipeline : GPUObjectBase, GPUPipelineBase, AutoCloseable
/**
 * A [GPURenderPipeline](https://www.w3.org/TR/webgpu/#gpurenderpipeline) is a type of pipeline that controls the vertex and fragment shader stages. It can be used in both [GPURenderPassEncoder] and [GPURenderBundleEncoder].
 * 
 * This interface extends [GPUObjectBase](https://www.w3.org/TR/webgpu/#gpuobjectbase), [GPUPipelineBase](https://www.w3.org/TR/webgpu/#gpupipelinebase), and implements [AutoCloseable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-auto-closeable/) to manage resource lifecycle.
 * 
 * ### Render Pipeline Inputs
 * - **Bindings**: According to the given [GPUPipelineLayout].
 * - **Vertex and Index Buffers**: Described by [GPUVertexState](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexstate).
 * - **Color Attachments**: Described by [GPUColorTargetState](https://www.w3.org/TR/webgpu/#dictdef-gpucolortargetstate).
 * - **Depth-Stencil Attachment** (optional): Described by [GPUDepthStencilState](https://www.w3.org/TR/webgpu/#dictdef-gpudepthstencilstate).
 * 
 */
interface GPURenderPipeline : GPUObjectBase, GPUPipelineBase, AutoCloseable
/**
 * The `GPUCommandBuffer` interface represents a command buffer in the WebGPU API. It is used to encapsulate a list of GPU commands that can be executed on the [Queue timeline](https://www.w3.org/TR/webgpu/#queue-timeline).
 * 
 * This interface inherits from [`GPUObjectBase`](https://www.w3.org/TR/webgpu/#gpuobjectbase) and implements `AutoCloseable`, allowing for proper resource management.
 * 
 * ### Device Timeline Properties
 * 
 * - **[[command_list]]**: A read-only list of [GPU commands](https://www.w3.org/TR/webgpu/#gpu-command) to be executed when this command buffer is submitted. This property is essential for managing the sequence of operations that will be performed on the GPU.
 * 
 * - **[[renderState]]**: The current state used by any render pass commands being executed. Initially, this property is `null`, but it can be set to a valid [RenderState](https://www.w3.org/TR/webgpu/#renderstate) during the execution of render passes.
 * 
 * In this example, a `GPUCommandBuffer` is created using the `device.createCommandBuffer()` method. Commands are added to the command buffer within a render pass, and finally, the command buffer is submitted to the queue for execution.
 * 
 * ### Notes
 * 
 * - The `GPUCommandBuffer` interface is designed to be used in conjunction with other WebGPU interfaces such as [`GPUDevice`](https://www.w3.org/TR/webgpu/#gpudevice) and [`GPUQueue`](https://www.w3.org/TR/webgpu/#gpuqueue).
 * - Proper management of command buffers is crucial for efficient GPU resource utilization. Always ensure that command buffers are properly closed after use to avoid memory leaks.
 * 
 */
interface GPUCommandBuffer : GPUObjectBase, AutoCloseable
/**
 * The `GPUCommandsMixin` interface defines state common to all interfaces which encode commands. This mixin does not include any methods but serves as a base for other command-encoding interfaces in the WebGPU API.
 * 
 * **Context and Purpose:**
 * This interface is part of the WebGPU specification and is used to provide a consistent way to manage command encoding across different GPU-related interfaces. It ensures that all command-encoding interfaces share a common set of properties or behaviors, even if it does not define any methods itself.
 * 
 * **References:**
 * - [WebGPU Specification: GPUCommandsMixin](https://www.w3.org/TR/webgpu/#gpucommandsmixin)
 * 
 */
interface GPUCommandsMixin
/**
 * The `GPUCommandEncoder` interface represents a command encoder that allows the creation of command buffers for rendering and compute operations. It is part of the WebGPU API, which provides low-level access to GPU capabilities.
 * 
 * This interface includes methods for beginning render and compute passes, copying data between buffers and textures, clearing buffers, resolving query sets, and finishing command encoding.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#command-encoder).
 * 
 * **Included Interfaces:**
 * - `GPUObjectBase`: Provides base functionality for GPU objects.
 * - `GPUCommandsMixin`: Mixin interface for common GPU commands.
 * - `GPUDebugCommandsMixin`: Mixin interface for debug-related GPU commands.
 * 
 * **See Also:**
 * - [GPURenderPassEncoder](https://www.w3.org/TR/webgpu/#gpurenderpassencoder)
 * - [GPUComputePassEncoder](https://www.w3.org/TR/webgpu/#gpucomputepassencoder)
 * 
 */
interface GPUCommandEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, AutoCloseable {
	/**
	 * Begins a render pass using the specified descriptor. This method returns a `GPURenderPassEncoder` that can be used to record rendering commands.
	 * 
	 * **Parameters:**
	 * - `descriptor`: A `GPURenderPassDescriptor` object that specifies the configuration for the render pass.
	 * 
	 * **Returns:**
	 * - A `GPURenderPassEncoder` instance that can be used to record rendering commands within the render pass.
	 * 
	 * **See Also:**
	 * - [GPURenderPassDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpurenderpassdescriptor)
	 * 
	 */
	fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder
	/**
	 * Begins a compute pass using the specified descriptor. This method returns a `GPUComputePassEncoder` that can be used to record compute commands.
	 * 
	 * **Parameters:**
	 * - `descriptor`: An optional `GPUComputePassDescriptor` object that specifies the configuration for the compute pass. If not provided, default values are used.
	 * 
	 * **Returns:**
	 * - A `GPUComputePassEncoder` instance that can be used to record compute commands within the compute pass.
	 * 
	 * **See Also:**
	 * - [GPUComputePassDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpucomputepassdescriptor)
	 * 
	 */
	fun beginComputePass(descriptor: GPUComputePassDescriptor? = null): GPUComputePassEncoder
	/**
	 * Copies data from one buffer to another. This method allows for efficient data transfer between GPU buffers.
	 * 
	 * **Parameters:**
	 * - `source`: The source `GPUBuffer` from which data will be copied.
	 * - `sourceOffset`: The offset within the source buffer where the copy operation will start.
	 * - `destination`: The destination `GPUBuffer` to which data will be copied.
	 * - `destinationOffset`: The offset within the destination buffer where the copy operation will start.
	 * - `size`: An optional parameter specifying the size of the data to be copied. If not provided, the entire range from `sourceOffset` to the end of the source buffer is copied.
	 * 
	 * **See Also:**
	 * - [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer)
	 * 
	 */
	fun copyBufferToBuffer(source: GPUBuffer, sourceOffset: GPUSize64, destination: GPUBuffer, destinationOffset: GPUSize64, size: GPUSize64? = null)
	/**
	 * Copies data from a buffer to a texture. This method allows for efficient transfer of data from a GPU buffer to a GPU texture.
	 * 
	 * **Parameters:**
	 * - `source`: A `GPUTexelCopyBufferInfo` object that specifies the source buffer and its layout.
	 * - `destination`: A `GPUTexelCopyTextureInfo` object that specifies the destination texture and its layout.
	 * - `copySize`: A `GPUExtent3D` object that specifies the size of the data to be copied.
	 * 
	 * **See Also:**
	 * - [GPUTexelCopyBufferInfo](https://www.w3.org/TR/webgpu/#gputexelcopybufferinfo)
	 * - [GPUTexelCopyTextureInfo](https://www.w3.org/TR/webgpu/#gputexelcopytextureinfo)
	 * 
	 */
	fun copyBufferToTexture(source: GPUTexelCopyBufferInfo, destination: GPUTexelCopyTextureInfo, copySize: GPUExtent3D)
	/**
	 * Copies data from a texture to a buffer. This method allows for efficient transfer of data from a GPU texture to a GPU buffer.
	 * 
	 * **Parameters:**
	 * - `source`: A `GPUTexelCopyTextureInfo` object that specifies the source texture and its layout.
	 * - `destination`: A `GPUTexelCopyBufferInfo` object that specifies the destination buffer and its layout.
	 * - `copySize`: A `GPUExtent3D` object that specifies the size of the data to be copied.
	 * 
	 * **See Also:**
	 * - [GPUTexelCopyTextureInfo](https://www.w3.org/TR/webgpu/#gputexelcopytextureinfo)
	 * - [GPUTexelCopyBufferInfo](https://www.w3.org/TR/webgpu/#gputexelcopybufferinfo)
	 * 
	 */
	fun copyTextureToBuffer(source: GPUTexelCopyTextureInfo, destination: GPUTexelCopyBufferInfo, copySize: GPUExtent3D)
	/**
	 * Copies data from one texture to another. This method allows for efficient transfer of data between GPU textures.
	 * 
	 * **Parameters:**
	 * - `source`: A `GPUTexelCopyTextureInfo` object that specifies the source texture and its layout.
	 * - `destination`: A `GPUTexelCopyTextureInfo` object that specifies the destination texture and its layout.
	 * - `copySize`: A `GPUExtent3D` object that specifies the size of the data to be copied.
	 * 
	 * **See Also:**
	 * - [GPUTexelCopyTextureInfo](https://www.w3.org/TR/webgpu/#gputexelcopytextureinfo)
	 * 
	 */
	fun copyTextureToTexture(source: GPUTexelCopyTextureInfo, destination: GPUTexelCopyTextureInfo, copySize: GPUExtent3D)
	/**
	 * Clears the contents of a buffer. This method sets the specified range of the buffer to zero.
	 * 
	 * **Parameters:**
	 * - `buffer`: The `GPUBuffer` to be cleared.
	 * - `offset`: An optional parameter specifying the starting offset within the buffer where the clear operation will begin. Defaults to 0 if not provided.
	 * - `size`: An optional parameter specifying the size of the range to be cleared. If not provided, the entire range from `offset` to the end of the buffer is cleared.
	 * 
	 * **See Also:**
	 * - [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer)
	 * 
	 */
	fun clearBuffer(buffer: GPUBuffer, offset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Resolves a set of queries and writes the results to a buffer. This method is used for performance monitoring and debugging.
	 * 
	 * **Parameters:**
	 * - `querySet`: The `GPUQuerySet` containing the queries to be resolved.
	 * - `firstQuery`: The index of the first query in the query set to resolve.
	 * - `queryCount`: The number of queries to resolve starting from `firstQuery`.
	 * - `destination`: The `GPUBuffer` where the results of the resolved queries will be written.
	 * - `destinationOffset`: The offset within the destination buffer where the results will be written.
	 * 
	 * **See Also:**
	 * - [GPUQuerySet](https://www.w3.org/TR/webgpu/#gpuqueryset)
	 * 
	 */
	fun resolveQuerySet(querySet: GPUQuerySet, firstQuery: GPUSize32, queryCount: GPUSize32, destination: GPUBuffer, destinationOffset: GPUSize64)
	/**
	 * Finishes the command encoding process and returns a `GPUCommandBuffer` that can be submitted to the GPU for execution.
	 * 
	 * **Parameters:**
	 * - `descriptor`: An optional `GPUCommandBufferDescriptor` object that specifies configuration options for the command buffer. If not provided, default values are used.
	 * 
	 * **Returns:**
	 * - A `GPUCommandBuffer` instance that contains all the recorded commands and can be submitted to the GPU.
	 * 
	 * **See Also:**
	 * - [GPUCommandBuffer](https://www.w3.org/TR/webgpu/#gpucommandbuffer)
	 * 
	 */
	fun finish(descriptor: GPUCommandBufferDescriptor? = null): GPUCommandBuffer
}

/**
 * The `GPUBindingCommandsMixin` interface extends the functionality of GPU command objects by providing methods to set bind groups. This mixin assumes the presence of `GPUObjectBase` and `GPUCommandsMixin` members on the same object.
 * 
 * It includes device timeline properties for managing bind groups and dynamic offsets, which are essential for configuring the rendering pipeline in WebGPU.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpubindingcommandsmixin).
 * 
 */
interface GPUBindingCommandsMixin {
	/**
	 * Sets a bind group for a specific index in the rendering pipeline.
	 * 
	 * @param index The index at which to set the bind group. This must be a valid `GPUIndex32` value.
	 * @param bindGroup The `GPUBindGroup` to set at the specified index. If `null`, the bind group at the specified index is unset.
	 * @param dynamicOffsetsData A list of unsigned integers representing dynamic offsets for the bind group. This parameter is optional and defaults to an empty list.
	 * 
	 * This method updates the internal state of the command encoder to include the specified bind group and dynamic offsets at the given index. The `bind_group` parameter allows for flexible binding configurations, enabling efficient resource management in the rendering pipeline.
	 * 
	 * For more information, see the [WebGPU specification on GPUBindingCommandsMixin](https://www.w3.org/TR/webgpu/#gpubindingcommandsmixin).
	 * 
	 */
	fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt> = emptyList())
	/**
	 * Sets immediate data for subsequent render or compute commands. Called on: GPUBindingCommandsMixin this. Arguments: Arguments for the GPUBindingCommandsMixin.setImmediates(rangeOffset, data, dataOffset, dataSize) method. Parameter Type Nullable Optional Description rangeOffset GPUSize32 ✘ ✘ Offset in bytes into the immediate data range to begin writing at. data AllowSharedBufferSource ✘ ✘ Data to write into the immediate data range. dataOffset GPUSize64 ✘ ✔ Offset into data to begin writing from. Given in elements if data is a TypedArray and bytes otherwise. dataSize GPUSize64 ✘ ✔ Size of content to write from data. Given in elements if data is a TypedArray and bytes otherwise. Returns: undefined Content timeline steps: If data is an ArrayBuffer or DataView, let the element type be "byte". Otherwise, data is a TypedArray; let the element type be the type of the TypedArray. Let dataElementCount be the size of data, in elements. If dataSize is missing, let contentsSize be dataElementCount − dataOffset. Otherwise, let contentsSize be dataSize. If any of the following conditions are unsatisfied, throw an OperationError and return. contentsSize ≥ 0. dataOffset + contentsSize ≤ dataElementCount. contentsSize, converted to bytes, is a multiple of 4. Let dataContents be a copy of the bytes held by the buffer source data. Let contents be the contentsSize elements of dataContents starting at an offset of dataOffset elements. Let contentsBytes be contentsSize converted to bytes. Issue the subsequent steps on the Device timeline of this.[[device]]. Device timeline steps: Validate the encoder state of this. If it returns false, return. If any of the following conditions are unsatisfied, invalidate this and return. rangeOffset is a multiple of 4. rangeOffset + contentsBytes ≤ this.[[device]].[[limits]].maxImmediateSize. For each byte b at index i in contents: Let byteIndex be rangeOffset + i. Let slotIndex be floor(byteIndex ÷ 4). Set this.[[immediate_data]][byteIndex] to b. Set this.[[immediate_slots_set]][slotIndex] to true.
	 * 
	 */
	fun setImmediates(rangeOffset: GPUSize32, data: ArrayBuffer, dataOffset: GPUSize64 = 0u, dataSize: GPUSize64? = null)
}

/**
 * The `GPUDebugCommandsMixin` interface provides methods to apply debug labels to groups of commands or insert a single label into the command sequence. This is useful for debugging and profiling purposes, allowing developers to create a hierarchy of labeled commands that can be visualized in browser developer tools.
 * 
 * Debug groups can be nested to create a hierarchy of labeled commands. These groups must be well-balanced, meaning every `pushDebugGroup` call must have a corresponding `popDebugGroup` call. Like [object labels](https://www.w3.org/TR/webgpu/#dom-gpuobjectbase-label), these labels have no required behavior but may be shown in error messages and browser developer tools, and may be passed to native API backends.
 * 
 * This interface assumes the presence of `GPUObjectBase` and `GPUCommandsMixin` members on the same object. It must only be included by interfaces which also include those mixins.
 * 
 */
interface GPUDebugCommandsMixin {
	/**
	 * Pushes a debug group onto the command buffer with the specified label.
	 * 
	 * @param groupLabel The label for the debug group. This is a string that will be used to identify the group in debugging tools. @throws IllegalArgumentException if `groupLabel` is null or empty.
	 * 
	 */
	fun pushDebugGroup(groupLabel: String)
	/**
	 * Pops the topmost debug group from the command buffer.
	 * 
	 * This method must be called after a corresponding `pushDebugGroup` call to maintain a well-balanced hierarchy of debug groups. @throws IllegalStateException if there is no debug group to pop (i.e., the stack is empty).
	 * 
	 */
	fun popDebugGroup()
	/**
	 * Inserts a debug marker into the command buffer with the specified label.
	 * 
	 * This method is useful for inserting single labels at specific points in the command sequence, which can be helpful for debugging and profiling. @param markerLabel The label for the debug marker. This is a string that will be used to identify the marker in debugging tools. @throws IllegalArgumentException if `markerLabel` is null or empty.
	 * 
	 */
	fun insertDebugMarker(markerLabel: String)
}

/**
 * The `GPUComputePassEncoder` interface represents a compute pass encoder, which is used to encode commands for a compute pass. This interface allows you to set the pipeline, dispatch workgroups, and end the compute pass.
 * 
 * A compute pass encoder is created by a [GPUCommandEncoder] and is used to record commands that will be executed on the GPU. The primary purpose of this interface is to manage the execution of compute shaders, which are used for general-purpose computations on the GPU.
 * 
 * **Inherited from:**
 * - [GPUObjectBase]
 * - [GPUCommandsMixin]
 * - [GPUDebugCommandsMixin]
 * - [GPUBindingCommandsMixin]
 * 
 * **Device Timeline Properties:**
 * - `[[command_encoder]]`: The GPUCommandEncoder that created this compute pass encoder.
 * - `[[endTimestampWrite]]`: A GPU command, if any, writing a timestamp when the pass ends. Defaults to null.
 * - `[[pipeline]]`: The current [GPUComputePipeline]. Initially null.
 * 
 */
interface GPUComputePassEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin {
	/**
	 * Sets the compute pipeline for this compute pass encoder.
	 * 
	 * **Parameters:**
	 * - `pipeline`: The [GPUComputePipeline] to set for this compute pass encoder.
	 * 
	 * **Returns:** Nothing
	 * 
	 */
	fun setPipeline(pipeline: GPUComputePipeline)
	/**
	 * Dispatches the specified number of workgroups to be executed by the compute pipeline.
	 * 
	 * **Parameters:**
	 * - `workgroupCountX`: The number of workgroups to dispatch in the X dimension. Must be greater than 0.
	 * - `workgroupCountY`: (Optional) The number of workgroups to dispatch in the Y dimension. Defaults to 1 if not specified.
	 * - `workgroupCountZ`: (Optional) The number of workgroups to dispatch in the Z dimension. Defaults to 1 if not specified.
	 * 
	 * **Returns:** Nothing
	 * 
	 */
	fun dispatchWorkgroups(workgroupCountX: GPUSize32, workgroupCountY: GPUSize32 = 1u, workgroupCountZ: GPUSize32 = 1u)
	/**
	 * Dispatches the specified number of workgroups to be executed by the compute pipeline using an indirect buffer.
	 * 
	 * **Parameters:**
	 * - `indirectBuffer`: The [GPUBuffer] containing the indirect parameters.
	 * - `indirectOffset`: The offset in bytes within the indirect buffer where the indirect parameters start.
	 * 
	 * **Returns:** Nothing
	 * 
	 */
	fun dispatchWorkgroupsIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
	/**
	 * Ends the compute pass encoder.
	 * 
	 * **Returns:** Nothing
	 * 
	 * This method finalizes the commands recorded by the `GPUComputePassEncoder`. It must be called to complete the encoding of a compute pass. After calling this method, no further commands can be added to the encoder.
	 * 
	 */
	fun end()
}

/**
 * The `GPURenderPassEncoder` interface represents a render pass encoder, which is used to encode commands into a render pass. This interface allows for the configuration of various rendering states and the execution of draw calls within a render pass.
 * 
 * A render pass encoder is created by a [GPUCommandEncoder] and is used to record commands that will be executed on the GPU. The `GPURenderPassEncoder` interface includes methods for setting viewport, scissor rectangle, blend constant, stencil reference, beginning and ending occlusion queries, executing render bundles, and ending the render pass.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpurenderpassencoder).
 * 
 */
interface GPURenderPassEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin, GPURenderCommandsMixin {
	/**
	 * Sets the viewport for the render pass. The viewport defines a clipping rectangle in normalized device coordinates (NDC) that specifies the region of the render target to which rendering commands are directed.
	 * 
	 * **Parameters:**
	 * - `x`: The x-coordinate of the viewport's origin.
	 * - `y`: The y-coordinate of the viewport's origin.
	 * - `width`: The width of the viewport.
	 * - `height`: The height of the viewport.
	 * - `minDepth`: The minimum depth value for the viewport.
	 * - `maxDepth`: The maximum depth value for the viewport.
	 * 
	 */
	fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float)
	/**
	 * Sets the scissor rectangle for the render pass. The scissor rectangle defines a clipping region in pixel coordinates that restricts rendering to a specific area of the render target.
	 * 
	 * **Parameters:**
	 * - `x`: The x-coordinate of the scissor rectangle's origin.
	 * - `y`: The y-coordinate of the scissor rectangle's origin.
	 * - `width`: The width of the scissor rectangle.
	 * - `height`: The height of the scissor rectangle.
	 * 
	 */
	fun setScissorRect(x: GPUIntegerCoordinate, y: GPUIntegerCoordinate, width: GPUIntegerCoordinate, height: GPUIntegerCoordinate)
	/**
	 * Sets the blend constant color for the render pass. The blend constant color is used in blending operations to provide a constant color value that can be blended with the source and destination colors.
	 * 
	 * **Parameters:**
	 * - `color`: The blend constant color, represented as a [GPUColor](https://www.w3.org/TR/webgpu/#typedefdef-gpucolor).
	 * 
	 */
	fun setBlendConstant(color: GPUColor)
	/**
	 * Sets the stencil reference value for the render pass. The stencil reference value is used in stencil testing to compare against the stencil buffer values.
	 * 
	 * **Parameters:**
	 * - `reference`: The stencil reference value, represented as a [GPUStencilValue](https://www.w3.org/TR/webgpu/#typedefdef-gpustencilvalue).
	 * 
	 */
	fun setStencilReference(reference: GPUStencilValue)
	/**
	 * Begins an occlusion query at the specified index. Occlusion queries are used to determine whether a specific region of the render target is visible or occluded by other geometry.
	 * 
	 * **Parameters:**
	 * - `queryIndex`: The index of the occlusion query, represented as a [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32).
	 * 
	 */
	fun beginOcclusionQuery(queryIndex: GPUSize32)
	/**
	 * Ends the current occlusion query. This method should be called after the commands that are to be tested for occlusion have been recorded.
	 * 
	 */
	fun endOcclusionQuery()
	/**
	 * Executes a list of render bundles within the render pass. Render bundles are pre-recorded sequences of commands that can be executed multiple times with different parameters.
	 * 
	 * **Parameters:**
	 * - `bundles`: A list of [GPURenderBundle](https://www.w3.org/TR/webgpu/#gpurenderbundle) objects to execute.
	 * 
	 */
	fun executeBundles(bundles: List<GPURenderBundle>)
	/**
	 * Ends the render pass. This method should be called after all rendering commands have been recorded to finalize the render pass.
	 * 
	 */
	fun end()
}

/**
 * The `GPURenderCommandsMixin` interface defines rendering commands that are common to both [GPURenderPassEncoder](https://www.w3.org/TR/webgpu/#gpurenderpassencoder) and [GPURenderBundleEncoder](https://www.w3.org/TR/webgpu/#gpurenderbundleencoder). This mixin is used to encapsulate the rendering commands that can be executed within a render pass or bundle.
 * 
 * The `GPURenderCommandsMixin` assumes the presence of members from [GPUObjectBase](https://www.w3.org/TR/webgpu/#gpuobjectbase), [GPUCommandsMixin](https://www.w3.org/TR/webgpu/#gpucommandsmixin), and [GPUBindingCommandsMixin](https://www.w3.org/TR/webgpu/#gpubindingcommandsmixin) on the same object. It must only be included by interfaces that also include those mixins.
 * 
 */
interface GPURenderCommandsMixin {
	/**
	 * Sets the rendering pipeline to be used for subsequent drawing commands.
	 * 
	 * **Parameters:**
	 * - `pipeline`: The [GPURenderPipeline](https://www.w3.org/TR/webgpu/#gpurenderpipeline) to set as the current pipeline.
	 * 
	 */
	fun setPipeline(pipeline: GPURenderPipeline)
	/**
	 * Sets the index buffer to be used for indexed drawing commands.
	 * 
	 * **Parameters:**
	 * - `buffer`: The [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) containing the index data.
	 * - `indexFormat`: The format of the indices in the buffer, specified as a [GPUIndexFormat](https://www.w3.org/TR/webgpu/#enumdef-gpuindexformat).
	 * - `offset`: An optional offset within the buffer where the index data starts. Defaults to 0.
	 * - `size`: An optional size of the index data in bytes. If not specified, the entire buffer is used.
	 * 
	 */
	fun setIndexBuffer(buffer: GPUBuffer, indexFormat: GPUIndexFormat, offset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Sets the vertex buffer at a specific slot to be used for subsequent drawing commands.
	 * 
	 * **Parameters:**
	 * - `slot`: The index of the vertex buffer slot.
	 * - `buffer`: The [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) containing the vertex data. Can be null to unset the buffer.
	 * - `offset`: An optional offset within the buffer where the vertex data starts. Defaults to 0.
	 * - `size`: An optional size of the vertex data in bytes. If not specified, the entire buffer is used.
	 * 
	 */
	fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Issues a draw command to render vertices.
	 * 
	 * **Parameters:**
	 * - `vertexCount`: The number of vertices to draw.
	 * - `instanceCount`: An optional number of instances to draw. Defaults to 1.
	 * - `firstVertex`: An optional offset into the vertex buffer. Defaults to 0.
	 * - `firstInstance`: An optional offset into the instance data. Defaults to 0.
	 * 
	 */
	fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32 = 1u, firstVertex: GPUSize32 = 0u, firstInstance: GPUSize32 = 0u)
	/**
	 * Issues an indexed draw command to render vertices using indices.
	 * 
	 * **Parameters:**
	 * - `indexCount`: The number of indices to draw.
	 * - `instanceCount`: An optional number of instances to draw. Defaults to 1.
	 * - `firstIndex`: An optional offset into the index buffer. Defaults to 0.
	 * - `baseVertex`: An optional base vertex offset. Defaults to 0.
	 * - `firstInstance`: An optional offset into the instance data. Defaults to 0.
	 * 
	 */
	fun drawIndexed(indexCount: GPUSize32, instanceCount: GPUSize32 = 1u, firstIndex: GPUSize32 = 0u, baseVertex: GPUSignedOffset32 = 0, firstInstance: GPUSize32 = 0u)
	/**
	 * Issues an indirect draw command to render vertices using data from a buffer.
	 * 
	 * **Parameters:**
	 * - `indirectBuffer`: The [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) containing the indirect draw parameters.
	 * - `indirectOffset`: The offset within the buffer where the indirect draw parameters start.
	 * 
	 */
	fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
	/**
	 * Issues an indirect indexed draw command to render vertices using data from a buffer.
	 * 
	 * **Parameters:**
	 * - `indirectBuffer`: The [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) containing the indirect indexed draw parameters.
	 * - `indirectOffset`: The offset within the buffer where the indirect indexed draw parameters start.
	 * 
	 */
	fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64)
}

/**
 * /**
 *  * Represents a render bundle in the WebGPU API. A `GPURenderBundle` encapsulates a list of GPU commands that can be executed by a [GPURenderPassEncoder](https://www.w3.org/TR/webgpu/#dom-gpurenderpassencoder).
 *  * 
 *  * This interface is part of the WebGPU API, which provides a low-level, cross-platform graphics API for the web. For more details, refer to the [official W3C specification](https://www.w3.org/TR/webgpu/#gpurenderbundle).
 *  * 
 *  * @see [GPUObjectBase](https://www.w3.org/TR/webgpu/#dom-gpuobjectbase)
 *  */
 * interface GPURenderBundle : GPUObjectBase
 * 
 */
interface GPURenderBundle : GPUObjectBase
/**
 * The `GPURenderBundleEncoder` interface represents an encoder for creating render bundles in WebGPU. A render bundle is a collection of rendering commands that can be executed multiple times with different parameters, improving performance by reducing the overhead of command encoding.
 * 
 * This interface inherits from several mixins and interfaces:
 * - [GPUObjectBase]: Provides basic object properties such as `label`.
 * - [GPUCommandsMixin]: Mixin for common GPU commands.
 * - [GPUDebugCommandsMixin]: Mixin for debug-related commands.
 * - [GPUBindingCommandsMixin]: Mixin for binding-related commands.
 * - [GPURenderCommandsMixin]: Mixin for render-related commands.
 * - [AutoCloseable]: Ensures that resources are closed properly.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/).
 * 
 */
interface GPURenderBundleEncoder : GPUObjectBase, GPUCommandsMixin, GPUDebugCommandsMixin, GPUBindingCommandsMixin, GPURenderCommandsMixin, AutoCloseable {
	/**
	 * Encodes the render commands into a `GPURenderBundle` and finalizes the encoder.
	 * 
	 * **Parameters:**
	 * - `descriptor`: An optional `GPURenderBundleDescriptor` that specifies additional parameters for creating the render bundle. If not provided, default values are used.
	 * 
	 * **Returns:**
	 * - A `GPURenderBundle` object containing the encoded render commands.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: finish](https://www.w3.org/TR/webgpu/#dom-gpurenderbundleencoder-finish)
	 * 
	 */
	fun finish(descriptor: GPURenderBundleDescriptor? = null): GPURenderBundle
}

/**
 * The `GPUQueue` interface represents a queue that allows for the submission of command buffers and other operations related to GPU execution. It is part of the WebGPU API, providing a way to manage and execute commands on the GPU.
 * 
 * This interface inherits from [GPUObjectBase](https://www.w3.org/TR/webgpu/#gpuobjectbase), which provides basic object management functionality.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpuqueue).
 * 
 */
interface GPUQueue : GPUObjectBase {
	/**
	 * Submits a list of command buffers to the GPU queue for execution. This method allows you to enqueue commands that will be processed by the GPU in the order they are submitted.
	 * 
	 * @param commandBuffers A list of [GPUCommandBuffer](https://www.w3.org/TR/webgpu/#gpucommandbuffer) objects to be executed.
	 * 
	 */
	fun submit(commandBuffers: List<GPUCommandBuffer>)
	/**
	 * Returns a promise that resolves when all previously submitted work is complete. This method can be used to synchronize GPU operations and ensure that certain tasks have finished executing before proceeding with other operations.
	 * 
	 * @return A [Result](https://kotlinlang.org/api/latest/kotlinx-coroutines/core/kotlinx.coroutines/-result/) object that completes when the submitted work is done.
	 * 
	 */
	suspend fun onSubmittedWorkDone(): Result<Unit>
	/**
	 * Writes data from a buffer source to a GPU buffer. This method allows you to transfer data from a host buffer (e.g., an ArrayBuffer) to a GPU buffer for use in GPU operations.
	 * 
	 * @param buffer The [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) object to which the data will be written.
	 * @param bufferOffset The offset within the GPU buffer where the data will be written.
	 * @param data The source data to be written to the GPU buffer. This can be an [ArrayBuffer](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/ArrayBuffer).
	 * @param dataOffset The offset within the source data from which to start reading. Defaults to 0.
	 * @param size The number of bytes to write. If null, the entire buffer is written.
	 * 
	 */
	fun writeBuffer(buffer: GPUBuffer, bufferOffset: GPUSize64, data: ArrayBuffer, dataOffset: GPUSize64 = 0u, size: GPUSize64? = null)
	/**
	 * Writes texture data from a buffer source to a GPU texture. This method allows you to transfer texture data from a host buffer to a GPU texture for use in rendering operations.
	 * 
	 * @param destination The [GPUTexelCopyTextureInfo](https://www.w3.org/TR/webgpu/#gputexelcopytextureinfo) object specifying the destination texture and its properties.
	 * @param data The source data to be written to the GPU texture. This can be an [ArrayBuffer](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/ArrayBuffer).
	 * @param dataLayout The [GPUTexelCopyBufferLayout](https://www.w3.org/TR/webgpu/#gputexelcopybufferlayout) object specifying the layout of the source data.
	 * @param size The extent of the texture data to be written.
	 * 
	 */
	fun writeTexture(destination: GPUTexelCopyTextureInfo, data: ArrayBuffer, dataLayout: GPUTexelCopyBufferLayout, size: GPUExtent3D)
}

/**
 * Represents a set of queries in the WebGPU API. A `GPUQuerySet` is used to manage and retrieve information about GPU operations such as occlusion queries, pipeline statistics queries, etc.
 * 
 * This interface inherits from [GPUObjectBase](https://www.w3.org/TR/webgpu/#gpuobjectbase) and implements [AutoCloseable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-auto-closeable/) to ensure proper resource management. The `destroy` method must be called to release the resources associated with this query set.
 * 
 * For more details, refer to the [WebGPU specification on GPUQuerySet](https://www.w3.org/TR/webgpu/#gpuqueryset).
 * 
 */
interface GPUQuerySet : GPUObjectBase, AutoCloseable {
	/**
	 * The type of the queries managed by this `GPUQuerySet`. This property is read-only and specifies the kind of queries that can be performed using this query set.
	 * 
	 * **Type:** [GPUQueryType](https://www.w3.org/TR/webgpu/#enumdef-gpuquerytype)
	 * 
	 */
	val type: GPUQueryType
	/**
	 * The number of queries managed by this `GPUQuerySet`. This property is read-only and indicates the total count of queries that can be performed using this query set.
	 * 
	 * **Type:** [GPUSize32Out](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32out)
	 * 
	 */
	val count: GPUSize32Out
}

/**
 * Represents information about why a [GPUDevice](https://www.w3.org/TR/webgpu/#gpudevice) was lost. This interface provides details that can help developers understand the cause of device loss and take appropriate actions.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUDeviceLostInfo](https://www.w3.org/TR/webgpu/#gpudevicelostinfo)
 * 
 */
interface GPUDeviceLostInfo {
	/**
	 * The reason why the GPU device was lost. This is an instance of [GPUDeviceLostReason](https://www.w3.org/TR/webgpu/#enumdef-gpudevicelostreason), which enumerates possible causes for device loss.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUDeviceLostInfo.reason](https://www.w3.org/TR/webgpu/#dom-gpudevicelostinfo-reason)
	 * 
	 */
	val reason: GPUDeviceLostReason
	/**
	 * A message providing additional information about why the GPU device was lost. This string is implementation-defined and should not be parsed by applications.
	 * 
	 * **Important:**
	 * The message may contain sensitive information and should be handled with care. It is intended for debugging purposes and should not be displayed to end-users without proper sanitization.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUDeviceLostInfo.message](https://www.w3.org/TR/webgpu/#dom-gpudevicelostinfo-message)
	 * 
	 */
	val message: String
}

/**
 * The `GPUError` interface represents the base class for all errors that can be surfaced from WebGPU operations. This includes errors returned by [popErrorScope()] and those triggered by the `uncapturederror` event.
 * 
 * **Context:** Errors are generated under specific conditions defined in the respective algorithms of WebGPU operations. No errors are generated from a lost device. For more details, refer to the [Errors & Debugging section] of the WebGPU specification.
 * 
 * **Note:** Future versions of this specification may introduce new subtypes of `GPUError`. Applications should handle this possibility by using the error's `message` property when possible and specializing using `instanceof`. Use `error.constructor.name` for serialization purposes, such as generating debug reports.
 * 
 */
sealed interface GPUError {
	/**
	 * A read-only string that provides a human-readable message describing the error.
	 * 
	 * **Type:** [String]
	 * 
	 * **Behavior:** This property contains a descriptive message that can be used to understand the nature of the error. It is particularly useful for debugging and logging purposes.
	 * 
	 */
	val message: String
}

/**
 * A subtype of [GPUError] that indicates an operation did not satisfy all validation requirements. Validation errors are always indicative of an application error and are expected to fail the same way across all devices, assuming the same [[features]] and [[limits]] are in use.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#gpuvalidationerror).
 * 
 * In this example, if an operation on the `GPUDevice` fails due to a validation error, it will be caught by the `catch` block, and the error message will be printed.
 * 
 */
interface GPUValidationError : GPUError
/**
 * Represents a subtype of GPUError that indicates an out-of-memory condition. This error occurs when there is insufficient free memory to complete the requested operation.
 * 
 * The operation may succeed if attempted again with a lower memory requirement (e.g., using smaller texture dimensions), or if memory used by other resources is released first.
 * 
 * **See Also:**
 * - [GPUError](https://www.w3.org/TR/webgpu/#gpuerror)
 * 
 */
interface GPUOutOfMemoryError : GPUError
/**
 * A subtype of GPUError that indicates an operation failed for a system or implementation-specific reason, even when all validation requirements have been satisfied. This error may occur if the operation exceeds the capabilities of the implementation in ways not easily captured by the supported limits.
 * 
 * For example, the same operation might succeed on other devices or under different circumstances.
 * 
 * **See also:**
 * - GPUError
 * - [Supported Limits](https://www.w3.org/TR/webgpu/#supported-limits)
 * 
 * **Related Operations:**
 * - [Generate an Internal Error](https://www.w3.org/TR/webgpu/#generate-an-internal-error)
 * 
 */
interface GPUInternalError : GPUError
/**
 * Represents the base descriptor for GPU objects. This interface is used to provide a common structure for labeling GPU objects, which can be helpful for debugging and identification purposes.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase).
 * 
 */
interface GPUObjectDescriptorBase {
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
	val label: String
}

/**
 * The `GPURequestAdapterOptions` interface provides hints to the user agent indicating what configuration is suitable for the application. This interface allows developers to specify preferences and constraints for the GPU adapter selection process.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpurequestadapteroptions).
 * 
 */
interface GPURequestAdapterOptions {
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
	val featureLevel: String
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
	val powerPreference: GPUPowerPreference?
	/**
	 * The `forceFallbackAdapter` property indicates whether only a fallback adapter may be returned. If set to `true`, the user agent will return a fallback adapter if available, or `null` if not supported.
	 * 
	 * **Default Value:** false
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurequestadapteroptions-forcefallbackadapter).
	 * 
	 */
	val forceFallbackAdapter: Boolean
	/**
	 * The `xrCompatible` property indicates whether the best adapter for rendering to a WebXR session must be returned. If set to `true`, the user agent will prioritize adapters suitable for WebXR rendering.
	 * 
	 * **Default Value:** false
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurequestadapteroptions-xrcompatible).
	 * 
	 */
	val xrCompatible: Boolean
}

/**
 * The `GPUDeviceDescriptor` interface describes a device request. It specifies the features, limits, and default queue descriptor required by the GPU device.
 * 
 * This interface is used to configure the creation of a [GPUDevice] object, which represents a GPU adapter and provides methods for creating GPU resources such as buffers, textures, and pipelines.
 * 
 * For more details, refer to the [WebGPU specification on `GPUDeviceDescriptor`](https://www.w3.org/TR/webgpu/#gpudevicedescriptor).
 * 
 */
interface GPUDeviceDescriptor : GPUObjectDescriptorBase {
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
	val requiredFeatures: List<GPUFeatureName>
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
	val requiredLimits: GPUSupportedLimits?
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
	val defaultQueue: GPUQueueDescriptor
	/**
	 * Describes the `onUncapturedError` member of `GPUDeviceDescriptor`.
	 * 
	 */
	val onUncapturedError: GPUUncapturedErrorCallback?
}

/**
 * Represents a descriptor for creating GPU buffers. This interface extends [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#gpuobjectdescriptorbase) and is used to specify the properties of a buffer, such as its size, usage flags, and whether it should be mapped at creation.
 * 
 * For more details, refer to the [WebGPU specification on GPUBufferDescriptor](https://www.w3.org/TR/webgpu/#gpubufferdescriptor).
 * 
 */
interface GPUBufferDescriptor : GPUObjectDescriptorBase {
	/**
	 * The size of the buffer in bytes. This value must be a multiple of 4 and greater than or equal to 4.
	 * 
	 * **Type:** [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	val size: GPUSize64
	/**
	 * Specifies the allowed usages for the buffer. This is a bitmask of [GPUBufferUsageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gpubufferusageflags) that indicates how the buffer will be used.
	 * 
	 * **Type:** [GPUBufferUsageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gpubufferusageflags)
	 * 
	 */
	val usage: GPUBufferUsage
	/**
	 * Indicates whether the buffer should be created in an already mapped state. If `true`, the buffer can be immediately accessed using [getMappedRange()](https://www.w3.org/TR/webgpu/#dom-gpubuffer-getmappedrange). This is useful for setting the buffer's initial data.
	 * 
	 * **Type:** Boolean
	 * 
	 * **Default Value:** `false`
	 * 
	 */
	val mappedAtCreation: Boolean
}

/**
 * Represents a descriptor for creating GPU textures. This interface extends [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#gpuobjectdescriptorbase) and defines the properties required to specify the characteristics of a texture.
 * 
 * For more details, refer to the [WebGPU specification on GPUTextureDescriptor](https://www.w3.org/TR/webgpu/#gputexturedescriptor).
 * 
 */
interface GPUTextureDescriptor : GPUObjectDescriptorBase {
	/**
	 * Specifies the size of the texture in 3D space. This is a required property.
	 * 
	 * **Type:** [GPUExtent3D]
	 * 
	 */
	val size: GPUExtent3D
	/**
	 * Specifies the number of mipmap levels in the texture. The default value is 1.
	 * 
	 * **Type:** [GPUIntegerCoordinate]
	 * 
	 */
	val mipLevelCount: GPUIntegerCoordinate
	/**
	 * Specifies the number of samples for multisampling. The default value is 1.
	 * 
	 * **Type:** [GPUSize32]
	 * 
	 */
	val sampleCount: GPUSize32
	/**
	 * Specifies the dimension of the texture. The default value is "2d".
	 * 
	 * **Type:** [GPUTextureDimension]
	 * 
	 */
	val dimension: GPUTextureDimension
	/**
	 * Specifies the format of the texture data. This is a required property.
	 * 
	 * **Type:** [GPUTextureFormat]
	 * 
	 */
	val format: GPUTextureFormat
	/**
	 * Specifies the usage flags for the texture. This is a required property.
	 * 
	 * **Type:** [GPUTextureUsageFlags]
	 * 
	 */
	val usage: GPUTextureUsage
	/**
	 * Specifies a list of formats that can be used to create views of the texture. The default value is an empty list.
	 * 
	 * **Type:** List<[GPUTextureFormat]>
	 * 
	 */
	val viewFormats: List<GPUTextureFormat>
	/**
	 * On devices without "core-features-and-limits", views created from this texture must have this as their dimension. If not specified, a default is chosen. On devices with "core-features-and-limits", this is ignored, and there is no such restriction.
	 * 
	 */
	val textureBindingViewDimension: GPUTextureViewDimension?
}

/**
 * The `GPUTextureViewDescriptor` interface defines a set of properties that describe how to create a view on a texture. This descriptor is used when creating a [GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview) object, which represents a specific way to access the data in a texture.
 * 
 * A texture view allows for different formats, dimensions, and usages of the underlying texture data. This is particularly useful for scenarios where you need to access the same texture data in multiple ways without duplicating the actual texture data.
 * 
 */
interface GPUTextureViewDescriptor : GPUObjectDescriptorBase {
	/**
	 * `format` specifies the format of the texture view. This must be either the `format` of the texture or one of the `viewFormats` specified during its creation.
	 * 
	 * See also: [WebGPU Specification - GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat).
	 * 
	 */
	val format: GPUTextureFormat?
	/**
	 * `dimension` specifies the dimension to view the texture as. This property determines how the texture will be interpreted in terms of its dimensionality (e.g., 1D, 2D, or 3D).
	 * 
	 * See also: [WebGPU Specification - GPUTextureViewDimension](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	val dimension: GPUTextureViewDimension?
	/**
	 * `usage` specifies the allowed usages for the texture view. This must be a subset of the `usage` flags of the texture. If set to 0, it defaults to the full set of usage flags of the texture.
	 * 
	 * See also: [WebGPU Specification - GPUTextureUsageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gputextureusageflags).
	 * 
	 */
	val usage: GPUTextureUsage
	/**
	 * `aspect` specifies which aspects of the texture are accessible to the texture view. This property determines whether the view can access color, depth, stencil, or all aspects of the texture.
	 * 
	 * See also: [WebGPU Specification - GPUTextureAspect](https://www.w3.org/TR/webgpu/#enumdef-gputextureaspect).
	 * 
	 */
	val aspect: GPUTextureAspect
	/**
	 * `baseMipLevel` specifies the first (most detailed) mipmap level accessible to the texture view. This property defines the starting point for mipmap levels that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	val baseMipLevel: GPUIntegerCoordinate
	/**
	 * `mipLevelCount` specifies how many mipmap levels, starting with `baseMipLevel`, are accessible to the texture view. This property defines the range of mipmap levels that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	val mipLevelCount: GPUIntegerCoordinate?
	/**
	 * `baseArrayLayer` specifies the index of the first array layer accessible to the texture view. This property defines the starting point for array layers that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	val baseArrayLayer: GPUIntegerCoordinate
	/**
	 * `arrayLayerCount` specifies how many array layers, starting with `baseArrayLayer`, are accessible to the texture view. This property defines the range of array layers that can be accessed by the view.
	 * 
	 * See also: [WebGPU Specification - GPUIntegerCoordinate](https://www.w3.org/TR/webgpu/#typedefdef-gpuintegercoordinate).
	 * 
	 */
	val arrayLayerCount: GPUIntegerCoordinate?
	/**
	 * `swizzle` maps the red, green, blue, and alpha output channels, in that order. Each position accepts one of `r`, `g`, `b`, `a`, `0`, or `1`; selections may be repeated. Its default, `rgba`, preserves the original channels. A non-identity swizzle requires the `texture-component-swizzle` feature to be enabled on the device before the view is created.
	 * 
	 * Non-identity swizzles affect shader reads. WebGPU requires the identity `rgba` swizzle when a view is used as a storage texture or as a render attachment.
	 * 
	 * In Kotlin, configure the channels with `GPUTextureSwizzle`. For example, the WebGPU value `b01r` is `GPUTextureSwizzle(red = GPUTextureSwizzleSource.Blue, green = GPUTextureSwizzleSource.Zero, blue = GPUTextureSwizzleSource.One, alpha = GPUTextureSwizzleSource.Red)`. Use `toWebGpuString()` when converting to the Web binding's `DOMString`.
	 * 
	 * See also: [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gputextureviewdescriptor).
	 * 
	 */
	val swizzle: GPUTextureSwizzle
}

/**
 * The `GPUSamplerDescriptor` interface defines the properties of a sampler used in WebGPU. This descriptor specifies how textures are sampled during rendering, including addressing modes, filtering modes, and level-of-detail (LOD) clamping. It is part of the GPUObjectDescriptorBase hierarchy and is used to create `GPUSampler` objects.
 * 
 * For more details, refer to the [WebGPU specification on GPUSamplerDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpusamplerdescriptor).
 * 
 */
interface GPUSamplerDescriptor : GPUObjectDescriptorBase {
	/**
	 * Specifies the addressing mode for the U coordinate of the texture. This determines how texture coordinates are handled when they extend beyond the bounds of the texture. The default value is `GPUAddressMode.CLAMP_TO_EDGE`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUAddressMode](https://www.w3.org/TR/webgpu/#enumdef-gpuaddressmode).
	 * 
	 */
	val addressModeU: GPUAddressMode
	/**
	 * Specifies the addressing mode for the V coordinate of the texture. This determines how texture coordinates are handled when they extend beyond the bounds of the texture. The default value is `GPUAddressMode.CLAMP_TO_EDGE`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUAddressMode](https://www.w3.org/TR/webgpu/#enumdef-gpuaddressmode).
	 * 
	 */
	val addressModeV: GPUAddressMode
	/**
	 * Specifies the addressing mode for the W coordinate of the texture. This determines how texture coordinates are handled when they extend beyond the bounds of the texture. The default value is `GPUAddressMode.CLAMP_TO_EDGE`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUAddressMode](https://www.w3.org/TR/webgpu/#enumdef-gpuaddressmode).
	 * 
	 */
	val addressModeW: GPUAddressMode
	/**
	 * Specifies the filtering mode used when the sampled area is smaller than or equal to one texel. The default value is `GPUFilterMode.NEAREST`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUFilterMode](https://www.w3.org/TR/webgpu/#enumdef-gpufiltermode).
	 * 
	 */
	val magFilter: GPUFilterMode
	/**
	 * Specifies the filtering mode used when the sampled area is larger than one texel. The default value is `GPUFilterMode.NEAREST`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUFilterMode](https://www.w3.org/TR/webgpu/#enumdef-gpufiltermode).
	 * 
	 */
	val minFilter: GPUFilterMode
	/**
	 * Specifies the filtering mode used when sampling between mipmap levels. The default value is `GPUMipmapFilterMode.NEAREST`.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUMipmapFilterMode](https://www.w3.org/TR/webgpu/#enumdef-gpumipmapfiltermode).
	 * 
	 */
	val mipmapFilter: GPUMipmapFilterMode
	/**
	 * Specifies the minimum level of detail (LOD) used internally when sampling a texture. The default value is `0.0`.
	 * 
	 * For more details, refer to the [WebGPU specification on levels of detail](https://www.w3.org/TR/webgpu/#levels-of-detail).
	 * 
	 */
	val lodMinClamp: Float
	/**
	 * Specifies the maximum level of detail (LOD) used internally when sampling a texture. The default value is `32.0`.
	 * 
	 * For more details, refer to the [WebGPU specification on levels of detail](https://www.w3.org/TR/webgpu/#levels-of-detail).
	 * 
	 */
	val lodMaxClamp: Float
	/**
	 * Specifies the comparison function used by a comparison sampler. When provided, the sampler will be a comparison sampler with the specified `GPUCompareFunction`. Comparison samplers may use filtering, but the sampling results will be implementation-dependent and may differ from the normal filtering rules.
	 * 
	 * For more details, refer to the [WebGPU specification on GPUCompareFunction](https://www.w3.org/TR/webgpu/#enumdef-gpucomparefunction).
	 * 
	 */
	val compare: GPUCompareFunction?
	/**
	 * Specifies the maximum anisotropy value clamp used by the sampler. Anisotropic filtering is enabled when `maxAnisotropy` is greater than 1 and the implementation supports it. The default value is `1`.
	 * 
	 * For more details, refer to the [WebGPU specification on anisotropic filtering](https://www.w3.org/TR/webgpu/#dom-gpusamplerdescriptor-maxanisotropy).
	 * 
	 */
	val maxAnisotropy: UShort
}

/**
 * Represents a descriptor for creating a GPUBindGroupLayout. This interface extends GPUObjectDescriptorBase and is used to define the layout of bind groups in WebGPU.
 * 
 * A `GPUBindGroupLayoutDescriptor` specifies a list of entries that describe shader resource bindings. Each entry defines how resources are bound to shaders, including buffers, samplers, textures, and external textures.
 * 
 */
interface GPUBindGroupLayoutDescriptor : GPUObjectDescriptorBase {
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
	val entries: List<GPUBindGroupLayoutEntry>
}

/**
 * Represents a binding layout entry for a GPU bind group. This interface defines the structure of individual bindings within a [GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#gpubindgrouplayout).
 * 
 * A `GPUBindGroupLayoutEntry` specifies how resources are bound to shader stages, including buffers, samplers, textures, and storage textures. Only one type of binding (buffer, sampler, texture, storageTexture) can be defined for any given entry.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUBindGroupLayoutEntry](https://www.w3.org/TR/webgpu/#dictdef-gpubindgrouplayoutentry)
 * 
 */
interface GPUBindGroupLayoutEntry {
	/**
	 * A unique identifier for a resource binding within the [GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#gpubindgrouplayout). This ID corresponds to a `GPUBindGroupEntry.binding` and a `@binding` attribute in the [GPUShaderModule](https://www.w3.org/TR/webgpu/#gpushadermodule).
	 * 
	 * **Type:** [GPUIndex32](https://www.w3.org/TR/webgpu/#typedefdef-gpuindex32)
	 * 
	 */
	val binding: GPUIndex32
	/**
	 * A bitset of the members of [GPUShaderStage](https://www.w3.org/TR/webgpu/#namespacedef-gpushaderstage). Each set bit indicates that a `GPUBindGroupLayoutEntry`'s resource will be accessible from the associated shader stage.
	 * 
	 * **Type:** [GPUShaderStageFlags](https://www.w3.org/TR/webgpu/#typedefdef-gpushaderstageflags)
	 * 
	 */
	val visibility: GPUShaderStage
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUBufferBinding](https://www.w3.org/TR/webgpu/#dictdef-gpubufferbinding).
	 * 
	 * **Type:** [GPUBufferBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gpubufferbindinglayout)
	 * 
	 */
	val buffer: GPUBufferBindingLayout?
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUSampler](https://www.w3.org/TR/webgpu/#gpusampler).
	 * 
	 * **Type:** [GPUSamplerBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gpusamplerbindinglayout)
	 * 
	 */
	val sampler: GPUSamplerBindingLayout?
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview).
	 * 
	 * **Type:** [GPUTextureBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gputexturebindinglayout)
	 * 
	 */
	val texture: GPUTextureBindingLayout?
	/**
	 * When provided, indicates that the binding resource type for this `GPUBindGroupLayoutEntry` is [GPUTextureView](https://www.w3.org/TR/webgpu/#gputextureview).
	 * 
	 * **Type:** [GPUStorageTextureBindingLayout?](https://www.w3.org/TR/webgpu/#dictdef-gpustoragetexturebindinglayout)
	 * 
	 */
	val storageTexture: GPUStorageTextureBindingLayout?
}

/**
 * Represents a layout for buffer bindings in WebGPU. This interface defines the properties required to specify how buffers should be bound to binding points in shaders.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpubufferbindinglayout-dictionary).
 * 
 */
interface GPUBufferBindingLayout {
	/**
	 * Specifies the type required for buffers bound to this binding point. This property determines how the buffer will be used in the shader.
	 * 
	 * **Type:** [GPUBufferBindingType](https://www.w3.org/TR/webgpu/#enumdef-gpubufferbindingtype)
	 * 
	 * **Default Value:** "uniform"
	 * 
	 */
	val type: GPUBufferBindingType
	/**
	 * Indicates whether this binding requires a dynamic offset. A dynamic offset allows for more flexible buffer binding, enabling the use of different buffer sizes at runtime.
	 * 
	 * **Type:** Boolean
	 * 
	 * **Default Value:** false
	 * 
	 */
	val hasDynamicOffset: Boolean
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
	val minBindingSize: GPUSize64
}

/**
 * Represents a binding layout for samplers in WebGPU. This interface defines the type of sampler that can be bound to a specific binding.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpusamplerbindinglayout).
 * 
 */
interface GPUSamplerBindingLayout {
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
	val type: GPUSamplerBindingType
}

/**
 * Represents the layout for a GPU texture binding. This interface defines the required properties for specifying how textures should be bound in a GPU pipeline.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gputexturebindinglayout).
 * 
 */
interface GPUTextureBindingLayout {
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
	val sampleType: GPUTextureSampleType
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
	val viewDimension: GPUTextureViewDimension
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
	val multisampled: Boolean
}

/**
 * Represents the layout configuration for a storage texture binding in WebGPU. This interface defines how textures are accessed and used within shaders, specifying the access mode, format, and view dimension.
 * 
 * For more details, refer to the [WebGPU specification on GPUStorageTextureBindingLayout](https://www.w3.org/TR/webgpu/#dictdef-gpustoragetexturebindinglayout).
 * 
 */
interface GPUStorageTextureBindingLayout {
	/**
	 * Specifies the access mode for this binding, indicating whether the texture is readable, writable, or both. This property defaults to `GPUStorageTextureAccess.WriteOnly`.
	 * 
	 * **Type:** [GPUStorageTextureAccess](https://www.w3.org/TR/webgpu/#enumdef-gpustoragetextureaccess)
	 * 
	 * **Default Value:** `GPUStorageTextureAccess.WriteOnly`
	 * 
	 */
	val access: GPUStorageTextureAccess
	/**
	 * Specifies the required format of texture views bound to this binding. This property is mandatory and defines how the texture data is interpreted.
	 * 
	 * **Type:** [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat)
	 * 
	 */
	val format: GPUTextureFormat
	/**
	 * Specifies the required dimension for texture views bound to this binding. This property defaults to `GPUTextureViewDimension.D2`.
	 * 
	 * **Type:** [GPUTextureViewDimension](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension)
	 * 
	 * **Default Value:** `GPUTextureViewDimension.D2`
	 * 
	 */
	val viewDimension: GPUTextureViewDimension
}

/**
 * The `GPUBindGroupDescriptor` interface represents a descriptor for creating bind groups in WebGPU. It extends the `GPUObjectDescriptorBase` and is used to specify the layout and entries of a bind group.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpubindgroupdescriptor).
 * 
 */
interface GPUBindGroupDescriptor : GPUObjectDescriptorBase {
	/**
	 * The `layout` property specifies the `GPUBindGroupLayout` that the entries of this bind group will conform to. This layout defines how resources are bound and accessed in shaders.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubindgroupdescriptor-layout).
	 * 
	 */
	val layout: GPUBindGroupLayout
	/**
	 * The `entries` property is a list of `GPUBindGroupEntry` objects that describe the resources to expose to the shader for each binding described by the `layout`. Each entry specifies how a particular resource should be bound.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpubindgroupdescriptor-entries).
	 * 
	 */
	val entries: List<GPUBindGroupEntry>
}

/**
 * Represents a single resource to be bound in a [GPUBindGroup]. This interface is used to describe the binding of resources such as samplers, texture views, external textures, or buffer bindings within a bind group.
 * 
 * For more details, refer to the [WebGPU specification on GPUBindGroupEntry](https://www.w3.org/TR/webgpu/#dictdef-gpubindgroupentry).
 * 
 */
interface GPUBindGroupEntry {
	/**
	 * A unique identifier for a resource binding within the [GPUBindGroup]. This identifier corresponds to a `GPUBindGroupLayoutEntry.binding` and a `@binding` attribute in the [GPUShaderModule].
	 * 
	 * **Type:** [GPUIndex32](TYPE_MAPPING.md)
	 * 
	 */
	val binding: GPUIndex32
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
	val resource: GPUBindingResource
}

/**
 * The `GPUPipelineLayoutDescriptor` interface defines all the [GPUBindGroupLayout](https://www.w3.org/TR/webgpu/#dictdef-gpubindgrouplayout)s used by a pipeline. This descriptor is essential for configuring the layout of bind groups in a GPU pipeline, ensuring that shader modules can access resources correctly.
 * 
 * **Inheritance**: This interface inherits from [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase).
 * 
 * In this example, `bindGroupLayout1` and `bindGroupLayout2` are instances of [GPUBindGroupLayout]. The `bindGroupLayouts` list defines the layout of bind groups that the pipeline will use.
 * 
 */
interface GPUPipelineLayoutDescriptor : GPUObjectDescriptorBase {
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
	val bindGroupLayouts: List<GPUBindGroupLayout>
	/**
	 * The size, in bytes, of the immediate data range used by the pipeline.
	 * 
	 */
	val immediateSize: GPUSize32
}

/**
 * Represents a descriptor for creating a [GPUShaderModule] in WebGPU. This interface extends `GPUObjectDescriptorBase` and is used to specify the WGSL source code and compilation hints required to create a shader module. The shader module is a compiled version of the shader code that can be used in rendering or compute pipelines.
 * 
 * **See also:**
 * - [W3C WebGPU Specification: GPUShaderModuleDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpushadermoduledescriptor)
 * 
 */
interface GPUShaderModuleDescriptor : GPUObjectDescriptorBase {
	/**
	 * The WGSL source code for the shader module. This string contains the shader program written in the WebGPU Shading Language (WGSL). The shader code defines the vertex and fragment shaders or compute shaders that will be used in the rendering or compute pipeline.
	 * 
	 * **Type:** `String`
	 * 
	 * **See also:**
	 * - [W3C WebGPU Specification: GPUShaderModuleDescriptor.code](https://www.w3.org/TR/webgpu/#dom-gpushadermoduledescriptor-code)
	 * 
	 */
	val code: String
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
	val compilationHints: List<GPUShaderModuleCompilationHint>
}

/**
 * Represents a hint for compiling a GPUShaderModule. This interface provides information about the entry point and layout that may be used with the shader module in future pipeline creation calls.
 * 
 * For more details, refer to the WebGPU specification on Shader Module Compilation Information: https://www.w3.org/TR/webgpu/#shader-module-compilation-information.
 * 
 */
interface GPUShaderModuleCompilationHint {
	/**
	 * The entry point of the shader module. This is a required field and must be specified.
	 * 
	 * Type: String (https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/).
	 * 
	 */
	val entryPoint: String
	/**
	 * A GPUPipelineLayout that the shader module may be used with in future pipeline creation calls. If set to null, the default pipeline layout for the entry point associated with this hint will be used.
	 * 
	 * Type: GPUPipelineLayout or GPUAutoLayoutMode.
	 * 
	 */
	val layout: GPUPipelineLayout?
}

/**
 * Represents the base descriptor for a GPU pipeline. This interface extends [GPUObjectDescriptorBase] and is used to define the layout of a GPU pipeline.
 * 
 * The `layout` property specifies either a [GPUPipelineLayout] or an automatic layout mode (`"auto"`). When `"auto"` is specified, the pipeline layout is generated automatically.
 * 
 */
interface GPUPipelineDescriptorBase : GPUObjectDescriptorBase {
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
	val layout: GPUPipelineLayout?
}

/**
 * Represents a programmable stage in a GPU pipeline. This interface describes the entry point in a user-provided [GPUShaderModule] that controls one of the programmable stages of a pipeline.
 * Entry point names follow the rules defined in [WGSL identifier comparison](https://gpuweb.github.io/gpuweb/wgsl/#identifier-comparison).
 * 
 */
interface GPUProgrammableStage {
	/**
	 * The shader module containing the entry point for this programmable stage. This is a required field and must be provided when creating an instance of [GPUProgrammableStage].
	 * 
	 */
	val module: GPUShaderModule
	/**
	 * The name of the entry point in the shader module. This is an optional field and can be null if not specified.
	 * Entry point names must follow the rules defined in [WGSL identifier comparison](https://gpuweb.github.io/gpuweb/wgsl/#identifier-comparison).
	 * 
	 */
	val entryPoint: String?
	/**
	 * A map of constant values that can be passed to the shader module. The keys are strings representing the names of the constants, and the values are of type [GPUPipelineConstantValue].
	 * This field is optional and defaults to an empty map if not provided.
	 * 
	 */
	val constants: Map<String, GPUPipelineConstantValue>
}

/**
 * Represents a descriptor for creating a compute pipeline in WebGPU. This interface extends [GPUPipelineDescriptorBase] and is used to define the configuration for a compute pipeline, which executes compute shaders.
 * 
 * A compute pipeline is responsible for performing general-purpose computations on the GPU. It does not render graphics but can be used for tasks such as data processing, simulations, and other parallel computations.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpucomputepipelinedescriptor).
 * 
 */
interface GPUComputePipelineDescriptor : GPUPipelineDescriptorBase {
	/**
	 * Specifies the compute shader stage for the pipeline. This member is required and must be set to a valid [GPUProgrammableStage] object that describes the compute shader entry point.
	 * 
	 * The compute shader is responsible for executing the compute operations defined in the shader code. It does not produce visual output but can perform parallel computations on data.
	 * 
	 * **Type:** [GPUProgrammableStage]
	 * 
	 */
	val compute: GPUProgrammableStage
}

/**
 * The `GPURenderPipelineDescriptor` interface extends `GPUPipelineDescriptorBase` and defines the configuration for a render pipeline in WebGPU. It specifies the vertex, primitive, depth-stencil, multisample, and fragment states required to create a render pipeline. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dictdef-gpurenderpipelinedescriptor).
 * 
 */
interface GPURenderPipelineDescriptor : GPUPipelineDescriptorBase {
	/**
	 * `vertex` of type `GPUVertexState`. Describes the vertex shader entry point of the pipeline and its input buffer layouts. This is a required field. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-vertex).
	 * 
	 */
	val vertex: GPUVertexState
	/**
	 * `primitive` of type `GPUPrimitiveState`, defaulting to `{}` if not provided. Describes the primitive-related properties of the pipeline, such as topology and strip index format. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-primitive).
	 * 
	 */
	val primitive: GPUPrimitiveState
	/**
	 * `depthStencil` of type `GPUDepthStencilState?`. Describes the optional depth-stencil properties, including testing, operations, and bias. This field is nullable. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-depthstencil).
	 * 
	 */
	val depthStencil: GPUDepthStencilState?
	/**
	 * `multisample` of type `GPUMultisampleState`, defaulting to `{}` if not provided. Describes the multi-sampling properties of the pipeline, such as count and mask. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-multisample).
	 * 
	 */
	val multisample: GPUMultisampleState
	/**
	 * `fragment` of type `GPUFragmentState?`. Describes the fragment shader entry point of the pipeline and its output colors. If not provided, the [no color output mode](https://www.w3.org/TR/webgpu/#no-color-output) is enabled. This field is nullable. For more details, refer to the [W3C specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpipelinedescriptor-fragment).
	 * 
	 */
	val fragment: GPUFragmentState?
}

/**
 * Represents the state of a primitive in WebGPU, defining how primitives are rendered. This interface is used to configure various aspects of primitive rendering such as topology, strip index format, front face orientation, cull mode, and depth clipping behavior.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuprimitivestate).
 * 
 */
interface GPUPrimitiveState {
	/**
	 * Specifies the type of primitive topology used for rendering. This determines how vertices are interpreted when drawing primitives.
	 * 
	 * **Default Value:** `GPUPrimitiveTopology.TriangleList`
	 * 
	 * **See Also:**
	 * - [GPUPrimitiveTopology](https://www.w3.org/TR/webgpu/#enumdef-gpuprimitivetopology)
	 * 
	 */
	val topology: GPUPrimitiveTopology
	/**
	 * Specifies the format of the strip index buffer, if used. This is relevant when rendering primitives that use strip indexing.
	 * 
	 * **Default Value:** `null`
	 * 
	 * **See Also:**
	 * - [GPUIndexFormat](https://www.w3.org/TR/webgpu/#enumdef-gpuindexformat)
	 * 
	 */
	val stripIndexFormat: GPUIndexFormat?
	/**
	 * Specifies the orientation of the front face of primitives. This determines which side of a triangle is considered the front face for culling and other operations.
	 * 
	 * **Default Value:** `GPUFrontFace.CCW`
	 * 
	 * **See Also:**
	 * - [GPUFrontFace](https://www.w3.org/TR/webgpu/#enumdef-gpufrontface)
	 * 
	 */
	val frontFace: GPUFrontFace
	/**
	 * Specifies the culling mode for primitives. This determines which faces of a primitive are discarded during rendering.
	 * 
	 * **Default Value:** `GPUCullMode.None`
	 * 
	 * **See Also:**
	 * - [GPUCullMode](https://www.w3.org/TR/webgpu/#enumdef-gpucullmode)
	 * 
	 */
	val cullMode: GPUCullMode
	/**
	 * Specifies whether depth values are clipped or unclipped. This feature requires the `"depth-clip-control"` feature to be enabled.
	 * 
	 * **Default Value:** `false`
	 * 
	 * **See Also:**
	 * - [WebGPU Features](https://www.w3.org/TR/webgpu/#features)
	 * 
	 */
	val unclippedDepth: Boolean
}

/**
 * Represents the multisampling state used by a [GPURenderPipeline](https://www.w3.org/TR/webgpu/#gpurenderpipeline) to interact with render pass attachments that support multisampling.
 * 
 * This interface defines how many samples per pixel are used, which samples are written to, and whether alpha-to-coverage is enabled. The multisample state is crucial for rendering high-quality images by reducing aliasing artifacts.
 * 
 * **See also:**
 * - [GPUMultisampleState dictionary in the WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpumultisamplestate)
 * 
 */
interface GPUMultisampleState {
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
	val count: GPUSize32
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
	val mask: GPUSampleMask
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
	val alphaToCoverageEnabled: Boolean
}

/**
 * Represents a fragment state in WebGPU, which is a type of programmable stage that defines how fragments are processed during rendering. This interface extends [GPUProgrammableStage] and includes specific configurations for color targets.
 * 
 * The `GPUFragmentState` interface is used to configure the fragment shader stage of a GPU pipeline, specifying how the colors are written to the render target. This is crucial for defining the visual output of a rendering operation.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUProgrammableStage](https://www.w3.org/TR/webgpu/#gpuprogrammablestage)
 * 
 */
interface GPUFragmentState : GPUProgrammableStage {
	/**
	 * A list of [GPUColorTargetState] objects that define the formats and behaviors of the color targets this pipeline writes to. Each `GPUColorTargetState` in the list specifies how a particular color target should be handled during rendering.
	 * 
	 * **Type:** List<[GPUColorTargetState]?>
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUFragmentState](https://www.w3.org/TR/webgpu/#dictdef-gpufragmentstate)
	 * 
	 */
	val targets: List<GPUColorTargetState>
}

/**
 * Represents the state of a color target in a GPU render pipeline. This interface defines the format, blending behavior, and write mask for a color attachment in a render pass.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpucolortargetstate).
 * 
 */
interface GPUColorTargetState {
	/**
	 * The format of this color target. The pipeline will only be compatible with render pass encoders which use a texture view of this format in the corresponding color attachment.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolortargetstate-format).
	 * 
	 */
	val format: GPUTextureFormat
	/**
	 * The blending behavior for this color target. If left undefined, disables blending for this color target.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolortargetstate-blend).
	 * 
	 */
	val blend: GPUBlendState?
	/**
	 * Bitmask controlling which channels are written to when drawing to this color target. Defaults to `0xF` (all channels).
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucolortargetstate-writemask).
	 * 
	 */
	val writeMask: GPUColorWrite
}

/**
 * Represents the blend state used in rendering operations, defining how colors and alpha values are blended.
 * 
 * This interface is part of the WebGPU API and corresponds to the `GPUBlendState` dictionary defined in the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpublendstate).
 * 
 * The `GPUBlendState` interface includes two properties: `color` and `alpha`, both of type `GPUBlendComponent`. These properties specify the blending behavior for color channels and alpha channels, respectively.
 * 
 */
interface GPUBlendState {
	/**
	 * Defines the blending behavior of the corresponding render target for color channels.
	 * 
	 * This property is of type `GPUBlendComponent` and specifies how the color channels are blended during rendering. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpublendstate-color).
	 * 
	 */
	val color: GPUBlendComponent
	/**
	 * Defines the blending behavior of the corresponding render target for the alpha channel.
	 * 
	 * This property is of type `GPUBlendComponent` and specifies how the alpha channel is blended during rendering. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpublendstate-alpha).
	 * 
	 */
	val alpha: GPUBlendComponent
}

/**
 * Represents a blend component used in blending operations for color or alpha components of a fragment. This interface defines how the source and destination colors are combined during rendering.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpublendcomponent).
 * 
 */
interface GPUBlendComponent {
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
	val operation: GPUBlendOperation
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
	val srcFactor: GPUBlendFactor
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
	val dstFactor: GPUBlendFactor
}

/**
 * Represents the depth and stencil state configuration for a GPU render pipeline. This interface defines various properties that control how depth and stencil tests are performed during rendering.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#depth-stencil-state).
 * 
 */
interface GPUDepthStencilState {
	/**
	 * Specifies the format of the depth/stencil texture. This property determines how depth and stencil values are stored in the texture.
	 * 
	 * **Type:** [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat)
	 * 
	 */
	val format: GPUTextureFormat
	/**
	 * Indicates whether depth values are written to the depth buffer. When set to `true`, depth values are written; when set to `false` or `null`, depth values are not written.
	 * 
	 * **Type:** Boolean?
	 * 
	 */
	val depthWriteEnabled: Boolean?
	/**
	 * Specifies the comparison function used for depth tests. This property determines how the current depth value is compared to the stored depth value.
	 * 
	 * **Type:** [GPUCompareFunction](https://www.w3.org/TR/webgpu/#enumdef-gpucomparefunction)
	 * 
	 */
	val depthCompare: GPUCompareFunction?
	/**
	 * Defines the stencil state for front-facing primitives. This property configures how stencil tests are performed for front-facing geometry.
	 * 
	 * **Type:** [GPUStencilFaceState](https://www.w3.org/TR/webgpu/#dictdef-gpustencilfacestate)
	 * 
	 */
	val stencilFront: GPUStencilFaceState
	/**
	 * Defines the stencil state for back-facing primitives. This property configures how stencil tests are performed for back-facing geometry.
	 * 
	 * **Type:** [GPUStencilFaceState](https://www.w3.org/TR/webgpu/#dictdef-gpustencilfacestate)
	 * 
	 */
	val stencilBack: GPUStencilFaceState
	/**
	 * Specifies the mask used for reading stencil values. This property determines which bits of the stencil value are considered during read operations.
	 * 
	 * **Type:** [GPUStencilValue](https://www.w3.org/TR/webgpu/#typedefdef-gpustencilvalue)
	 * 
	 * **Default Value:** `0xFFFFFFFF`
	 * 
	 */
	val stencilReadMask: GPUStencilValue
	/**
	 * Specifies the mask used for writing stencil values. This property determines which bits of the stencil value are modified during write operations.
	 * 
	 * **Type:** [GPUStencilValue](https://www.w3.org/TR/webgpu/#typedefdef-gpustencilvalue)
	 * 
	 * **Default Value:** `0xFFFFFFFF`
	 * 
	 */
	val stencilWriteMask: GPUStencilValue
	/**
	 * Specifies the depth bias value. This property is used to adjust the depth values for polygon offset.
	 * 
	 * **Type:** [GPUDepthBias](https://www.w3.org/TR/webgpu/#typedefdef-gpudepthbias)
	 * 
	 * **Default Value:** `0`
	 * 
	 */
	val depthBias: GPUDepthBias
	/**
	 * Specifies the slope scale factor for depth bias. This property is used to adjust the depth bias based on the slope of the polygon.
	 * 
	 * **Type:** Float
	 * 
	 * **Default Value:** `0.0`
	 * 
	 */
	val depthBiasSlopeScale: Float
	/**
	 * Specifies the clamp value for depth bias. This property limits the maximum depth bias that can be applied.
	 * 
	 * **Type:** Float
	 * 
	 * **Default Value:** `0.0`
	 * 
	 */
	val depthBiasClamp: Float
}

/**
 * Represents a set of stencil face state parameters that define how stencil tests and operations are performed. This interface is used to configure the behavior of the stencil buffer for front or back-facing triangles in a render pipeline.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpustencilfacestate).
 * 
 */
interface GPUStencilFaceState {
	/**
	 * Specifies the comparison function used for stencil tests. This determines how the current stencil value is compared to the reference value.
	 * 
	 * **Default Value:** `GPUCompareFunction.Always`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.compare](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-compare)
	 * 
	 */
	val compare: GPUCompareFunction
	/**
	 * Specifies the operation to perform when the stencil test fails. This defines what action to take if the comparison function does not pass.
	 * 
	 * **Default Value:** `GPUStencilOperation.Keep`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.failOp](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-failop)
	 * 
	 */
	val failOp: GPUStencilOperation
	/**
	 * Specifies the operation to perform when the stencil test passes but the depth test fails. This defines what action to take if the comparison function passes but the depth test does not.
	 * 
	 * **Default Value:** `GPUStencilOperation.Keep`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.depthFailOp](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-depthfailop)
	 * 
	 */
	val depthFailOp: GPUStencilOperation
	/**
	 * Specifies the operation to perform when both the stencil test and the depth test pass. This defines what action to take if both tests are successful.
	 * 
	 * **Default Value:** `GPUStencilOperation.Keep`
	 * 
	 * **See Also:**
	 * - [W3C WebGPU specification: GPUStencilFaceState.passOp](https://www.w3.org/TR/webgpu/#dom-gpustencilfacestate-passop)
	 * 
	 */
	val passOp: GPUStencilOperation
}

/**
 * Represents a vertex state in the WebGPU API, defining how vertex data is laid out and processed. This interface extends [GPUProgrammableStage], allowing it to be used as part of a render pipeline.
 * 
 * A `GPUVertexState` object specifies the layout of vertex attribute data in vertex buffers. Each buffer's layout is defined by a list of `GPUVertexBufferLayout` objects, which describe the structure and stride of the vertex data.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#vertex-state).
 * 
 */
interface GPUVertexState : GPUProgrammableStage {
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
	val buffers: List<GPUVertexBufferLayout>
}

/**
 * Represents the layout of a vertex buffer in WebGPU. This interface defines how vertices are structured and accessed, including the stride between elements, the step mode (whether data is per-vertex or per-instance), and the attributes that describe the vertex data.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexbufferlayout).
 * 
 */
interface GPUVertexBufferLayout {
	/**
	 * The stride, in bytes, between elements of this array. This value specifies how much memory is allocated for each vertex or instance in the buffer.
	 * 
	 * **Type:** [GPUSize64](https://www.w3.org/TR/webgpu/#typedefdef-gpusize64)
	 * 
	 */
	val arrayStride: GPUSize64
	/**
	 * Specifies whether each element of this array represents per-vertex data or per-instance data. The default value is `GPUVertexStepMode.VERTEX`.
	 * 
	 * **Type:** [GPUVertexStepMode](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexstepmode)
	 * 
	 */
	val stepMode: GPUVertexStepMode
	/**
	 * An array defining the layout of the vertex attributes within each element. This sequence describes how the vertex data is structured and accessed.
	 * 
	 * **Type:** List<[GPUVertexAttribute](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexattribute)>
	 * 
	 */
	val attributes: List<GPUVertexAttribute>
}

/**
 * Represents a vertex attribute in the WebGPU API. This interface defines the format, offset, and shader location of a vertex attribute.
 * 
 * A `GPUVertexAttribute` is used to describe how data from a vertex buffer should be interpreted by the GPU. It specifies the format of the data (e.g., float32, uint32), the byte offset within the vertex buffer where the data starts, and the shader location that corresponds to this attribute.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpuvertexattribute).
 * 
 */
interface GPUVertexAttribute {
	/**
	 * The format of the vertex attribute. This specifies how the data should be interpreted by the GPU.
	 * 
	 * **Type:** [GPUVertexFormat]
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuvertexattribute-format).
	 * 
	 */
	val format: GPUVertexFormat
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
	val offset: GPUSize64
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
	val shaderLocation: GPUIndex32
}

/**
 * The `GPUTexelCopyBufferLayout` interface describes the layout of texels in a buffer of bytes during a texel copy operation. This interface is used to define how data is organized in a [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) or an [AllowSharedBufferSource](https://webidl.spec.whatwg.org/#AllowSharedBufferSource) when performing texel copy operations.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gputexelcopybufferlayout).
 * 
 */
interface GPUTexelCopyBufferLayout {
	/**
	 * The `offset` property specifies the starting offset in bytes from the beginning of the buffer where the texel data begins. This value is of type [GPUSize64], which represents a 64-bit unsigned integer.
	 * 
	 * **Default Value:** `0`
	 * 
	 */
	val offset: GPUSize64
	/**
	 * The `bytesPerRow` property specifies the number of bytes per row in the texel data. This value is of type [GPUSize32], which represents a 32-bit unsigned integer.
	 * 
	 */
	val bytesPerRow: GPUSize32?
	/**
	 * The `rowsPerImage` property specifies the number of rows per image in the texel data. This value is of type [GPUSize32], which represents a 32-bit unsigned integer.
	 * 
	 */
	val rowsPerImage: GPUSize32?
}

/**
 * The `GPUTexelCopyBufferInfo` interface describes the information about a buffer source or destination of a texel copy operation. This includes details such as the buffer itself and its layout.
 * 
 * Together with the `copySize`, it defines the footprint of a region of texels in a [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer). This interface is essential for operations that involve copying texel data between buffers and textures.
 * 
 * For more details, refer to the [WebGPU specification on GPUTexelCopyBufferInfo](https://www.w3.org/TR/webgpu/#gputexelcopybufferinfo).
 * 
 */
interface GPUTexelCopyBufferInfo : GPUTexelCopyBufferLayout {
	/**
	 * The `buffer` property represents a buffer that either contains texel data to be copied or will store the texel data being copied, depending on the method it is being passed to.
	 * 
	 * This property is of type [GPUBuffer](https://www.w3.org/TR/webgpu/#gpubuffer) and must be a valid GPU buffer. The validity of this buffer is checked during the validation process of `GPUTexelCopyBufferInfo`.
	 * 
	 */
	val buffer: GPUBuffer
}

/**
 * Represents the information about a texture source or destination for a texel copy operation. This interface describes the sub-region of a texture that spans one or more contiguous texture subresources at the same mip-map level.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gputexelcopytextureinfo).
 * 
 */
interface GPUTexelCopyTextureInfo {
	/**
	 * The texture to copy to or from. This is a required field and must be specified.
	 * 
	 * **Type:** GPUTexture
	 * 
	 */
	val texture: GPUTexture
	/**
	 * The mip-map level of the texture to copy to or from. This field defaults to `0` if not specified.
	 * 
	 * **Type:** GPUIntegerCoordinate
	 * 
	 * **Default Value:** 0
	 * 
	 */
	val mipLevel: GPUIntegerCoordinate
	/**
	 * Defines the origin of the copy, which is the minimum corner of the texture sub-region to copy to or from. Together with `copySize`, this defines the full copy sub-region. This field defaults to `{}` if not specified.
	 * 
	 * **Type:** GPUOrigin3D
	 * 
	 * **Default Value:** `{}` (an empty GPUOrigin3D object)
	 * 
	 */
	val origin: GPUOrigin3D
	/**
	 * Defines which aspects of the texture to copy to or from. This field defaults to `all` if not specified.
	 * 
	 * **Type:** GPUTextureAspect
	 * 
	 * **Default Value:** GPUTextureAspect.ALL
	 * 
	 */
	val aspect: GPUTextureAspect
}

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
interface GPUCommandBufferDescriptor : GPUObjectDescriptorBase
/**
 * The `GPUCommandEncoderDescriptor` interface represents a descriptor used to create a [GPUCommandEncoder](https://www.w3.org/TR/webgpu/#gpucommandencoder) object. This descriptor inherits from the base descriptor interface [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase).
 * 
 * The `GPUCommandEncoderDescriptor` is used to specify configuration options for the command encoder, such as label and device.
 * 
 * **See Also:**
 * - [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase)
 * 
 */
interface GPUCommandEncoderDescriptor : GPUObjectDescriptorBase
/**
 * Represents a dictionary that specifies the query set and indices where timestamps will be written during a compute pass. This interface is used to measure the duration of compute passes by recording timestamps at the beginning and end of the pass.
 * 
 * For more details, refer to the [WebGPU specification on GPUComputePassTimestampWrites](https://www.w3.org/TR/webgpu/#dictdef-gpucomputepasstimestampwrites).
 * 
 */
interface GPUComputePassTimestampWrites {
	/**
	 * The `GPUQuerySet` of type "timestamp" that the query results will be written to. This set contains the queries where the timestamps will be recorded.
	 * 
	 * **Type**: [GPUQuerySet](https://www.w3.org/TR/webgpu/#gpuqueryset)
	 * 
	 */
	val querySet: GPUQuerySet
	/**
	 * If defined, indicates the query index in `querySet` into which the timestamp at the beginning of the compute pass will be written. This value is of type [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32).
	 * 
	 * **Type**: GPUSize32?
	 * 
	 */
	val beginningOfPassWriteIndex: GPUSize32?
	/**
	 * If defined, indicates the query index in `querySet` into which the timestamp at the end of the compute pass will be written. This value is of type [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32).
	 * 
	 * **Type**: GPUSize32?
	 * 
	 */
	val endOfPassWriteIndex: GPUSize32?
}

/**
 * Represents a descriptor for configuring a compute pass in WebGPU. This interface extends [GPUObjectDescriptorBase] and is used to specify the details of a compute pass, including timestamp writes.
 * 
 * For more information, see the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpucomputepassdescriptor).
 * 
 */
interface GPUComputePassDescriptor : GPUObjectDescriptorBase {
	/**
	 * Defines which timestamp values will be written for this pass and where to write them.
	 * 
	 * This property is of type [GPUComputePassTimestampWrites].
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpucomputepassdescriptor-timestampwrites).
	 * 
	 */
	val timestampWrites: GPUComputePassTimestampWrites?
}

/**
 * Represents a dictionary that specifies the query set and indices where timestamps will be written during a render pass. This interface is used to capture timing information at the beginning and end of a render pass.
 * 
 * For more details, refer to the [WebGPU specification on GPURenderPassTimestampWrites](https://www.w3.org/TR/webgpu/#dom-gpurenderpasstimestampwrites).
 * 
 */
interface GPURenderPassTimestampWrites {
	/**
	 * The GPUQuerySet of type `timestamp` that the query results will be written to.
	 * 
	 * This property is required and specifies the query set where the timestamps will be recorded.
	 * 
	 */
	val querySet: GPUQuerySet
	/**
	 * An optional index in the [querySet] that indicates where the timestamp at the beginning of the render pass will be written.
	 * 
	 * If defined, this property specifies the exact query index within the `querySet` where the start timestamp of the render pass will be recorded.
	 * 
	 */
	val beginningOfPassWriteIndex: GPUSize32?
	/**
	 * An optional index in the [querySet] that indicates where the timestamp at the end of the render pass will be written.
	 * 
	 * If defined, this property specifies the exact query index within the `querySet` where the end timestamp of the render pass will be recorded.
	 * 
	 */
	val endOfPassWriteIndex: GPUSize32?
}

/**
 * The `GPURenderPassDescriptor` interface defines the configuration for a render pass in WebGPU. It specifies the color attachments, depth/stencil attachment, occlusion query set, timestamp writes, and maximum draw count for the render pass.
 * 
 * This descriptor is used to configure the rendering process by specifying how different types of data will be handled during the render pass. The `colorAttachments` property defines which color buffers will receive the output from the render pass. The `depthStencilAttachment` specifies the depth/stencil buffer that will be used for depth testing and stencil operations. The `occlusionQuerySet` allows for occlusion queries to be performed, and the `timestampWrites` can be used to write timestamps during the render pass.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpurenderpassdescriptor).
 * 
 */
interface GPURenderPassDescriptor : GPUObjectDescriptorBase {
	/**
	 * The `colorAttachments` property is a list of `GPURenderPassColorAttachment` objects that define the color attachments for the render pass. Each attachment specifies how the output from the render pass will be written to a particular color buffer.
	 * 
	 * Due to usage compatibility, no color attachment may alias another attachment or any resource used inside the render pass.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdescriptor-colorattachments).
	 * 
	 */
	val colorAttachments: List<GPURenderPassColorAttachment>
	/**
	 * The `depthStencilAttachment` property specifies a `GPURenderPassDepthStencilAttachment` object that defines the depth/stencil attachment for the render pass. This attachment is used for depth testing and stencil operations during the rendering process.
	 * 
	 * Due to usage compatibility, no writable depth/stencil attachment may alias another attachment or any resource used inside the render pass.
	 * 
	 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdescriptor-depthstencilattachment).
	 * 
	 */
	val depthStencilAttachment: GPURenderPassDepthStencilAttachment?
	/**
	 * The `occlusionQuerySet` property specifies a `GPUQuerySet` object that defines where the occlusion query results will be stored for this render pass. Occlusion queries are used to determine whether certain pixels were rendered during the pass.
	 * 
	 */
	val occlusionQuerySet: GPUQuerySet?
	/**
	 * The `timestampWrites` property allows you to specify a list of `GPUQuerySet` objects that define where timestamp query results will be stored for this render pass. Timestamps are used to measure the time taken by different parts of the rendering process.
	 * 
	 */
	val timestampWrites: GPURenderPassTimestampWrites?
	/**
	 * The `maxDrawCount` property specifies the maximum number of draw calls that can be made during the render pass. This is useful for optimizing performance and managing resources efficiently.
	 * 
	 * Setting an appropriate value for `maxDrawCount` helps in preventing resource exhaustion and ensures smooth rendering.
	 * 
	 */
	val maxDrawCount: GPUSize64
}

/**
 * Represents a color attachment for a render pass in the WebGPU API. This interface defines the properties required to configure how colors are rendered and stored during a rendering operation.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dictdef-gpurenderpasscolorattachment).
 * 
 */
interface GPURenderPassColorAttachment {
	/**
	 * A GPUTextureView describing the texture subresource that will be output to for this color attachment.
	 * 
	 * This property is required and must be a valid renderable texture view. The format of the view must be a color renderable format.
	 * 
	 */
	val view: GPUTextureOrGPUTextureView
	/**
	 * Indicates the depth slice index of the GPUTextureView that will be output to for this color attachment when the view's dimension is "3d".
	 * 
	 * This property is optional and must only be provided if the GPUTextureView's dimension is "3d"
	 * 
	 */
	val depthSlice: GPUIntegerCoordinate?
	/**
	 * A GPUTextureView describing the texture subresource that will receive the resolved output for this color attachment if the GPUTextureView is multisampled.
	 * 
	 * This property is optional and must only be provided if the GPUTextureView's sample count is greater than 1. The resolve target must have a sample count of 1.
	 * 
	 */
	val resolveTarget: GPUTextureOrGPUTextureView?
	/**
	 * Indicates the value to clear the GPUTextureView to prior to executing the render pass.
	 * 
	 * This property is optional and defaults to {r: 0, g: 0, b: 0, a: 0} if not provided. It is ignored if the loadOp is not "clear". The components of clearValue are converted to a texel value of the texture format matching the render attachment.
	 * 
	 */
	val clearValue: GPUColor?
	/**
	 * Indicates the load operation to perform on the GPUTextureView prior to executing the render pass.
	 * 
	 * This property is required and specifies how the contents of the view should be handled before rendering. It can be one of the following values: "clear", "load", or "dont-care".
	 * 
	 */
	val loadOp: GPULoadOp
	/**
	 * The store operation to perform on the GPUTextureView after executing the render pass.
	 * 
	 * This property is required and specifies how the contents of the view should be handled after rendering. It can be one of the following values: "store", or "dont-store"
	 * 
	 */
	val storeOp: GPUStoreOp
}

/**
 * A union type representing either a `GPUTexture` or a `GPUTextureView`.
 * 
 */
sealed interface GPUTextureOrGPUTextureView
/**
 * The `GPURenderPassDepthStencilAttachment` interface represents a depth/stencil attachment for a render pass. It specifies the texture view and various operations to be performed on the depth and stencil components of that view during the render pass.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#gpurenderpassdepthstencilattachment).
 * 
 */
interface GPURenderPassDepthStencilAttachment {
	/**
	 * A `GPUTextureView` describing the texture subresource that will be output to and read from for this depth/stencil attachment.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-view).
	 * 
	 */
	val view: GPUTextureOrGPUTextureView
	/**
	 * Indicates the value to clear the `view`'s depth component to prior to executing the render pass. This value is ignored if `depthLoadOp` is not set to `GPULoadOp.CLEAR`. The value must be between 0.0 and 1.0, inclusive.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthclearvalue).
	 * 
	 */
	val depthClearValue: Float?
	/**
	 * Indicates the load operation to perform on the `view`'s depth component prior to executing the render pass. It is recommended to prefer clearing; see `GPULoadOp.CLEAR` for details.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthloadop).
	 * 
	 */
	val depthLoadOp: GPULoadOp?
	/**
	 * The store operation to perform on the `view`'s depth component after executing the render pass.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthstoreop).
	 * 
	 */
	val depthStoreOp: GPUStoreOp?
	/**
	 * Indicates that the depth component of the `view` is read-only. Defaults to `false`.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-depthreadonly).
	 * 
	 */
	val depthReadOnly: Boolean
	/**
	 * Indicates the value to clear the `view`'s stencil component to prior to executing the render pass. This value is ignored if `stencilLoadOp` is not set to `GPULoadOp.CLEAR`. The value will be converted to the type of the stencil aspect of the `view` by taking the same number of least significant bits (LSBs) as the number of bits in the stencil aspect of one texel of the `view`.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilclearvalue).
	 * 
	 */
	val stencilClearValue: GPUStencilValue
	/**
	 * Indicates the load operation to perform on the `view`'s stencil component prior to executing the render pass. It is recommended to prefer clearing; see `GPULoadOp.CLEAR` for details.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilloadop).
	 * 
	 */
	val stencilLoadOp: GPULoadOp?
	/**
	 * The store operation to perform on the `view`'s stencil component after executing the render pass.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilstoreop).
	 * 
	 */
	val stencilStoreOp: GPUStoreOp?
	/**
	 * Indicates that the stencil component of the `view` is read-only. Defaults to `false`.
	 * 
	 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpurenderpassdepthstencilattachment-stencilreadonly).
	 * 
	 */
	val stencilReadOnly: Boolean
}

/**
 * Represents the layout of a render pass, specifying the formats and sample counts for color and depth/stencil attachments.
 * 
 * This interface is used to define the configuration of a render pass, which includes the formats of the color attachments and the optional depth/stencil attachment. It also specifies the number of samples per pixel in the attachments.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#gpurenderpasslayout).
 * 
 */
interface GPURenderPassLayout : GPUObjectDescriptorBase {
	/**
	 * A list of the [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat)s of the color attachments for this pass or bundle.
	 * 
	 * This property specifies the formats of the color attachments that will be used in the render pass. Each format corresponds to a texture view that will be rendered to during the pass.
	 * 
	 */
	val colorFormats: List<GPUTextureFormat>
	/**
	 * The [GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat) of the depth/stencil attachment for this pass or bundle.
	 * 
	 * This property specifies the format of the depth/stencil attachment that will be used in the render pass. It is optional and can be `null` if no depth/stencil attachment is required.
	 * 
	 */
	val depthStencilFormat: GPUTextureFormat?
	/**
	 * Number of samples per pixel in the attachments for this pass or bundle.
	 * 
	 * This property specifies the number of samples per pixel for multisampling. The default value is `1`, which means no multisampling.
	 * 
	 */
	val sampleCount: GPUSize32
}

/**
 * Represents a descriptor for creating a [GPURenderBundle]. This interface inherits from [GPUObjectDescriptorBase], which provides common properties and methods for GPU objects.
 * 
 * The `GPURenderBundleDescriptor` is used to specify the configuration options when creating a render bundle. A render bundle encapsulates a sequence of rendering commands that can be executed multiple times with different parameters, improving performance by reducing the overhead of command encoding.
 * 
 */
interface GPURenderBundleDescriptor : GPUObjectDescriptorBase
/**
 * Represents a descriptor for creating a GPURenderBundleEncoder. This interface extends the GPURenderPassLayout and is used to specify whether the depth or stencil components of a render pass are read-only.
 * 
 * @see [WebGPU Specification - GPURenderBundleEncoderDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpurenderbundleencoderdescriptor)
 * 
 */
interface GPURenderBundleEncoderDescriptor : GPURenderPassLayout {
	/**
	 * Indicates whether the render bundle modifies the depth component of the GPURenderPassDepthStencilAttachment in any render pass it is executed in.
	 * 
	 * If set to true, the depth component is read-only. This can be useful for optimizing performance by avoiding unnecessary writes to the depth buffer.
	 * 
	 * @return A Boolean value indicating whether the depth component is read-only.
	 * @default false
	 * 
	 */
	val depthReadOnly: Boolean
	/**
	 * Indicates whether the render bundle modifies the stencil component of the GPURenderPassDepthStencilAttachment in any render pass it is executed in.
	 * 
	 * If set to true, the stencil component is read-only. This can be useful for optimizing performance by avoiding unnecessary writes to the stencil buffer.
	 * 
	 * @return A Boolean value indicating whether the stencil component is read-only.
	 * @default false
	 * 
	 */
	val stencilReadOnly: Boolean
}

/**
 * The `GPUQueueDescriptor` interface describes a queue request in the WebGPU API. This dictionary inherits from [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase), which means it includes all properties and methods defined by that base class.
 * 
 * The `GPUQueueDescriptor` is used to configure and create GPU queues, which are responsible for submitting commands to the GPU. This interface does not define any additional properties beyond those inherited from [GPUObjectDescriptorBase].
 * 
 * **See Also:**
 * - [GPUObjectDescriptorBase](https://www.w3.org/TR/webgpu/#dictdef-gpuobjectdescriptorbase) for inherited properties and methods.
 * 
 */
interface GPUQueueDescriptor : GPUObjectDescriptorBase
/**
 * Represents a descriptor for creating a [GPUQuerySet](https://www.w3.org/TR/webgpu/#gpuqueryset) object. This interface extends [GPUObjectDescriptorBase], providing the necessary configuration parameters to define the type and count of queries managed by the query set.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUQuerySetDescriptor](https://www.w3.org/TR/webgpu/#dictdef-gpuquerysetdescriptor)
 * 
 */
interface GPUQuerySetDescriptor : GPUObjectDescriptorBase {
	/**
	 * Specifies the type of queries managed by the [GPUQuerySet]. This property is required and must be set to one of the values defined in the [GPUQueryType](https://www.w3.org/TR/webgpu/#enumdef-gpuquerytype) enum.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: type](https://www.w3.org/TR/webgpu/#dom-gpuquerysetdescriptor-type)
	 * 
	 */
	val type: GPUQueryType
	/**
	 * Specifies the number of queries managed by the [GPUQuerySet]. This property is required and must be set to a valid [GPUSize32](https://www.w3.org/TR/webgpu/#typedefdef-gpusize32) value.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: count](https://www.w3.org/TR/webgpu/#dom-gpuquerysetdescriptor-count)
	 * 
	 */
	val count: GPUSize32
}

/**
 * Represents `GPUUncapturedErrorCallback` in the WebGPU API, as defined by the WebGPU specification.
 * 
 */
fun interface GPUUncapturedErrorCallback {
	/**
	 * Describes the `onUncapturedError` member of `GPUUncapturedErrorCallback`.
	 * 
	 */
	fun onUncapturedError(error: GPUError)
}
