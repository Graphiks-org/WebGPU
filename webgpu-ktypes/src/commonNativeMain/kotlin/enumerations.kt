@file:Suppress("unused")
// This file has been generated DO NO EDIT
package io.ygdrasil.webgpu

/**
 * Selects how texture coordinates outside the normalized range are addressed.
 *
 */
actual enum class GPUAddressMode(val value: UInt) {
	ClampToEdge(1u),
	Repeat(2u),
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
 * Defines the source or destination factor used to scale color and alpha values during blending.
 *
 */
actual enum class GPUBlendFactor(val value: UInt) {
	Zero(1u),
	One(2u),
	Src(3u),
	OneMinusSrc(4u),
	SrcAlpha(5u),
	OneMinusSrcAlpha(6u),
	Dst(7u),
	OneMinusDst(8u),
	DstAlpha(9u),
	OneMinusDstAlpha(10u),
	SrcAlphaSaturated(11u),
	Constant(12u),
	OneMinusConstant(13u),
	Src1(14u),
	OneMinusSrc1(15u),
	Src1Alpha(16u),
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
 * Defines the operation used to combine source and destination blend factors.
 *
 */
actual enum class GPUBlendOperation(val value: UInt) {
	Add(1u),
	Subtract(2u),
	ReverseSubtract(3u),
	Min(4u),
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
 * Selects how a buffer binding is accessed by shaders.
 *
 */
actual enum class GPUBufferBindingType(val value: UInt) {
	BindingNotUsed(0u),
	Uniform(2u),
	Storage(3u),
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
 * The mapping state of a GPUBuffer: mapped or unmapped.
 *
 */
actual enum class GPUBufferMapState(val value: UInt) {
	Unmapped(1u),
	Pending(2u),
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
 * Selects the comparison operation used by depth and stencil tests.
 *
 */
actual enum class GPUCompareFunction(val value: UInt) {
	Never(1u),
	Less(2u),
	Equal(3u),
	LessEqual(4u),
	Greater(5u),
	NotEqual(6u),
	GreaterEqual(7u),
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
 * Classifies a GPUCompilationMessage as an error, warning, or informational message.
 *
 */
actual enum class GPUCompilationMessageType(val value: UInt) {
	Error(1u),
	Warning(2u),
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
 * Selects whether no polygons, front-facing polygons, or back-facing polygons are culled.
 *
 */
actual enum class GPUCullMode(val value: UInt) {
	None(1u),
	Front(2u),
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
 * Indicates why a GPUDevice was lost.
 *
 * See [GPUDeviceLostReason in the WebGPU specification](https://www.w3.org/TR/webgpu/#enumdef-gpudevicelostreason).
 *
 */
actual enum class GPUDeviceLostReason(val value: UInt) {
	Unknown(1u),
	Destroyed(2u),
	CallbackCancelled(3u),
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
 * Selects the error category captured by a GPUDevice error scope.
 *
 */
actual enum class GPUErrorFilter(val value: UInt) {
	Validation(1u),
	OutOfMemory(2u),
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
 * Each GPUFeatureName identifies a set of functionality which, if available, allows additional usages of WebGPU that would have otherwise been invalid.
 *
 */
actual enum class GPUFeatureName(val value: UInt) {
	CoreFeaturesAndLimits(1u),
	DepthClipControl(2u),
	Depth32FloatStencil8(3u),
	TextureCompressionBC(4u),
	TextureCompressionBCSliced3D(5u),
	TextureCompressionETC2(6u),
	TextureCompressionASTC(7u),
	TextureCompressionASTCSliced3D(8u),
	TimestampQuery(9u),
	IndirectFirstInstance(10u),
	ShaderF16(11u),
	RG11B10UfloatRenderable(12u),
	BGRA8UnormStorage(13u),
	Float32Filterable(14u),
	Float32Blendable(15u),
	ClipDistances(16u),
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
 * Selects the filtering behavior used for minification and magnification sampling.
 *
 */
actual enum class GPUFilterMode(val value: UInt) {
	Nearest(1u),
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
 * Selects whether clockwise or counter-clockwise polygons are treated as front-facing.
 *
 */
actual enum class GPUFrontFace(val value: UInt) {
	CCW(1u),
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
 * The index format determines both the data type of index values in a buffer and, when used with strip primitive topologies ("line-strip" or "triangle-strip") also specifies the primitive restart value. The primitive restart value indicates which index value indicates that a new primitive should be started rather than continuing to construct the triangle strip with the prior indexed vertices.
 *
 */
actual enum class GPUIndexFormat(val value: UInt) {
	Uint16(1u),
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
 * Selects whether a render pass clears an attachment or loads its existing contents.
 *
 */
actual enum class GPULoadOp(val value: UInt) {
	Load(1u),
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
 * Selects how samples are filtered between mip levels.
 *
 */
actual enum class GPUMipmapFilterMode(val value: UInt) {
	Nearest(1u),
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
 * Hints whether adapter selection should favor lower power use or higher performance.
 *
 */
actual enum class GPUPowerPreference(val value: UInt) {
	LowPower(1u),
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
 * Selects how vertex data is assembled into points, lines, or triangles.
 *
 */
actual enum class GPUPrimitiveTopology(val value: UInt) {
	PointList(1u),
	LineList(2u),
	LineStrip(3u),
	TriangleList(4u),
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
 * Selects the kind of result recorded by a GPUQuerySet.
 *
 */
actual enum class GPUQueryType(val value: UInt) {
	Occlusion(1u),
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
 * Selects the sampling operations supported by a sampler binding.
 *
 */
actual enum class GPUSamplerBindingType(val value: UInt) {
	BindingNotUsed(0u),
	Filtering(2u),
	NonFiltering(3u),
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
 * Selects the operation applied to stencil values when a stencil test succeeds or fails.
 *
 */
actual enum class GPUStencilOperation(val value: UInt) {
	Keep(1u),
	Zero(2u),
	Replace(3u),
	Invert(4u),
	IncrementClamp(5u),
	DecrementClamp(6u),
	IncrementWrap(7u),
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
 * Selects how a storage texture binding may be accessed by shaders.
 *
 */
actual enum class GPUStorageTextureAccess(val value: UInt) {
	BindingNotUsed(0u),
	WriteOnly(2u),
	ReadOnly(3u),
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
 * Selects whether a render pass stores or discards an attachment's contents when the pass ends.
 *
 */
actual enum class GPUStoreOp(val value: UInt) {
	Store(1u),
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
 * Selects which aspect of a texture is addressed, such as color, depth, stencil, or all aspects.
 *
 */
actual enum class GPUTextureAspect(val value: UInt) {
	All(1u),
	StencilOnly(2u),
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
 * Selects the dimensionality of a GPUTexture: 1D, 2D, or 3D.
 *
 */
actual enum class GPUTextureDimension(val value: UInt) {
	OneD(1u),
	TwoD(2u),
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
 * Selects the texel representation and sampling or attachment capabilities of a GPUTexture.
 *
 */
actual enum class GPUTextureFormat(val value: UInt) {
	R8Unorm(1u),
	R8Snorm(2u),
	R8Uint(3u),
	R8Sint(4u),
	R16Unorm(5u),
	R16Snorm(6u),
	R16Uint(7u),
	R16Sint(8u),
	R16Float(9u),
	RG8Unorm(10u),
	RG8Snorm(11u),
	RG8Uint(12u),
	RG8Sint(13u),
	R32Float(14u),
	R32Uint(15u),
	R32Sint(16u),
	RG16Unorm(17u),
	RG16Snorm(18u),
	RG16Uint(19u),
	RG16Sint(20u),
	RG16Float(21u),
	RGBA8Unorm(22u),
	RGBA8UnormSrgb(23u),
	RGBA8Snorm(24u),
	RGBA8Uint(25u),
	RGBA8Sint(26u),
	BGRA8Unorm(27u),
	BGRA8UnormSrgb(28u),
	RGB10A2Uint(29u),
	RGB10A2Unorm(30u),
	RG11B10Ufloat(31u),
	RGB9E5Ufloat(32u),
	RG32Float(33u),
	RG32Uint(34u),
	RG32Sint(35u),
	RGBA16Unorm(36u),
	RGBA16Snorm(37u),
	RGBA16Uint(38u),
	RGBA16Sint(39u),
	RGBA16Float(40u),
	RGBA32Float(41u),
	RGBA32Uint(42u),
	RGBA32Sint(43u),
	Stencil8(44u),
	Depth16Unorm(45u),
	Depth24Plus(46u),
	Depth24PlusStencil8(47u),
	Depth32Float(48u),
	Depth32FloatStencil8(49u),
	BC1RGBAUnorm(50u),
	BC1RGBAUnormSrgb(51u),
	BC2RGBAUnorm(52u),
	BC2RGBAUnormSrgb(53u),
	BC3RGBAUnorm(54u),
	BC3RGBAUnormSrgb(55u),
	BC4RUnorm(56u),
	BC4RSnorm(57u),
	BC5RGUnorm(58u),
	BC5RGSnorm(59u),
	BC6HRGBUfloat(60u),
	BC6HRGBFloat(61u),
	BC7RGBAUnorm(62u),
	BC7RGBAUnormSrgb(63u),
	ETC2RGB8Unorm(64u),
	ETC2RGB8UnormSrgb(65u),
	ETC2RGB8A1Unorm(66u),
	ETC2RGB8A1UnormSrgb(67u),
	ETC2RGBA8Unorm(68u),
	ETC2RGBA8UnormSrgb(69u),
	EACR11Unorm(70u),
	EACR11Snorm(71u),
	EACRG11Unorm(72u),
	EACRG11Snorm(73u),
	ASTC4x4Unorm(74u),
	ASTC4x4UnormSrgb(75u),
	ASTC5x4Unorm(76u),
	ASTC5x4UnormSrgb(77u),
	ASTC5x5Unorm(78u),
	ASTC5x5UnormSrgb(79u),
	ASTC6x5Unorm(80u),
	ASTC6x5UnormSrgb(81u),
	ASTC6x6Unorm(82u),
	ASTC6x6UnormSrgb(83u),
	ASTC8x5Unorm(84u),
	ASTC8x5UnormSrgb(85u),
	ASTC8x6Unorm(86u),
	ASTC8x6UnormSrgb(87u),
	ASTC8x8Unorm(88u),
	ASTC8x8UnormSrgb(89u),
	ASTC10x5Unorm(90u),
	ASTC10x5UnormSrgb(91u),
	ASTC10x6Unorm(92u),
	ASTC10x6UnormSrgb(93u),
	ASTC10x8Unorm(94u),
	ASTC10x8UnormSrgb(95u),
	ASTC10x10Unorm(96u),
	ASTC10x10UnormSrgb(97u),
	ASTC12x10Unorm(98u),
	ASTC12x10UnormSrgb(99u),
	ASTC12x12Unorm(100u),
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
 * Selects the value type produced when sampling a texture binding.
 *
 */
actual enum class GPUTextureSampleType(val value: UInt) {
	BindingNotUsed(0u),
	Float(2u),
	UnfilterableFloat(3u),
	Depth(4u),
	Sint(5u),
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
 * Selects the dimensionality and array form of a GPUTextureView.
 *
 */
actual enum class GPUTextureViewDimension(val value: UInt) {
	OneD(1u),
	TwoD(2u),
	TwoDArray(3u),
	Cube(4u),
	CubeArray(5u),
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
 * Selects the scalar type, component count, and layout of a vertex attribute.
 *
 */
actual enum class GPUVertexFormat(val value: UInt) {
	Uint8(1u),
	Uint8x2(2u),
	Uint8x4(3u),
	Sint8(4u),
	Sint8x2(5u),
	Sint8x4(6u),
	Unorm8(7u),
	Unorm8x2(8u),
	Unorm8x4(9u),
	Snorm8(10u),
	Snorm8x2(11u),
	Snorm8x4(12u),
	Uint16(13u),
	Uint16x2(14u),
	Uint16x4(15u),
	Sint16(16u),
	Sint16x2(17u),
	Sint16x4(18u),
	Unorm16(19u),
	Unorm16x2(20u),
	Unorm16x4(21u),
	Snorm16(22u),
	Snorm16x2(23u),
	Snorm16x4(24u),
	Float16(25u),
	Float16x2(26u),
	Float16x4(27u),
	Float32(28u),
	Float32x2(29u),
	Float32x3(30u),
	Float32x4(31u),
	Uint32(32u),
	Uint32x2(33u),
	Uint32x3(34u),
	Uint32x4(35u),
	Sint32(36u),
	Sint32x2(37u),
	Sint32x3(38u),
	Sint32x4(39u),
	Unorm1010102(40u),
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
 * Selects whether a vertex buffer advances once per vertex or once per instance.
 *
 */
actual enum class GPUVertexStepMode(val value: UInt) {
	Vertex(1u),
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
