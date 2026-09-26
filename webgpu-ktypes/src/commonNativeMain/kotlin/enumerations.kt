@file:Suppress("unused")
// This file has been generated DO NO EDIT
package io.ygdrasil.webgpu

/**
 * The `GPUAddressMode` enum defines how texture coordinates are handled outside the range [0.0, 1.0]. This enumeration is used to specify the addressing mode for sampling textures in WebGPU. For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpuaddressmode).
 * 
 * **Possible values:**
 * - `ClampToEdge`: Texture coordinates are clamped between 0.0 and 1.0, inclusive.
 * - `Repeat`: Texture coordinates wrap to the other side of the texture.
 * - `MirrorRepeat`: Texture coordinates wrap to the other side of the texture, but the texture is flipped when the integer part of the coordinate is odd.
 * 
 */
actual enum class GPUAddressMode(val value: UInt) {
	/**
	 * Texture coordinates are clamped between 0.0 and 1.0, inclusive. This means that any coordinate outside this range will be snapped to the nearest edge of the texture.
	 * 
	 */
	ClampToEdge(1u),
	/**
	 * Texture coordinates wrap to the other side of the texture. This means that coordinates outside the range [0.0, 1.0] will be wrapped around to the opposite side of the texture.
	 * 
	 */
	Repeat(2u),
	/**
	 * Texture coordinates wrap to the other side of the texture, but the texture is flipped when the integer part of the coordinate is odd. This creates a mirroring effect as the coordinates wrap around.
	 * 
	 */
	MirrorRepeat(3u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUAddressMode? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUBlendFactor` enum defines how either a source or destination blend factor is calculated. This enum is used to specify the blending factors for color components in the rendering pipeline. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpublendfactor).
 * 
 */
actual enum class GPUBlendFactor(val value: UInt) {
	/**
	 * Specifies a blend factor where all RGBA components are set to (0, 0, 0, 0). This effectively disables the blending for the specified component.
	 * 
	 */
	Zero(1u),
	/**
	 * Specifies a blend factor where all RGBA components are set to (1, 1, 1, 1). This fully enables the blending for the specified component.
	 * 
	 */
	One(2u),
	/**
	 * Specifies a blend factor where the RGBA components are taken from the source color. The values are (R<sub>src</sub>, G<sub>src</sub>, B<sub>src</sub>, A<sub>src</sub>).
	 * 
	 */
	Src(3u),
	/**
	 * Specifies a blend factor where the RGBA components are the inverse of the source color. The values are (1 - R<sub>src</sub>, 1 - G<sub>src</sub>, 1 - B<sub>src</sub>, 1 - A<sub>src</sub>).
	 * 
	 */
	OneMinusSrc(4u),
	/**
	 * Specifies a blend factor where the RGBA components are taken from the source alpha. The values are (A<sub>src</sub>, A<sub>src</sub>, A<sub>src</sub>, A<sub>src</sub>).
	 * 
	 */
	SrcAlpha(5u),
	/**
	 * Specifies a blend factor where the RGBA components are the inverse of the source alpha. The values are (1 - A<sub>src</sub>, 1 - A<sub>src</sub>, 1 - A<sub>src</sub>, 1 - A<sub>src</sub>).
	 * 
	 */
	OneMinusSrcAlpha(6u),
	/**
	 * Specifies a blend factor where the RGBA components are taken from the destination color. The values are (R<sub>dst</sub>, G<sub>dst</sub>, B<sub>dst</sub>, A<sub>dst</sub>).
	 * 
	 */
	Dst(7u),
	/**
	 * Specifies a blend factor where the RGBA components are the inverse of the destination color. The values are (1 - R<sub>dst</sub>, 1 - G<sub>dst</sub>, 1 - B<sub>dst</sub>, 1 - A<sub>dst</sub>).
	 * 
	 */
	OneMinusDst(8u),
	/**
	 * Specifies a blend factor where the RGBA components are taken from the destination alpha. The values are (A<sub>dst</sub>, A<sub>dst</sub>, A<sub>dst</sub>, A<sub>dst</sub>).
	 * 
	 */
	DstAlpha(9u),
	/**
	 * Specifies a blend factor where the RGBA components are the inverse of the destination alpha. The values are (1 - A<sub>dst</sub>, 1 - A<sub>dst</sub>, 1 - A<sub>dst</sub>, 1 - A<sub>dst</sub>).
	 * 
	 */
	OneMinusDstAlpha(10u),
	/**
	 * Specifies a blend factor where the RGBA components are the minimum of the source alpha and the inverse of the destination alpha. The values are (min(A<sub>src</sub>, 1 - A<sub>dst</sub>), min(A<sub>src</sub>, 1 - A<sub>dst</sub>), min(A<sub>src</sub>, 1 - A<sub>dst</sub>), 1).
	 * 
	 */
	SrcAlphaSaturated(11u),
	/**
	 * Specifies a blend factor where the RGBA components are taken from a constant color. The values are (R<sub>const</sub>, G<sub>const</sub>, B<sub>const</sub>, A<sub>const</sub>).
	 * 
	 */
	Constant(12u),
	/**
	 * Specifies a blend factor where the RGBA components are the inverse of a constant color. The values are (1 - R<sub>const</sub>, 1 - G<sub>const</sub>, 1 - B<sub>const</sub>, 1 - A<sub>const</sub>).
	 * 
	 */
	OneMinusConstant(13u),
	/**
	 * Specifies a blend factor where the RGBA components are taken from an additional source color (src1). The values are (R<sub>src1</sub>, G<sub>src1</sub>, B<sub>src1</sub>, A<sub>src1</sub>). This feature requires [dual-source blending](https://www.w3.org/TR/webgpu/#dom-gpufeaturename-dual-source-blending).
	 * 
	 */
	Src1(14u),
	/**
	 * Specifies a blend factor where the RGBA components are the inverse of an additional source color (src1). The values are (1 - R<sub>src1</sub>, 1 - G<sub>src1</sub>, 1 - B<sub>src1</sub>, 1 - A<sub>src1</sub>). This feature requires [dual-source blending](https://www.w3.org/TR/webgpu/#dom-gpufeaturename-dual-source-blending).
	 * 
	 */
	OneMinusSrc1(15u),
	/**
	 * Specifies a blend factor where the RGBA components are taken from an additional source alpha (src1). The values are (A<sub>src1</sub>, A<sub>src1</sub>, A<sub>src1</sub>, A<sub>src1</sub>). This feature requires [dual-source blending](https://www.w3.org/TR/webgpu/#dom-gpufeaturename-dual-source-blending).
	 * 
	 */
	Src1Alpha(16u),
	/**
	 * Specifies a blend factor where the RGBA components are the inverse of an additional source alpha (src1). The values are (1 - A<sub>src1</sub>, 1 - A<sub>src1</sub>, 1 - A<sub>src1</sub>, 1 - A<sub>src1</sub>). This feature requires [dual-source blending](https://www.w3.org/TR/webgpu/#dom-gpufeaturename-dual-source-blending).
	 * 
	 */
	OneMinusSrc1Alpha(17u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUBlendFactor? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUBlendOperation` enum defines the algorithm used to combine source and destination blend factors in WebGPU. This is crucial for controlling how colors are blended during rendering operations.
 * 
 * For more details, refer to the [WebGPU specification on GPUBlendOperation](https://www.w3.org/TR/webgpu/#enumdef-gpublendoperation).
 * 
 */
actual enum class GPUBlendOperation(val value: UInt) {
	/**
	 * Represents the blend operation that adds the source and destination colors. This operation is useful for combining light sources or adding textures.
	 * 
	 */
	Add(1u),
	/**
	 * Represents the blend operation that subtracts the destination color from the source color. This operation can be used for creating effects like shadows or decals.
	 * 
	 */
	Subtract(2u),
	/**
	 * Represents the blend operation that subtracts the source color from the destination color. This operation can be used for creating effects like inverse shadows or highlights.
	 * 
	 */
	ReverseSubtract(3u),
	/**
	 * Represents the blend operation that takes the minimum value between the source and destination colors component-wise. This operation can be used for creating effects like darkening or masking.
	 * 
	 */
	Min(4u),
	/**
	 * Represents the blend operation that takes the maximum value between the source and destination colors component-wise. This operation can be used for creating effects like brightening or highlighting.
	 * 
	 */
	Max(5u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUBlendOperation? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the type of binding for a buffer in WebGPU. This enum defines the possible types of buffer bindings that can be used when creating bind groups.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpubufferbindingtype).
 * 
 */
actual enum class GPUBufferBindingType(val value: UInt) {
	/**
	 * Indicates that the buffer binding is not used. This value signifies that no data will be bound to this slot in the bind group.
	 * 
	 */
	BindingNotUsed(0u),
	/**
	 * Indicates that the buffer binding is used for uniform data. Uniform buffers are typically used to pass constant data to shaders, such as transformation matrices or global parameters.
	 * 
	 */
	Uniform(2u),
	/**
	 * Indicates that the buffer binding is used for storage data. Storage buffers are used to read from and write to in shaders, allowing for more dynamic data manipulation.
	 * 
	 */
	Storage(3u),
	/**
	 * Indicates that the buffer binding is used for read-only storage data. Read-only storage buffers allow shaders to read from them but not write to them, providing a way to pass large datasets efficiently.
	 * 
	 */
	ReadOnlyStorage(4u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUBufferBindingType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the mapping state of a GPU buffer. This enum is used to indicate whether a buffer is unmapped, pending a map operation, or currently mapped.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpubuffermapstate).
 * 
 */
actual enum class GPUBufferMapState(val value: UInt) {
	/**
	 * Indicates that the buffer is not currently mapped for use by `getMappedRange()`. This state means that the buffer data cannot be accessed directly through JavaScript or Kotlin. The buffer must be mapped using the `mapAsync()` method before it can be read or written.
	 * 
	 * [See the W3C specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpubuffermapstate).
	 * 
	 */
	Unmapped(1u),
	/**
	 * Indicates that a mapping of the buffer has been requested but is still pending. The mapping process may succeed or fail validation during the `mapAsync()` call. During this state, the buffer data is not yet accessible.
	 * 
	 * [See the W3C specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpubuffermapstate).
	 * 
	 */
	Pending(2u),
	/**
	 * Indicates that the buffer is currently mapped and can be accessed using `getMappedRange()`. This state allows direct read or write access to the buffer data through an `ArrayBuffer` view.
	 * 
	 * [See the W3C specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpubuffermapstate).
	 * 
	 */
	Mapped(3u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUBufferMapState? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUCompareFunction` enum defines the possible comparison functions used in depth and stencil operations within WebGPU. These functions determine how values are compared during rendering processes, such as depth testing or stencil testing.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpucomparefunction).
 * 
 */
actual enum class GPUCompareFunction(val value: UInt) {
	/**
	 * The `Never` comparison function ensures that no values pass the comparison test. This means that the test will always fail, regardless of the input values.
	 * 
	 */
	Never(1u),
	/**
	 * The `Less` comparison function allows a provided value to pass the test if it is less than the sampled value. This is commonly used in depth testing to determine if a fragment should be discarded based on its depth.
	 * 
	 */
	Less(2u),
	/**
	 * The `Equal` comparison function allows a provided value to pass the test if it is equal to the sampled value. This can be used in stencil testing to perform exact comparisons.
	 * 
	 */
	Equal(3u),
	/**
	 * The `LessEqual` comparison function allows a provided value to pass the test if it is less than or equal to the sampled value. This is useful in depth testing for scenarios where equality should also allow the fragment to pass.
	 * 
	 */
	LessEqual(4u),
	/**
	 * The `Greater` comparison function allows a provided value to pass the test if it is greater than the sampled value. This can be used in reverse depth testing scenarios.
	 * 
	 */
	Greater(5u),
	/**
	 * The `NotEqual` comparison function allows a provided value to pass the test if it is not equal to the sampled value. This is useful in stencil testing for scenarios where inequality should allow the fragment to pass.
	 * 
	 */
	NotEqual(6u),
	/**
	 * The `GreaterEqual` comparison function allows a provided value to pass the test if it is greater than or equal to the sampled value. This can be used in depth testing for scenarios where equality should also allow the fragment to pass.
	 * 
	 */
	GreaterEqual(7u),
	/**
	 * The `Always` comparison function ensures that all values pass the comparison test. This means that the test will always succeed, regardless of the input values.
	 * 
	 */
	Always(8u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUCompareFunction? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUCompilationMessageType` enum defines the types of messages that can be generated during the compilation of GPU shaders. These messages provide information, warnings, or errors to help developers diagnose issues with their shader code.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#gpucompilationmessagetype).
 * 
 */
actual enum class GPUCompilationMessageType(val value: UInt) {
	/**
	 * `Error` indicates that a compilation error has occurred. This type of message is used when the shader code contains syntax errors or other issues that prevent successful compilation.
	 * 
	 */
	Error(1u),
	/**
	 * `Warning` indicates a warning message. This type of message is used to inform developers about potential issues in the shader code that do not prevent compilation but may affect performance or correctness.
	 * 
	 */
	Warning(2u),
	/**
	 * `Info` indicates an informational message. This type of message provides general information about the compilation process, such as optimization details or other non-critical messages.
	 * 
	 */
	Info(3u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUCompilationMessageType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the culling mode used in rasterization, specifying which faces of a primitive to discard during rendering.\n\nThis enum corresponds to the `GPUCullMode` defined in the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpucullmode).\n\nThe possible values are:\n- [None]: No culling is performed.\n- [Front]: Front-facing triangles are culled.\n- [Back]: Back-facing triangles are culled.
 * 
 */
actual enum class GPUCullMode(val value: UInt) {
	/**
	 * Specifies that no culling is performed. Both front-facing and back-facing triangles are rendered.\n\n**See also:**\n- [WebGPU specification: `none`](https://www.w3.org/TR/webgpu/#dom-gpucullmode-none)
	 * 
	 */
	None(1u),
	/**
	 * Specifies that front-facing triangles are culled. Only back-facing triangles are rendered.\n\n**See also:**\n- [WebGPU specification: `front`](https://www.w3.org/TR/webgpu/#dom-gpucullmode-front)
	 * 
	 */
	Front(2u),
	/**
	 * Specifies that back-facing triangles are culled. Only front-facing triangles are rendered.\n\n**See also:**\n- [WebGPU specification: `back`](https://www.w3.org/TR/webgpu/#dom-gpucullmode-back)
	 * 
	 */
	Back(3u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUCullMode? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUDeviceLostReason` enum represents the reasons why a GPU device might be lost. This is crucial for handling errors and managing resources in WebGPU applications.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpudevicelostreason).
 * 
 */
actual enum class GPUDeviceLostReason(val value: UInt) {
	/**
	 * Indicates that the reason for the GPU device being lost is unknown. This can occur due to unforeseen errors or issues not covered by other reasons.
	 * 
	 */
	Unknown(1u),
	/**
	 * Indicates that the GPU device was explicitly destroyed, typically through a call to `GPUDevice.destroy()`. This is a normal operation and should be handled gracefully.
	 * 
	 */
	Destroyed(2u),
	CallbackCancelled(3u),
	/**
	 * Indicates that the GPU device failed to be created. This can occur due to hardware limitations, driver issues, or other initialization problems.
	 * 
	 */
	FailedCreation(4u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUDeviceLostReason? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUErrorFilter` enum defines the types of errors that should be caught when calling [pushErrorScope]. This enum is used to specify which kinds of GPU errors are to be monitored within a particular scope.
 * 
 * For more details, refer to the [W3C WebGPU specification on GPUErrorFilter](https://www.w3.org/TR/webgpu/#enumdef-gpuerrorfilter).
 * 
 */
actual enum class GPUErrorFilter(val value: UInt) {
	/**
	 * Indicates that the error scope will catch a `GPUValidationError`. This is useful for catching errors related to validation failures in GPU operations.
	 * 
	 * For more details, refer to the [W3C WebGPU specification on GPUValidationError](https://www.w3.org/TR/webgpu/#gpuvalidationerror).
	 * 
	 */
	Validation(1u),
	/**
	 * Indicates that the error scope will catch a `GPUOutOfMemoryError`. This is useful for catching errors related to memory allocation failures in GPU operations.
	 * 
	 * For more details, refer to the [W3C WebGPU specification on GPUOutOfMemoryError](https://www.w3.org/TR/webgpu/#gpuoutofmemoryerror).
	 * 
	 */
	OutOfMemory(2u),
	/**
	 * Indicates that the error scope will catch a `GPUInternalError`. This is useful for catching internal errors that occur within the GPU implementation.
	 * 
	 * For more details, refer to the [W3C WebGPU specification on GPUInternalError](https://www.w3.org/TR/webgpu/#gpuinternalerror).
	 * 
	 */
	Internal(3u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUErrorFilter? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * "The `GPUFeatureName` enum defines a set of feature names that identify specific functionalities available in WebGPU. Each feature name corresponds to an additional usage of WebGPU that would otherwise be invalid if the feature is not supported.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#gpufeaturename)."
 * 
 */
actual enum class GPUFeatureName(val value: UInt) {
	CoreFeaturesAndLimits(1u),
	/**
	 * The `DepthClipControl` feature allows the use of depth clip control in WebGPU. This feature enables more precise control over depth clipping, which can be useful for advanced rendering techniques.
	 * 
	 * [See W3C specification: Depth Clip Control](https://www.w3.org/TR/webgpu/#depth-clip-control)
	 * 
	 */
	DepthClipControl(2u),
	/**
	 * The `Depth32FloatStencil8` feature indicates support for a depth buffer with 32-bit floating-point precision and an 8-bit stencil buffer. This is useful for high-precision depth testing and stencil operations.
	 * 
	 * [See W3C specification: Depth32FloatStencil8](https://www.w3.org/TR/webgpu/#depth32float-stencil8)
	 * 
	 */
	Depth32FloatStencil8(3u),
	/**
	 * The `TextureCompressionBC` feature indicates support for BC (Block Compression) texture formats. This can significantly reduce the memory footprint of textures, which is beneficial for performance and storage.
	 * 
	 * [See W3C specification: Texture Compression BC](https://www.w3.org/TR/webgpu/#texture-compression-bc)
	 * 
	 */
	TextureCompressionBC(4u),
	/**
	 * The `TextureCompressionBCSliced3D` feature indicates support for BC texture compression in sliced 3D textures. This is useful for compressing 3D textures, which can improve performance and reduce memory usage.
	 * 
	 * [See W3C specification: Texture Compression BC Sliced 3D](https://www.w3.org/TR/webgpu/#texture-compression-bc-sliced-3d)
	 * 
	 */
	TextureCompressionBCSliced3D(5u),
	/**
	 * The `TextureCompressionETC2` feature indicates support for ETC2 (Ericsson Texture Compression) texture formats. This can reduce the memory footprint of textures, which is beneficial for performance and storage.
	 * 
	 * [See W3C specification: Texture Compression ETC2](https://www.w3.org/TR/webgpu/#texture-compression-etc2)
	 * 
	 */
	TextureCompressionETC2(6u),
	/**
	 * The `TextureCompressionASTC` feature indicates support for ASTC (Adaptive Scalable Texture Compression) texture formats. This can significantly reduce the memory footprint of textures, which is beneficial for performance and storage.
	 * 
	 * [See W3C specification: Texture Compression ASTC](https://www.w3.org/TR/webgpu/#texture-compression-astc)
	 * 
	 */
	TextureCompressionASTC(7u),
	/**
	 * The `TextureCompressionASTCSliced3D` feature indicates support for ASTC texture compression in sliced 3D textures. This is useful for compressing 3D textures, which can improve performance and reduce memory usage.
	 * 
	 * [See W3C specification: Texture Compression ASTC Sliced 3D](https://www.w3.org/TR/webgpu/#texture-compression-astc-sliced-3d)
	 * 
	 */
	TextureCompressionASTCSliced3D(8u),
	/**
	 * The `TimestampQuery` feature allows the use of timestamp queries in WebGPU. This can be used to measure the time taken by specific GPU operations, which is useful for performance profiling and optimization.
	 * 
	 * [See W3C specification: Timestamp Query](https://www.w3.org/TR/webgpu/#timestamp-query)
	 * 
	 */
	TimestampQuery(9u),
	/**
	 * The `IndirectFirstInstance` feature allows the use of indirect drawing commands with the first instance parameter. This can be used to draw multiple instances of a mesh with different starting points, which is useful for complex scenes.
	 * 
	 * [See W3C specification: Indirect First Instance](https://www.w3.org/TR/webgpu/#indirect-first-instance)
	 * 
	 */
	IndirectFirstInstance(10u),
	/**
	 * The `ShaderF16` feature indicates support for 16-bit floating-point precision in shaders. This can reduce the memory footprint of shader data, which is beneficial for performance and storage.
	 * 
	 * [See W3C specification: Shader F16](https://www.w3.org/TR/webgpu/#shader-f16)
	 * 
	 */
	ShaderF16(11u),
	/**
	 * The `RG11B10UfloatRenderable` feature indicates support for rendering to textures with the RG11B10Ufloat format. This is useful for high-dynamic-range (HDR) rendering.
	 * 
	 * [See W3C specification: RG11B10Ufloat Renderable](https://www.w3.org/TR/webgpu/#rg11b10ufloat-renderable)
	 * 
	 */
	RG11B10UfloatRenderable(12u),
	/**
	 * The `BGRA8UnormStorage` feature indicates support for storing textures in the BGRA8Unorm format. This is useful for compatibility with certain graphics APIs and formats.
	 * 
	 * [See W3C specification: BGRA8Unorm Storage](https://www.w3.org/TR/webgpu/#bgra8unorm-storage)
	 * 
	 */
	BGRA8UnormStorage(13u),
	/**
	 * The `Float32Filterable` feature indicates support for filtering textures with 32-bit floating-point precision. This is useful for high-quality texture sampling in shaders.
	 * 
	 * [See W3C specification: Float32 Filterable](https://www.w3.org/TR/webgpu/#float32-filterable)
	 * 
	 */
	Float32Filterable(14u),
	/**
	 * The `Float32Blendable` feature indicates support for blending textures with 32-bit floating-point precision. This is useful for high-quality blending operations in shaders.
	 * 
	 * [See W3C specification: Float32 Blendable](https://www.w3.org/TR/webgpu/#float32-blendable)
	 * 
	 */
	Float32Blendable(15u),
	/**
	 * The `ClipDistances` feature allows the use of clip distances in WebGPU. This can be used to perform custom clipping operations in shaders, which is useful for advanced rendering techniques.
	 * 
	 * [See W3C specification: Clip Distances](https://www.w3.org/TR/webgpu/#clip-distances)
	 * 
	 */
	ClipDistances(16u),
	/**
	 * The `DualSourceBlending` feature indicates support for dual-source blending. This allows for more complex blending operations, which can be useful for advanced rendering techniques.
	 * 
	 * [See W3C specification: Dual Source Blending](https://www.w3.org/TR/webgpu/#dual-source-blending)
	 * 
	 */
	DualSourceBlending(17u),
	Subgroups(18u),
	TextureFormatsTier1(19u),
	TextureFormatsTier2(20u),
	PrimitiveIndex(21u),
	TextureComponentSwizzle(22u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUFeatureName? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the filtering mode used for sampling textures. This enum defines how texture coordinates map to texel values.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpufiltermode).
 * 
 */
actual enum class GPUFilterMode(val value: UInt) {
	/**
	 * Specifies that the nearest texture sample should be returned. This mode is useful for pixelated or blocky textures, as it does not interpolate between texels.
	 * 
	 * [W3C Specification](https://www.w3.org/TR/webgpu/#enumdef-gpufiltermode-nearest)
	 * 
	 */
	Nearest(1u),
	/**
	 * Specifies that a linear interpolation should be performed between the nearest texture samples. This mode is useful for smooth textures, as it interpolates between texels.
	 * 
	 * [W3C Specification](https://www.w3.org/TR/webgpu/#enumdef-gpufiltermode-linear)
	 * 
	 */
	Linear(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUFilterMode? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUFrontFace` enum defines which polygons are considered front-facing by a [GPURenderPipeline](https://www.w3.org/TR/webgpu/#gpurenderpipeline). This is crucial for determining the visibility of polygons during rendering. For more details, refer to the [Polygon Rasterization section](https://www.w3.org/TR/webgpu/#polygon-rasterization) of the WebGPU specification.
 * 
 * **Enum Values:**
 * - `CCW`: Counter-clockwise winding order.
 * - `CW`: Clockwise winding order.
 * 
 */
actual enum class GPUFrontFace(val value: UInt) {
	/**
	 * Specifies that polygons with vertices whose framebuffer coordinates are given in counter-clockwise order are considered front-facing. This is useful for determining the visibility of polygons during rendering.
	 * 
	 */
	CCW(1u),
	/**
	 * Specifies that polygons with vertices whose framebuffer coordinates are given in clockwise order are considered front-facing. This is useful for determining the visibility of polygons during rendering.
	 * 
	 */
	CW(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUFrontFace? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the format of index data used in WebGPU. This enum defines two possible formats for indices: `Uint16` and `Uint32`. These formats specify whether the indices are stored as 16-bit or 32-bit unsigned integers.
 * 
 * For more details, refer to the [WebGPU specification on GPUIndexFormat](https://www.w3.org/TR/webgpu/#enumdef-gpuindexformat).
 * 
 */
actual enum class GPUIndexFormat(val value: UInt) {
	/**
	 * Represents the `uint16` index format. This enum value specifies that indices in an index buffer are stored as 16-bit unsigned integers.
	 * 
	 * **See also:**
	 * - [W3C GPUIndexFormat Specification](https://www.w3.org/TR/webgpu/#enumdef-gpuindexformat)
	 * 
	 */
	Uint16(1u),
	/**
	 * Represents the `uint32` index format. This enum value specifies that indices in an index buffer are stored as 32-bit unsigned integers.
	 * 
	 * **See also:**
	 * - [W3C GPUIndexFormat Specification](https://www.w3.org/TR/webgpu/#enumdef-gpuindexformat)
	 * 
	 */
	Uint32(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUIndexFormat? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the operations that can be performed to load values into an attachment during a render pass.
 * 
 * This enum defines two possible operations: `Load` and `Clear`. These operations determine how the initial value for an attachment is handled at the beginning of a render pass. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpuloadop).
 * 
 * **See also:**
 * - [GPULoadOp.Load]
 * - [GPULoadOp.Clear]
 * 
 */
actual enum class GPULoadOp(val value: UInt) {
	/**
	 * Loads the existing value for this attachment into the render pass.
	 * 
	 * This operation is used when you want to preserve the current contents of the attachment and use it as the starting point for the render pass. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuloadop-load).
	 * 
	 */
	Load(1u),
	/**
	 * Loads a clear value for this attachment into the render pass.
	 * 
	 * This operation is used when you want to start with a cleared (typically zeroed or black) value for the attachment. On some GPU hardware, particularly mobile devices, using `Clear` can be more efficient because it avoids loading data from main memory into tile-local memory. For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#dom-gpuloadop-clear).
	 * 
	 * **Note:** It is recommended to use `Clear` in cases where the initial value doesn't matter, such as when the render target will be cleared using a skybox.
	 * 
	 */
	Clear(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPULoadOp? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the filtering mode used for mipmapping in WebGPU. This enum defines two possible values: `Nearest` and `Linear`.
 * 
 * For more details, refer to the [WebGPU specification on GPUMipmapFilterMode](https://www.w3.org/TR/webgpu/#enumdef-gpumipmapfiltermode).
 * 
 */
actual enum class GPUMipmapFilterMode(val value: UInt) {
	/**
	 * Specifies that the nearest mipmap level should be used for filtering. This mode is faster but can result in more visible artifacts, especially when viewing textures from a distance.
	 * 
	 * [W3C Specification](https://www.w3.org/TR/webgpu/#enumdef-gpumipmapfiltermode)
	 * 
	 */
	Nearest(1u),
	/**
	 * Specifies that linear interpolation should be used between the two nearest mipmap levels for filtering. This mode provides smoother visuals but is computationally more expensive than [Nearest].
	 * 
	 * [W3C Specification](https://www.w3.org/TR/webgpu/#enumdef-gpumipmapfiltermode)
	 * 
	 */
	Linear(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUMipmapFilterMode? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the power preference for GPU operations. This enum is used to specify whether the application prefers low power consumption or high performance.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpupowerpreference).
 * 
 */
actual enum class GPUPowerPreference(val value: UInt) {
	/**
	 * Represents the `low-power` preference for GPU power management. This value indicates that the application prefers to prioritize power efficiency over performance.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUPowerPreference](https://www.w3.org/TR/webgpu/#enumdef-gpupowerpreference)
	 * 
	 */
	LowPower(1u),
	/**
	 * Represents the `high-performance` preference for GPU power management. This value indicates that the application prefers to prioritize performance over power efficiency.
	 * 
	 * **See also:**
	 * - [WebGPU Specification: GPUPowerPreference](https://www.w3.org/TR/webgpu/#enumdef-gpupowerpreference)
	 * 
	 */
	HighPerformance(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUPowerPreference? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUPrimitiveTopology` enum defines the types of primitives that can be used in draw calls made with a [GPURenderPipeline](https://www.w3.org/TR/webgpu/#gpurenderpipeline). This enumeration specifies how vertices are interpreted to form geometric primitives during rendering. For more details, see the [Rasterization section](https://www.w3.org/TR/webgpu/#rasterization) of the WebGPU specification.
 * 
 */
actual enum class GPUPrimitiveTopology(val value: UInt) {
	/**
	 * Each vertex defines a point primitive. This is useful for rendering individual points, such as particles or markers.
	 * 
	 */
	PointList(1u),
	/**
	 * Each consecutive pair of two vertices defines a line primitive. This is useful for rendering lines, such as wireframes or outlines.
	 * 
	 */
	LineList(2u),
	/**
	 * Each vertex after the first defines a line primitive between it and the previous vertex. This is useful for rendering connected lines, such as polylines.
	 * 
	 */
	LineStrip(3u),
	/**
	 * Each consecutive triplet of three vertices defines a triangle primitive. This is the most common topology for rendering 3D models and scenes.
	 * 
	 */
	TriangleList(4u),
	/**
	 * Each vertex after the first two defines a triangle primitive between it and the previous two vertices. This is useful for rendering connected triangles, such as in terrain or mesh rendering.
	 * 
	 */
	TriangleStrip(5u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUPrimitiveTopology? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the type of query that can be performed using the WebGPU API. This enum defines two types of queries: occlusion and timestamp.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpuquerytype).
 * 
 */
actual enum class GPUQueryType(val value: UInt) {
	/**
	 * Represents the occlusion query type. This enum value is used to perform occlusion queries, which determine whether objects are visible from a particular viewpoint.
	 * 
	 * **See also:**
	 * - [W3C WebGPU Specification: GPUQueryType](https://www.w3.org/TR/webgpu/#enumdef-gpuquerytype)
	 * 
	 */
	Occlusion(1u),
	/**
	 * Represents the timestamp query type. This enum value is used to perform timestamp queries, which capture the time at a specific point in the rendering pipeline.
	 * 
	 * **See also:**
	 * - [W3C WebGPU Specification: GPUQueryType](https://www.w3.org/TR/webgpu/#enumdef-gpuquerytype)
	 * 
	 */
	Timestamp(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUQueryType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the type of sampler binding used in WebGPU. This enum defines how textures are sampled when bound to a pipeline.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpusamplerbindingtype).
 * 
 */
actual enum class GPUSamplerBindingType(val value: UInt) {
	/**
	 * Indicates that no sampler binding is used. This can be useful for bindings that do not require sampling operations.
	 * 
	 */
	BindingNotUsed(0u),
	/**
	 * Specifies that filtering is applied when sampling textures. This is typically used for mipmapping and anisotropic filtering.
	 * 
	 */
	Filtering(2u),
	/**
	 * Indicates that no filtering is applied when sampling textures. This can be used for performance-critical applications where filtering is not required.
	 * 
	 */
	NonFiltering(3u),
	/**
	 * Specifies that comparison sampling is used. This is typically employed for depth textures and shadow mapping.
	 * 
	 */
	Comparison(4u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUSamplerBindingType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the operations that can be performed on the stencil buffer during rendering. This enum is used to specify how the stencil values are modified in a [render pass](https://www.w3.org/TR/webgpu/#dom-gpurenderpass).
 * 
 * The `GPUStencilOperation` enum defines several constants, each representing a different operation that can be applied to the stencil buffer. These operations control how the stencil test affects the stencil values during rendering.
 * 
 * **See also:**
 * - [WebGPU Specification: GPUStencilOperation](https://www.w3.org/TR/webgpu/#enumdef-gpustenciloperation)
 * 
 */
actual enum class GPUStencilOperation(val value: UInt) {
	/**
	 * Keeps the current stencil value unchanged. This operation does not modify the stencil buffer.
	 * 
	 */
	Keep(1u),
	/**
	 * Sets the stencil value to `0`. This operation replaces the current stencil value with zero.
	 * 
	 */
	Zero(2u),
	/**
	 * Sets the stencil value to the reference value specified in the [render state](https://www.w3.org/TR/webgpu/#dom-renderstate-stencilreference-slot). This operation replaces the current stencil value with the reference value.
	 * 
	 */
	Replace(3u),
	/**
	 * Bitwise-inverts the current stencil value. This operation flips all the bits of the current stencil value.
	 * 
	 */
	Invert(4u),
	/**
	 * Increments the current stencil value, clamping it to the maximum representable value of the depth-stencil attachment's stencil aspect. This operation increases the stencil value but ensures it does not exceed the maximum value.
	 * 
	 */
	IncrementClamp(5u),
	/**
	 * Decrements the current stencil value, clamping it to `0`. This operation decreases the stencil value but ensures it does not go below zero.
	 * 
	 */
	DecrementClamp(6u),
	/**
	 * Increments the current stencil value, wrapping it to zero if it exceeds the maximum representable value of the depth-stencil attachment's stencil aspect. This operation increases the stencil value and wraps around to zero if necessary.
	 * 
	 */
	IncrementWrap(7u),
	/**
	 * Decrements the current stencil value, wrapping it to the maximum representable value of the depth-stencil attachment's stencil aspect if it goes below `0`. This operation decreases the stencil value and wraps around to the maximum value if necessary.
	 * 
	 */
	DecrementWrap(8u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUStencilOperation? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the access mode for a storage texture binding, indicating whether the texture can be read from, written to, or both. This enum is used to specify the intended usage of a texture in a GPU pipeline.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpustoragetextureaccess).
 * 
 */
actual enum class GPUStorageTextureAccess(val value: UInt) {
	/**
	 * Indicates that the binding is not used. This value can be used when a texture binding is intentionally left unused in a pipeline.
	 * 
	 */
	BindingNotUsed(0u),
	/**
	 * Specifies that the texture can only be written to. This mode is useful for scenarios where the texture is used as an output target, such as render targets or storage textures.
	 * 
	 */
	WriteOnly(2u),
	/**
	 * Specifies that the texture can only be read from. This mode is suitable for textures that are used as input sources, such as samplers or storage textures that are read by shaders.
	 * 
	 */
	ReadOnly(3u),
	/**
	 * Specifies that the texture can be both read from and written to. This mode is used for textures that need to be updated and read within the same pipeline stage.
	 * 
	 */
	ReadWrite(4u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUStorageTextureAccess? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUStoreOp` enum defines operations that specify how to handle the resulting value of a render pass for an attachment. This is used in conjunction with [GPUTextureView] and [GPURenderPassDescriptor] to control whether the rendered output should be stored or discarded.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpustoreop).
 * 
 */
actual enum class GPUStoreOp(val value: UInt) {
	/**
	 * Stores the resulting value of the render pass for this attachment. This operation ensures that the final rendered output is retained and can be used in subsequent operations.
	 * 
	 * [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpustoreop-store)
	 * 
	 */
	Store(1u),
	/**
	 * Discards the resulting value of the render pass for this attachment. This operation does not retain the final rendered output, and implementations are not required to perform a clear at the end of the render pass.
	 * 
	 * [W3C Specification](https://www.w3.org/TR/webgpu/#dom-gpustoreop-discard)
	 * 
	 */
	Discard(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUStoreOp? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the different aspects of a texture that can be accessed. This enum is used to specify which parts of a texture format are accessible when creating a [GPUTextureView](https://www.w3.org/TR/webgpu/#dom-gputextureview).
 * 
 * Each value in this enum corresponds to a set of aspects, defining what parts of the texture data can be accessed. This is crucial for operations that need to read from or write to specific components of a texture.
 * 
 * **See also:**
 * - [GPUTextureView](https://www.w3.org/TR/webgpu/#dom-gputextureview)
 * 
 */
actual enum class GPUTextureAspect(val value: UInt) {
	/**
	 * Specifies that all available aspects of the texture format will be accessible. For color formats, this includes the color aspect. For combined depth-stencil formats, both the depth and stencil aspects are accessible. Depth-or-stencil formats with a single aspect will only make that specific aspect accessible.
	 * 
	 * **Set of aspects:** [color](https://www.w3.org/TR/webgpu/#aspect-color), [depth](https://www.w3.org/TR/webgpu/#aspect-depth), [stencil](https://www.w3.org/TR/webgpu/#aspect-stencil)
	 * 
	 */
	All(1u),
	/**
	 * Specifies that only the stencil aspect of a depth-or-stencil format will be accessible. This is useful for operations that need to work exclusively with the stencil buffer.
	 * 
	 * **Set of aspects:** [stencil](https://www.w3.org/TR/webgpu/#aspect-stencil)
	 * 
	 */
	StencilOnly(2u),
	/**
	 * Specifies that only the depth aspect of a depth-or-stencil format will be accessible. This is useful for operations that need to work exclusively with the depth buffer.
	 * 
	 * **Set of aspects:** [depth](https://www.w3.org/TR/webgpu/#aspect-depth)
	 * 
	 */
	DepthOnly(3u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUTextureAspect? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the dimensionality of a texture in WebGPU. This enum defines three possible dimensions for textures: one-dimensional, two-dimensional, and three-dimensional.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gputexturedimension).
 * 
 */
actual enum class GPUTextureDimension(val value: UInt) {
	/**
	 * Specifies a texture that has one dimension, width. "OneD" textures cannot have mipmaps, be multisampled, use compressed or depth/stencil formats, or be used as a render target.
	 * 
	 */
	OneD(1u),
	/**
	 * Specifies a texture that has a width and height, and may have layers.
	 * 
	 */
	TwoD(2u),
	/**
	 * Specifies a texture that has a width, height, and depth. "ThreeD" textures cannot be multisampled, and their format must support 3D textures (all [plain color formats](https://www.w3.org/TR/webgpu/#plain-color-formats) and some [packed/compressed formats](https://www.w3.org/TR/webgpu/#packed-formats)).
	 * 
	 */
	ThreeD(3u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUTextureDimension? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUTextureFormat` enum defines various texture formats that can be used with the WebGPU API. Each format specifies how pixel data is stored and interpreted, including details such as color channels, bit depth, and compression methods.
 * 
 * For more information, refer to the [WebGPU specification on GPUTextureFormat](https://www.w3.org/TR/webgpu/#enumdef-gputextureformat).
 * 
 */
actual enum class GPUTextureFormat(val value: UInt) {
	/**
	 * Represents an 8-bit unsigned normalized format. Each pixel component is stored as an 8-bit value, where 0 represents the minimum value and 255 represents the maximum value.
	 * 
	 */
	R8Unorm(1u),
	/**
	 * Represents an 8-bit signed normalized format. Each pixel component is stored as an 8-bit value, where -128 represents the minimum value and 127 represents the maximum value.
	 * 
	 */
	R8Snorm(2u),
	/**
	 * Represents an 8-bit unsigned integer format. Each pixel component is stored as an 8-bit unsigned integer, ranging from 0 to 255.
	 * 
	 */
	R8Uint(3u),
	/**
	 * Represents an 8-bit signed integer format. Each pixel component is stored as an 8-bit signed integer.
	 * 
	 */
	R8Sint(4u),
	R16Unorm(5u),
	R16Snorm(6u),
	/**
	 * Represents a 16-bit unsigned integer format. Each pixel component is stored as a 16-bit unsigned integer.
	 * 
	 */
	R16Uint(7u),
	/**
	 * Represents a 16-bit signed integer format. Each pixel component is stored as a 16-bit signed integer.
	 * 
	 */
	R16Sint(8u),
	/**
	 * Represents a 16-bit floating-point format. Each pixel component is stored as a 16-bit floating-point value.
	 * 
	 */
	R16Float(9u),
	/**
	 * Represents an 8-bit unsigned normalized format for two channels (red and green). Each channel is stored as an 8-bit value, where 0 represents the minimum value and 255 represents the maximum value.
	 * 
	 */
	RG8Unorm(10u),
	/**
	 * Represents an 8-bit signed normalized format for two channels (red and green). Each channel is stored as an 8-bit value, where -128 represents the minimum value and 127 represents the maximum value.
	 * 
	 */
	RG8Snorm(11u),
	/**
	 * Represents an 8-bit unsigned integer format for two channels (red and green). Each channel is stored as an 8-bit unsigned integer, ranging from 0 to 255.
	 * 
	 */
	RG8Uint(12u),
	/**
	 * Represents an 8-bit signed integer format for two channels (red and green). Each channel is stored as an 8-bit signed integer.
	 * 
	 */
	RG8Sint(13u),
	/**
	 * Represents a 32-bit floating-point format. Each pixel component is stored as a 32-bit floating-point value.
	 * 
	 */
	R32Float(14u),
	/**
	 * Represents a 32-bit unsigned integer format. Each pixel component is stored as a 32-bit unsigned integer.
	 * 
	 */
	R32Uint(15u),
	/**
	 * Represents a 32-bit signed integer format. Each pixel component is stored as a 32-bit signed integer.
	 * 
	 */
	R32Sint(16u),
	RG16Unorm(17u),
	RG16Snorm(18u),
	/**
	 * Represents a 16-bit unsigned integer format for two channels (red and green). Each channel is stored as a 16-bit unsigned integer.
	 * 
	 */
	RG16Uint(19u),
	/**
	 * Represents a 16-bit signed integer format for two channels (red and green). Each channel is stored as a 16-bit signed integer.
	 * 
	 */
	RG16Sint(20u),
	/**
	 * Represents a 16-bit floating-point format for two channels (red and green). Each channel is stored as a 16-bit floating-point value.
	 * 
	 */
	RG16Float(21u),
	/**
	 * Represents an 8-bit unsigned normalized format for four channels (red, green, blue, alpha). Each channel is stored as an 8-bit value, where 0 represents the minimum value and 255 represents the maximum value.
	 * 
	 */
	RGBA8Unorm(22u),
	/**
	 * Represents an 8-bit unsigned normalized format for four channels (red, green, blue, alpha) in sRGB color space. Each channel is stored as an 8-bit value, where 0 represents the minimum value and 255 represents the maximum value.
	 * 
	 */
	RGBA8UnormSrgb(23u),
	/**
	 * Represents an 8-bit signed normalized format for four channels (red, green, blue, alpha). Each channel is stored as an 8-bit value, where -128 represents the minimum value and 127 represents the maximum value.
	 * 
	 */
	RGBA8Snorm(24u),
	/**
	 * Represents an 8-bit unsigned integer format for four channels (red, green, blue, alpha). Each channel is stored as an 8-bit unsigned integer, ranging from 0 to 255.
	 * 
	 */
	RGBA8Uint(25u),
	/**
	 * Represents an 8-bit signed integer format for four channels (red, green, blue, alpha). Each channel is stored as an 8-bit signed integer.
	 * 
	 */
	RGBA8Sint(26u),
	/**
	 * Represents an 8-bit unsigned normalized format for four channels (blue, green, red, alpha). Each channel is stored as an 8-bit value, where 0 represents the minimum value and 255 represents the maximum value.
	 * 
	 */
	BGRA8Unorm(27u),
	/**
	 * Represents an 8-bit unsigned normalized format for four channels (blue, green, red, alpha) in sRGB color space. Each channel is stored as an 8-bit value, where 0 represents the minimum value and 255 represents the maximum value.
	 * 
	 */
	BGRA8UnormSrgb(28u),
	/**
	 * Represents a packed 32-bit unsigned integer format for four channels (red, green, blue, alpha). The red, green, and blue channels are each 10 bits, and the alpha channel is 2 bits.
	 * 
	 */
	RGB10A2Uint(29u),
	/**
	 * Represents a packed 32-bit unsigned normalized format for four channels (red, green, blue, alpha). The red, green, and blue channels are each 10 bits, and the alpha channel is 2 bits.
	 * 
	 */
	RGB10A2Unorm(30u),
	/**
	 * Represents a packed 32-bit unsigned floating-point format for three channels (red, green, blue). The red and green channels are each 11 bits, and the blue channel is 10 bits.
	 * 
	 */
	RG11B10Ufloat(31u),
	/**
	 * Represents a packed 32-bit unsigned floating-point format for three channels (red, green, blue) with a shared exponent. The red, green, and blue channels are each 9 bits, and the shared exponent is 5 bits.
	 * 
	 */
	RGB9E5Ufloat(32u),
	/**
	 * Represents a 64-bit floating-point format for two channels (red, green). Each channel is stored as a 32-bit floating-point value.
	 * 
	 */
	RG32Float(33u),
	/**
	 * Represents a 64-bit unsigned integer format for two channels (red, green). Each channel is stored as a 32-bit unsigned integer.
	 * 
	 */
	RG32Uint(34u),
	/**
	 * Represents a 64-bit signed integer format for two channels (red, green). Each channel is stored as a 32-bit signed integer.
	 * 
	 */
	RG32Sint(35u),
	RGBA16Unorm(36u),
	RGBA16Snorm(37u),
	/**
	 * Represents a 64-bit unsigned integer format for four channels (red, green, blue, alpha). Each channel is stored as a 16-bit unsigned integer.
	 * 
	 */
	RGBA16Uint(38u),
	/**
	 * Represents a 64-bit signed integer format for four channels (red, green, blue, alpha). Each channel is stored as a 16-bit signed integer.
	 * 
	 */
	RGBA16Sint(39u),
	/**
	 * Represents a 64-bit floating-point format for four channels (red, green, blue, alpha). Each channel is stored as a 16-bit floating-point value.
	 * 
	 */
	RGBA16Float(40u),
	/**
	 * Represents a 128-bit floating-point format for four channels (red, green, blue, alpha). Each channel is stored as a 32-bit floating-point value.
	 * 
	 */
	RGBA32Float(41u),
	/**
	 * Represents a 128-bit unsigned integer format for four channels (red, green, blue, alpha). Each channel is stored as a 32-bit unsigned integer.
	 * 
	 */
	RGBA32Uint(42u),
	/**
	 * Represents a 128-bit signed integer format for four channels (red, green, blue, alpha). Each channel is stored as a 32-bit signed integer.
	 * 
	 */
	RGBA32Sint(43u),
	/**
	 * Represents an 8-bit stencil format. This format is used for storing stencil values in depth-stencil textures.
	 * 
	 */
	Stencil8(44u),
	/**
	 * Represents a 16-bit unsigned normalized depth format. This format is used for storing depth values in depth textures.
	 * 
	 */
	Depth16Unorm(45u),
	/**
	 * Represents a 24-bit or 32-bit unsigned normalized depth format. This format is used for storing depth values in depth textures.
	 * 
	 */
	Depth24Plus(46u),
	/**
	 * Represents a combined 24-bit or 32-bit unsigned normalized depth and 8-bit stencil format. This format is used for storing both depth and stencil values in depth-stencil textures.
	 * 
	 */
	Depth24PlusStencil8(47u),
	/**
	 * Represents a 32-bit floating-point depth format. This format is used for storing high-precision depth values in depth textures.
	 * 
	 */
	Depth32Float(48u),
	/**
	 * Represents a combined 32-bit floating-point depth and 8-bit stencil format. This format is used for storing both high-precision depth and stencil values in depth-stencil textures.
	 * 
	 */
	Depth32FloatStencil8(49u),
	/**
	 * Represents a BC1 (DXT1) compressed format for RGBA textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC1RGBAUnorm(50u),
	/**
	 * Represents a BC1 (DXT1) compressed format for RGBA textures with unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC1RGBAUnormSrgb(51u),
	/**
	 * Represents a BC2 (DXT3) compressed format for RGBA textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC2RGBAUnorm(52u),
	/**
	 * Represents a BC2 (DXT3) compressed format for RGBA textures with unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC2RGBAUnormSrgb(53u),
	/**
	 * Represents a BC3 (DXT5) compressed format for RGBA textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC3RGBAUnorm(54u),
	/**
	 * Represents a BC3 (DXT5) compressed format for RGBA textures with unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC3RGBAUnormSrgb(55u),
	/**
	 * Represents a BC4 compressed format for R textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC4RUnorm(56u),
	/**
	 * Represents a BC4 compressed format for R textures with signed normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC4RSnorm(57u),
	/**
	 * Represents a BC5 compressed format for RG textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC5RGUnorm(58u),
	/**
	 * Represents a BC5 compressed format for RG textures with signed normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC5RGSnorm(59u),
	/**
	 * Represents a BC6H compressed format for RGB textures with unsigned floating-point values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC6HRGBUfloat(60u),
	/**
	 * Represents a BC6H compressed format for RGB textures with floating-point values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC6HRGBFloat(61u),
	/**
	 * Represents a BC7 compressed format for RGBA textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC7RGBAUnorm(62u),
	/**
	 * Represents a BC7 compressed format for RGBA textures with unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	BC7RGBAUnormSrgb(63u),
	/**
	 * Represents an ETC2 compressed format for RGB textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ETC2RGB8Unorm(64u),
	/**
	 * Represents an ETC2 compressed format for RGB textures with unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ETC2RGB8UnormSrgb(65u),
	/**
	 * Represents an ETC2 compressed format for RGB textures with 1-bit alpha channel and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ETC2RGB8A1Unorm(66u),
	/**
	 * Represents an ETC2 compressed format for RGB textures with 1-bit alpha channel and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ETC2RGB8A1UnormSrgb(67u),
	/**
	 * Represents an ETC2 compressed format for RGBA textures with unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ETC2RGBA8Unorm(68u),
	/**
	 * Represents an ETC2 compressed format for RGBA textures with unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ETC2RGBA8UnormSrgb(69u),
	/**
	 * Represents an EAC compressed format for R textures with 11-bit unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	EACR11Unorm(70u),
	/**
	 * Represents an EAC compressed format for R textures with 11-bit signed normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	EACR11Snorm(71u),
	/**
	 * Represents an EAC compressed format for RG textures with 11-bit unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	EACRG11Unorm(72u),
	/**
	 * Represents an EAC compressed format for RG textures with 11-bit signed normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	EACRG11Snorm(73u),
	/**
	 * Represents an ASTC compressed format for textures with 4x4 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC4x4Unorm(74u),
	/**
	 * Represents an ASTC compressed format for textures with 4x4 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC4x4UnormSrgb(75u),
	/**
	 * Represents an ASTC compressed format for textures with 5x4 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC5x4Unorm(76u),
	/**
	 * Represents an ASTC compressed format for textures with 5x4 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC5x4UnormSrgb(77u),
	/**
	 * Represents an ASTC compressed format for textures with 5x5 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC5x5Unorm(78u),
	/**
	 * Represents an ASTC compressed format for textures with 5x5 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC5x5UnormSrgb(79u),
	/**
	 * Represents an ASTC compressed format for textures with 6x5 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC6x5Unorm(80u),
	/**
	 * Represents an ASTC compressed format for textures with 6x5 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC6x5UnormSrgb(81u),
	/**
	 * Represents an ASTC compressed format for textures with 6x6 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC6x6Unorm(82u),
	/**
	 * Represents an ASTC compressed format for textures with 6x6 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC6x6UnormSrgb(83u),
	/**
	 * Represents an ASTC compressed format for textures with 8x5 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC8x5Unorm(84u),
	/**
	 * Represents an ASTC compressed format for textures with 8x5 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC8x5UnormSrgb(85u),
	/**
	 * Represents an ASTC compressed format for textures with 8x6 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC8x6Unorm(86u),
	/**
	 * Represents an ASTC compressed format for textures with 8x6 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC8x6UnormSrgb(87u),
	/**
	 * Represents an ASTC compressed format for textures with 8x8 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC8x8Unorm(88u),
	/**
	 * Represents an ASTC compressed format for textures with 8x8 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC8x8UnormSrgb(89u),
	/**
	 * Represents an ASTC compressed format for textures with 10x5 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x5Unorm(90u),
	/**
	 * Represents an ASTC compressed format for textures with 10x5 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x5UnormSrgb(91u),
	/**
	 * Represents an ASTC compressed format for textures with 10x6 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x6Unorm(92u),
	/**
	 * Represents an ASTC compressed format for textures with 10x6 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x6UnormSrgb(93u),
	/**
	 * Represents an ASTC compressed format for textures with 10x8 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x8Unorm(94u),
	/**
	 * Represents an ASTC compressed format for textures with 10x8 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x8UnormSrgb(95u),
	/**
	 * Represents an ASTC compressed format for textures with 10x10 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x10Unorm(96u),
	/**
	 * Represents an ASTC compressed format for textures with 10x10 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC10x10UnormSrgb(97u),
	/**
	 * Represents an ASTC compressed format for textures with 12x10 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC12x10Unorm(98u),
	/**
	 * Represents an ASTC compressed format for textures with 12x10 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC12x10UnormSrgb(99u),
	/**
	 * Represents an ASTC compressed format for textures with 12x12 block size and unsigned normalized values. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC12x12Unorm(100u),
	/**
	 * Represents an ASTC compressed format for textures with 12x12 block size and unsigned normalized values in sRGB color space. This format is used for texture compression in WebGPU.
	 * 
	 */
	ASTC12x12UnormSrgb(101u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUTextureFormat? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the sample type for textures in WebGPU. This enum defines the possible formats that a texture can have, which determines how the texture data is sampled and interpreted.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gputexturesampletype).
 * 
 */
actual enum class GPUTextureSampleType(val value: UInt) {
	/**
	 * Indicates that the binding is not used. This value is typically used when a texture binding is not required for a particular operation.
	 * 
	 */
	BindingNotUsed(0u),
	/**
	 * Specifies that the texture samples are in floating-point format. This is commonly used for high dynamic range (HDR) rendering and other effects that require precise color representation.
	 * 
	 */
	Float(2u),
	/**
	 * Specifies that the texture samples are in an unfilterable floating-point format. This format is used when the texture data should not be filtered (e.g., for depth textures or other specialized uses).
	 * 
	 */
	UnfilterableFloat(3u),
	/**
	 * Specifies that the texture samples are in depth format. Depth textures are used for depth buffering and shadow mapping, where the depth information is stored in the texture.
	 * 
	 */
	Depth(4u),
	/**
	 * Specifies that the texture samples are in signed integer format. This format is used for textures that store signed integer data, such as normal maps or other specialized textures.
	 * 
	 */
	Sint(5u),
	/**
	 * Specifies that the texture samples are in unsigned integer format. This format is used for textures that store unsigned integer data, such as stencil buffers or other specialized textures.
	 * 
	 */
	Uint(6u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUTextureSampleType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Represents the dimensionality of a texture view in WebGPU. This enum defines how a texture is viewed, which affects the corresponding WGSL types and sampling behavior.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
 * 
 */
actual enum class GPUTextureViewDimension(val value: UInt) {
	/**
	 * Represents a texture view dimension where the texture is viewed as a 1-dimensional image.
	 * 
	 * This dimension corresponds to the following WGSL types:
	 * - `texture_1d`
	 * - `texture_storage_1d`
	 * 
	 * [See the official WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	OneD(1u),
	/**
	 * Represents a texture view dimension where the texture is viewed as a single 2-dimensional image.
	 * 
	 * This dimension corresponds to the following WGSL types:
	 * - `texture_2d`
	 * - `texture_storage_2d`
	 * - `texture_multisampled_2d`
	 * - `texture_depth_2d`
	 * - `texture_depth_multisampled_2d`
	 * 
	 * [See the official WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	TwoD(2u),
	/**
	 * Represents a texture view dimension where the texture is viewed as an array of 2-dimensional images.
	 * 
	 * This dimension corresponds to the following WGSL types:
	 * - `texture_2d_array`
	 * - `texture_storage_2d_array`
	 * - `texture_depth_2d_array`
	 * 
	 * [See the official WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	TwoDArray(3u),
	/**
	 * Represents a texture view dimension where the texture is viewed as a cubemap.
	 * 
	 * The view has 6 array layers, each corresponding to a face of the cube in the order `[+X, -X, +Y, -Y, +Z, -Z]`. Sampling is done seamlessly across the faces of the cubemap.
	 * 
	 * This dimension corresponds to the following WGSL types:
	 * - `texture_cube`
	 * - `texture_depth_cube`
	 * 
	 * [See the official WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	Cube(4u),
	/**
	 * Represents a texture view dimension where the texture is viewed as a packed array of `n` cubemaps, each with 6 array layers.
	 * 
	 * This dimension corresponds to the following WGSL types:
	 * - `texture_cube_array`
	 * - `texture_depth_cube_array`
	 * 
	 * [See the official WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	CubeArray(5u),
	/**
	 * Represents a texture view dimension where the texture is viewed as a 3-dimensional image.
	 * 
	 * This dimension corresponds to the following WGSL types:
	 * - `texture_3d`
	 * - `texture_storage_3d`
	 * 
	 * [See the official WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gputextureviewdimension).
	 * 
	 */
	ThreeD(6u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUTextureViewDimension? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUVertexFormat` enum defines the possible formats for vertex attributes in WebGPU. Each format specifies the data type, number of components, and byte size of the vertex attribute. This enumeration is crucial for configuring vertex buffers and ensuring compatibility with shader programs.
 * 
 * For more details, refer to the [WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
 * 
 */
actual enum class GPUVertexFormat(val value: UInt) {
	/**
	 * Represents a single unsigned 8-bit integer component. This format is useful for vertex attributes that require compact storage and minimal precision.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 1
	 * **Byte Size:** 1 byte
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint8(1u),
	/**
	 * Represents two unsigned 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 2
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint8x2(2u),
	/**
	 * Represents four unsigned 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as color values.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 4
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint8x4(3u),
	/**
	 * Represents a single signed 8-bit integer component. This format is useful for vertex attributes that require compact storage and minimal precision.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 1
	 * **Byte Size:** 1 byte
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint8(4u),
	/**
	 * Represents two signed 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as texture coordinates.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 2
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint8x2(5u),
	/**
	 * Represents four signed 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as color values.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 4
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint8x4(6u),
	/**
	 * Represents a single unsigned normalized 8-bit integer component. This format is useful for vertex attributes that require compact storage and minimal precision, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 1
	 * **Byte Size:** 1 byte
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm8(7u),
	/**
	 * Represents two unsigned normalized 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 2
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm8x2(8u),
	/**
	 * Represents four unsigned normalized 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as color values.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 4
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm8x4(9u),
	/**
	 * Represents a single signed normalized 8-bit integer component. This format is useful for vertex attributes that require compact storage and minimal precision, such as texture coordinates.
	 * 
	 * **Data Type:** Signed normalized
	 * **Components:** 1
	 * **Byte Size:** 1 byte
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Snorm8(10u),
	/**
	 * Represents two signed normalized 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as texture coordinates.
	 * 
	 * **Data Type:** Signed normalized
	 * **Components:** 2
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Snorm8x2(11u),
	/**
	 * Represents four signed normalized 8-bit integer components. This format is useful for vertex attributes that require compact storage and minimal precision, such as color values.
	 * 
	 * **Data Type:** Signed normalized
	 * **Components:** 4
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Snorm8x4(12u),
	/**
	 * Represents a single unsigned 16-bit integer component. This format is useful for vertex attributes that require moderate precision and storage.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 1
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint16(13u),
	/**
	 * Represents two unsigned 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 2
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint16x2(14u),
	/**
	 * Represents four unsigned 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as color values.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 4
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint16x4(15u),
	/**
	 * Represents a single signed 16-bit integer component. This format is useful for vertex attributes that require moderate precision and storage.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 1
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint16(16u),
	/**
	 * Represents two signed 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 2
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint16x2(17u),
	/**
	 * Represents four signed 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as color values.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 4
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint16x4(18u),
	/**
	 * Represents a single unsigned normalized 16-bit integer component. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 1
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm16(19u),
	/**
	 * Represents two unsigned normalized 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 2
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm16x2(20u),
	/**
	 * Represents four unsigned normalized 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as color values.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 4
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm16x4(21u),
	/**
	 * Represents a single signed normalized 16-bit integer component. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Signed normalized
	 * **Components:** 1
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Snorm16(22u),
	/**
	 * Represents two signed normalized 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Signed normalized
	 * **Components:** 2
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Snorm16x2(23u),
	/**
	 * Represents four signed normalized 16-bit integer components. This format is useful for vertex attributes that require moderate precision and storage, such as color values.
	 * 
	 * **Data Type:** Signed normalized
	 * **Components:** 4
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Snorm16x4(24u),
	/**
	 * Represents a single 16-bit floating-point component. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Float
	 * **Components:** 1
	 * **Byte Size:** 2 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Float16(25u),
	/**
	 * Represents two 16-bit floating-point components. This format is useful for vertex attributes that require moderate precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Float
	 * **Components:** 2
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Float16x2(26u),
	/**
	 * Represents four 16-bit floating-point components. This format is useful for vertex attributes that require moderate precision and storage, such as color values.
	 * 
	 * **Data Type:** Float
	 * **Components:** 4
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Float16x4(27u),
	/**
	 * Represents a single 32-bit floating-point component. This format is useful for vertex attributes that require high precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Float
	 * **Components:** 1
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Float32(28u),
	/**
	 * Represents two 32-bit floating-point components. This format is useful for vertex attributes that require high precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Float
	 * **Components:** 2
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Float32x2(29u),
	/**
	 * Represents three 32-bit floating-point components. This format is useful for vertex attributes that require high precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Float
	 * **Components:** 3
	 * **Byte Size:** 12 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Float32x3(30u),
	/**
	 * Represents four 32-bit floating-point components. This format is useful for vertex attributes that require high precision and storage, such as color values.
	 * 
	 * **Data Type:** Float
	 * **Components:** 4
	 * **Byte Size:** 16 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Float32x4(31u),
	/**
	 * Represents a single 32-bit unsigned integer component. This format is useful for vertex attributes that require high precision and storage, such as indices.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 1
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint32(32u),
	/**
	 * Represents two 32-bit unsigned integer components. This format is useful for vertex attributes that require high precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 2
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint32x2(33u),
	/**
	 * Represents three 32-bit unsigned integer components. This format is useful for vertex attributes that require high precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 3
	 * **Byte Size:** 12 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint32x3(34u),
	/**
	 * Represents four 32-bit unsigned integer components. This format is useful for vertex attributes that require high precision and storage, such as color values.
	 * 
	 * **Data Type:** Unsigned int
	 * **Components:** 4
	 * **Byte Size:** 16 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Uint32x4(35u),
	/**
	 * Represents a single 32-bit signed integer component. This format is useful for vertex attributes that require high precision and storage, such as indices.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 1
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint32(36u),
	/**
	 * Represents two 32-bit signed integer components. This format is useful for vertex attributes that require high precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 2
	 * **Byte Size:** 8 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint32x2(37u),
	/**
	 * Represents three 32-bit signed integer components. This format is useful for vertex attributes that require high precision and storage, such as texture coordinates.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 3
	 * **Byte Size:** 12 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint32x3(38u),
	/**
	 * Represents four 32-bit signed integer components. This format is useful for vertex attributes that require high precision and storage, such as color values.
	 * 
	 * **Data Type:** Signed int
	 * **Components:** 4
	 * **Byte Size:** 16 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Sint32x4(39u),
	/**
	 * Represents a 10-10-10-2 unsigned normalized component. This format is useful for vertex attributes that require specific precision and storage, such as color values.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 4
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm1010102(40u),
	/**
	 * Represents a 8-bit unsigned normalized component in BGRA order. This format is useful for vertex attributes that require specific precision and storage, such as color values.
	 * 
	 * **Data Type:** Unsigned normalized
	 * **Components:** 4
	 * **Byte Size:** 4 bytes
	 * 
	 * [See the WebGPU specification for more details](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexformat).
	 * 
	 */
	Unorm8x4BGRA(41u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUVertexFormat? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The `GPUVertexStepMode` enum defines the step mode that configures how an address for vertex buffer data is computed, based on the current vertex or instance index.
 * 
 * This enumeration is used to specify whether the vertex buffer data should be stepped per vertex or per instance. It is crucial for configuring vertex input in GPU rendering pipelines.
 * 
 * For more details, refer to the [W3C WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpuvertexstepmode).
 * 
 */
actual enum class GPUVertexStepMode(val value: UInt) {
	/**
	 * The address is advanced by `arrayStride` for each vertex, and reset between instances.
	 * 
	 * This mode is used when the vertex data should be stepped per vertex. It is useful in scenarios where each vertex in a mesh has its own set of attributes.
	 * 
	 */
	Vertex(1u),
	/**
	 * The address is advanced by `arrayStride` for each instance.
	 * 
	 * This mode is used when the vertex data should be stepped per instance. It is useful in scenarios where multiple instances of a mesh share the same vertex data but have different transformations or attributes.
	 * 
	 */
	Instance(2u);


	companion object {
		/**
		 * Retrieves the corresponding [UInt] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [UInt] or `null` if no match is found.
		 */
		fun of(value: UInt): GPUVertexStepMode? {
			return entries.find { it.value == value }
		}
    }

}
