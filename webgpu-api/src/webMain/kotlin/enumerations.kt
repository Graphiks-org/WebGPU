@file:Suppress("unused")
// This file has been generated DO NO EDIT
package org.graphiks.webgpu

/**
 * Selects how texture coordinates outside the normalized range are addressed.
 *
 */
actual enum class GPUAddressMode(val value: String) {
	ClampToEdge("clamp-to-edge"),
	Repeat("repeat"),
	MirrorRepeat("mirror-repeat");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUAddressMode? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Defines the source or destination factor used to scale color and alpha values during blending.
 *
 */
actual enum class GPUBlendFactor(val value: String) {
	Zero("zero"),
	One("one"),
	Src("src"),
	OneMinusSrc("one-minus-src"),
	SrcAlpha("src-alpha"),
	OneMinusSrcAlpha("one-minus-src-alpha"),
	Dst("dst"),
	OneMinusDst("one-minus-dst"),
	DstAlpha("dst-alpha"),
	OneMinusDstAlpha("one-minus-dst-alpha"),
	SrcAlphaSaturated("src-alpha-saturated"),
	Constant("constant"),
	OneMinusConstant("one-minus-constant"),
	Src1("src1"),
	OneMinusSrc1("one-minus-src1"),
	Src1Alpha("src1-alpha"),
	OneMinusSrc1Alpha("one-minus-src1-alpha");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUBlendFactor? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Defines the operation used to combine source and destination blend factors.
 *
 */
actual enum class GPUBlendOperation(val value: String) {
	Add("add"),
	Subtract("subtract"),
	ReverseSubtract("reverse-subtract"),
	Min("min"),
	Max("max");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUBlendOperation? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects how a buffer binding is accessed by shaders.
 *
 */
actual enum class GPUBufferBindingType(val value: String) {
	BindingNotUsed("unsupported"),
	Uniform("uniform"),
	Storage("storage"),
	ReadOnlyStorage("read-only-storage");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUBufferBindingType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The mapping state of a GPUBuffer: mapped or unmapped.
 *
 */
actual enum class GPUBufferMapState(val value: String) {
	Unmapped("unmapped"),
	Pending("pending"),
	Mapped("mapped");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUBufferMapState? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the comparison operation used by depth and stencil tests.
 *
 */
actual enum class GPUCompareFunction(val value: String) {
	Never("never"),
	Less("less"),
	Equal("equal"),
	LessEqual("less-equal"),
	Greater("greater"),
	NotEqual("not-equal"),
	GreaterEqual("greater-equal"),
	Always("always");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUCompareFunction? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Classifies a GPUCompilationMessage as an error, warning, or informational message.
 *
 */
actual enum class GPUCompilationMessageType(val value: String) {
	Error("error"),
	Warning("warning"),
	Info("info");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUCompilationMessageType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects whether no polygons, front-facing polygons, or back-facing polygons are culled.
 *
 */
actual enum class GPUCullMode(val value: String) {
	None("none"),
	Front("front"),
	Back("back");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUCullMode? {
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
actual enum class GPUDeviceLostReason(val value: String) {
	Unknown("unknown"),
	Destroyed("destroyed"),
	CallbackCancelled("unsupported"),
	FailedCreation("unsupported");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUDeviceLostReason? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the error category captured by a GPUDevice error scope.
 *
 */
actual enum class GPUErrorFilter(val value: String) {
	Validation("validation"),
	OutOfMemory("out-of-memory"),
	Internal("internal");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUErrorFilter? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Each GPUFeatureName identifies a set of functionality which, if available, allows additional usages of WebGPU that would have otherwise been invalid.
 *
 */
actual enum class GPUFeatureName(val value: String) {
	CoreFeaturesAndLimits("core-features-and-limits"),
	DepthClipControl("depth-clip-control"),
	Depth32FloatStencil8("depth32float-stencil8"),
	TextureCompressionBC("texture-compression-bc"),
	TextureCompressionBCSliced3D("texture-compression-bc-sliced-3d"),
	TextureCompressionETC2("texture-compression-etc2"),
	TextureCompressionASTC("texture-compression-astc"),
	TextureCompressionASTCSliced3D("texture-compression-astc-sliced-3d"),
	TimestampQuery("timestamp-query"),
	IndirectFirstInstance("indirect-first-instance"),
	ShaderF16("shader-f16"),
	RG11B10UfloatRenderable("rg11b10ufloat-renderable"),
	BGRA8UnormStorage("bgra8unorm-storage"),
	Float32Filterable("float32-filterable"),
	Float32Blendable("float32-blendable"),
	ClipDistances("clip-distances"),
	DualSourceBlending("dual-source-blending"),
	Subgroups("subgroups"),
	TextureFormatsTier1("texture-formats-tier1"),
	TextureFormatsTier2("texture-formats-tier2"),
	PrimitiveIndex("primitive-index"),
	TextureComponentSwizzle("texture-component-swizzle");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUFeatureName? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the filtering behavior used for minification and magnification sampling.
 *
 */
actual enum class GPUFilterMode(val value: String) {
	Nearest("nearest"),
	Linear("linear");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUFilterMode? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects whether clockwise or counter-clockwise polygons are treated as front-facing.
 *
 */
actual enum class GPUFrontFace(val value: String) {
	CCW("ccw"),
	CW("cw");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUFrontFace? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * The index format determines both the data type of index values in a buffer and, when used with strip primitive topologies ("line-strip" or "triangle-strip") also specifies the primitive restart value. The primitive restart value indicates which index value indicates that a new primitive should be started rather than continuing to construct the triangle strip with the prior indexed vertices.
 *
 */
actual enum class GPUIndexFormat(val value: String) {
	Uint16("uint16"),
	Uint32("uint32");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUIndexFormat? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects whether a render pass clears an attachment or loads its existing contents.
 *
 */
actual enum class GPULoadOp(val value: String) {
	Load("load"),
	Clear("clear");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPULoadOp? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects how samples are filtered between mip levels.
 *
 */
actual enum class GPUMipmapFilterMode(val value: String) {
	Nearest("nearest"),
	Linear("linear");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUMipmapFilterMode? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Hints whether adapter selection should favor lower power use or higher performance.
 *
 */
actual enum class GPUPowerPreference(val value: String) {
	LowPower("low-power"),
	HighPerformance("high-performance");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUPowerPreference? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects how vertex data is assembled into points, lines, or triangles.
 *
 */
actual enum class GPUPrimitiveTopology(val value: String) {
	PointList("point-list"),
	LineList("line-list"),
	LineStrip("line-strip"),
	TriangleList("triangle-list"),
	TriangleStrip("triangle-strip");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUPrimitiveTopology? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the kind of result recorded by a GPUQuerySet.
 *
 */
actual enum class GPUQueryType(val value: String) {
	Occlusion("occlusion"),
	Timestamp("timestamp");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUQueryType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the sampling operations supported by a sampler binding.
 *
 */
actual enum class GPUSamplerBindingType(val value: String) {
	BindingNotUsed("unsupported"),
	Filtering("filtering"),
	NonFiltering("non-filtering"),
	Comparison("comparison");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUSamplerBindingType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the operation applied to stencil values when a stencil test succeeds or fails.
 *
 */
actual enum class GPUStencilOperation(val value: String) {
	Keep("keep"),
	Zero("zero"),
	Replace("replace"),
	Invert("invert"),
	IncrementClamp("increment-clamp"),
	DecrementClamp("decrement-clamp"),
	IncrementWrap("increment-wrap"),
	DecrementWrap("decrement-wrap");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUStencilOperation? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects how a storage texture binding may be accessed by shaders.
 *
 */
actual enum class GPUStorageTextureAccess(val value: String) {
	BindingNotUsed("unsupported"),
	WriteOnly("write-only"),
	ReadOnly("read-only"),
	ReadWrite("read-write");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUStorageTextureAccess? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects whether a render pass stores or discards an attachment's contents when the pass ends.
 *
 */
actual enum class GPUStoreOp(val value: String) {
	Store("store"),
	Discard("discard");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUStoreOp? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects which aspect of a texture is addressed, such as color, depth, stencil, or all aspects.
 *
 */
actual enum class GPUTextureAspect(val value: String) {
	All("all"),
	StencilOnly("stencil-only"),
	DepthOnly("depth-only");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUTextureAspect? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the dimensionality of a GPUTexture: 1D, 2D, or 3D.
 *
 */
actual enum class GPUTextureDimension(val value: String) {
	OneD("1d"),
	TwoD("2d"),
	ThreeD("3d");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUTextureDimension? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the texel representation and sampling or attachment capabilities of a GPUTexture.
 *
 */
actual enum class GPUTextureFormat(val value: String) {
	R8Unorm("r8unorm"),
	R8Snorm("r8snorm"),
	R8Uint("r8uint"),
	R8Sint("r8sint"),
	R16Unorm("r16unorm"),
	R16Snorm("r16snorm"),
	R16Uint("r16uint"),
	R16Sint("r16sint"),
	R16Float("r16float"),
	RG8Unorm("rg8unorm"),
	RG8Snorm("rg8snorm"),
	RG8Uint("rg8uint"),
	RG8Sint("rg8sint"),
	R32Float("r32float"),
	R32Uint("r32uint"),
	R32Sint("r32sint"),
	RG16Unorm("rg16unorm"),
	RG16Snorm("rg16snorm"),
	RG16Uint("rg16uint"),
	RG16Sint("rg16sint"),
	RG16Float("rg16float"),
	RGBA8Unorm("rgba8unorm"),
	RGBA8UnormSrgb("rgba8unorm-srgb"),
	RGBA8Snorm("rgba8snorm"),
	RGBA8Uint("rgba8uint"),
	RGBA8Sint("rgba8sint"),
	BGRA8Unorm("bgra8unorm"),
	BGRA8UnormSrgb("bgra8unorm-srgb"),
	RGB10A2Uint("rgb10a2uint"),
	RGB10A2Unorm("rgb10a2unorm"),
	RG11B10Ufloat("rg11b10ufloat"),
	RGB9E5Ufloat("rgb9e5ufloat"),
	RG32Float("rg32float"),
	RG32Uint("rg32uint"),
	RG32Sint("rg32sint"),
	RGBA16Unorm("rgba16unorm"),
	RGBA16Snorm("rgba16snorm"),
	RGBA16Uint("rgba16uint"),
	RGBA16Sint("rgba16sint"),
	RGBA16Float("rgba16float"),
	RGBA32Float("rgba32float"),
	RGBA32Uint("rgba32uint"),
	RGBA32Sint("rgba32sint"),
	Stencil8("stencil8"),
	Depth16Unorm("depth16unorm"),
	Depth24Plus("depth24plus"),
	Depth24PlusStencil8("depth24plus-stencil8"),
	Depth32Float("depth32float"),
	Depth32FloatStencil8("depth32float-stencil8"),
	BC1RGBAUnorm("bc1-rgba-unorm"),
	BC1RGBAUnormSrgb("bc1-rgba-unorm-srgb"),
	BC2RGBAUnorm("bc2-rgba-unorm"),
	BC2RGBAUnormSrgb("bc2-rgba-unorm-srgb"),
	BC3RGBAUnorm("bc3-rgba-unorm"),
	BC3RGBAUnormSrgb("bc3-rgba-unorm-srgb"),
	BC4RUnorm("bc4-r-unorm"),
	BC4RSnorm("bc4-r-snorm"),
	BC5RGUnorm("bc5-rg-unorm"),
	BC5RGSnorm("bc5-rg-snorm"),
	BC6HRGBUfloat("bc6h-rgb-ufloat"),
	BC6HRGBFloat("bc6h-rgb-float"),
	BC7RGBAUnorm("bc7-rgba-unorm"),
	BC7RGBAUnormSrgb("bc7-rgba-unorm-srgb"),
	ETC2RGB8Unorm("etc2-rgb8unorm"),
	ETC2RGB8UnormSrgb("etc2-rgb8unorm-srgb"),
	ETC2RGB8A1Unorm("etc2-rgb8a1unorm"),
	ETC2RGB8A1UnormSrgb("etc2-rgb8a1unorm-srgb"),
	ETC2RGBA8Unorm("etc2-rgba8unorm"),
	ETC2RGBA8UnormSrgb("etc2-rgba8unorm-srgb"),
	EACR11Unorm("eac-r11unorm"),
	EACR11Snorm("eac-r11snorm"),
	EACRG11Unorm("eac-rg11unorm"),
	EACRG11Snorm("eac-rg11snorm"),
	ASTC4x4Unorm("astc-4x4-unorm"),
	ASTC4x4UnormSrgb("astc-4x4-unorm-srgb"),
	ASTC5x4Unorm("astc-5x4-unorm"),
	ASTC5x4UnormSrgb("astc-5x4-unorm-srgb"),
	ASTC5x5Unorm("astc-5x5-unorm"),
	ASTC5x5UnormSrgb("astc-5x5-unorm-srgb"),
	ASTC6x5Unorm("astc-6x5-unorm"),
	ASTC6x5UnormSrgb("astc-6x5-unorm-srgb"),
	ASTC6x6Unorm("astc-6x6-unorm"),
	ASTC6x6UnormSrgb("astc-6x6-unorm-srgb"),
	ASTC8x5Unorm("astc-8x5-unorm"),
	ASTC8x5UnormSrgb("astc-8x5-unorm-srgb"),
	ASTC8x6Unorm("astc-8x6-unorm"),
	ASTC8x6UnormSrgb("astc-8x6-unorm-srgb"),
	ASTC8x8Unorm("astc-8x8-unorm"),
	ASTC8x8UnormSrgb("astc-8x8-unorm-srgb"),
	ASTC10x5Unorm("astc-10x5-unorm"),
	ASTC10x5UnormSrgb("astc-10x5-unorm-srgb"),
	ASTC10x6Unorm("astc-10x6-unorm"),
	ASTC10x6UnormSrgb("astc-10x6-unorm-srgb"),
	ASTC10x8Unorm("astc-10x8-unorm"),
	ASTC10x8UnormSrgb("astc-10x8-unorm-srgb"),
	ASTC10x10Unorm("astc-10x10-unorm"),
	ASTC10x10UnormSrgb("astc-10x10-unorm-srgb"),
	ASTC12x10Unorm("astc-12x10-unorm"),
	ASTC12x10UnormSrgb("astc-12x10-unorm-srgb"),
	ASTC12x12Unorm("astc-12x12-unorm"),
	ASTC12x12UnormSrgb("astc-12x12-unorm-srgb");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUTextureFormat? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the value type produced when sampling a texture binding.
 *
 */
actual enum class GPUTextureSampleType(val value: String) {
	BindingNotUsed("unsupported"),
	Float("float"),
	UnfilterableFloat("unfilterable-float"),
	Depth("depth"),
	Sint("sint"),
	Uint("uint");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUTextureSampleType? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the dimensionality and array form of a GPUTextureView.
 *
 */
actual enum class GPUTextureViewDimension(val value: String) {
	OneD("1d"),
	TwoD("2d"),
	TwoDArray("2d-array"),
	Cube("cube"),
	CubeArray("cube-array"),
	ThreeD("3d");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUTextureViewDimension? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects the scalar type, component count, and layout of a vertex attribute.
 *
 */
actual enum class GPUVertexFormat(val value: String) {
	Uint8("uint8"),
	Uint8x2("uint8x2"),
	Uint8x4("uint8x4"),
	Sint8("sint8"),
	Sint8x2("sint8x2"),
	Sint8x4("sint8x4"),
	Unorm8("unorm8"),
	Unorm8x2("unorm8x2"),
	Unorm8x4("unorm8x4"),
	Snorm8("snorm8"),
	Snorm8x2("snorm8x2"),
	Snorm8x4("snorm8x4"),
	Uint16("uint16"),
	Uint16x2("uint16x2"),
	Uint16x4("uint16x4"),
	Sint16("sint16"),
	Sint16x2("sint16x2"),
	Sint16x4("sint16x4"),
	Unorm16("unorm16"),
	Unorm16x2("unorm16x2"),
	Unorm16x4("unorm16x4"),
	Snorm16("snorm16"),
	Snorm16x2("snorm16x2"),
	Snorm16x4("snorm16x4"),
	Float16("float16"),
	Float16x2("float16x2"),
	Float16x4("float16x4"),
	Float32("float32"),
	Float32x2("float32x2"),
	Float32x3("float32x3"),
	Float32x4("float32x4"),
	Uint32("uint32"),
	Uint32x2("uint32x2"),
	Uint32x3("uint32x3"),
	Uint32x4("uint32x4"),
	Sint32("sint32"),
	Sint32x2("sint32x2"),
	Sint32x3("sint32x3"),
	Sint32x4("sint32x4"),
	Unorm1010102("unorm10-10-10-2"),
	Unorm8x4BGRA("unorm8x4-bgra");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUVertexFormat? {
			return entries.find { it.value == value }
		}
    }

}

/**
 * Selects whether a vertex buffer advances once per vertex or once per instance.
 *
 */
actual enum class GPUVertexStepMode(val value: String) {
	Vertex("vertex"),
	Instance("instance");


	companion object {
		/**
		 * Retrieves the corresponding [String] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [String] or `null` if no match is found.
		 */
		fun of(value: String): GPUVertexStepMode? {
			return entries.find { it.value == value }
		}
    }

}
