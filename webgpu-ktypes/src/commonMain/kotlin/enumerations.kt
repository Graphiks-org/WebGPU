@file:Suppress("unused")
// This file has been generated DO NO EDIT
package io.ygdrasil.webgpu

/**
 * Selects how texture coordinates outside the normalized range are addressed.
 *
 */
expect enum class GPUAddressMode {
	ClampToEdge,
	Repeat,
	MirrorRepeat;
}

/**
 * Defines the source or destination factor used to scale color and alpha values during blending.
 *
 */
expect enum class GPUBlendFactor {
	Zero,
	One,
	Src,
	OneMinusSrc,
	SrcAlpha,
	OneMinusSrcAlpha,
	Dst,
	OneMinusDst,
	DstAlpha,
	OneMinusDstAlpha,
	SrcAlphaSaturated,
	Constant,
	OneMinusConstant,
	Src1,
	OneMinusSrc1,
	Src1Alpha,
	OneMinusSrc1Alpha;
}

/**
 * Defines the operation used to combine source and destination blend factors.
 *
 */
expect enum class GPUBlendOperation {
	Add,
	Subtract,
	ReverseSubtract,
	Min,
	Max;
}

/**
 * Selects how a buffer binding is accessed by shaders.
 *
 */
expect enum class GPUBufferBindingType {
	BindingNotUsed,
	Uniform,
	Storage,
	ReadOnlyStorage;
}

/**
 * The mapping state of a GPUBuffer: mapped or unmapped.
 *
 */
expect enum class GPUBufferMapState {
	Unmapped,
	Pending,
	Mapped;
}

/**
 * Selects the comparison operation used by depth and stencil tests.
 *
 */
expect enum class GPUCompareFunction {
	Never,
	Less,
	Equal,
	LessEqual,
	Greater,
	NotEqual,
	GreaterEqual,
	Always;
}

/**
 * Classifies a GPUCompilationMessage as an error, warning, or informational message.
 *
 */
expect enum class GPUCompilationMessageType {
	Error,
	Warning,
	Info;
}

/**
 * Selects whether no polygons, front-facing polygons, or back-facing polygons are culled.
 *
 */
expect enum class GPUCullMode {
	None,
	Front,
	Back;
}

/**
 * Indicates why a GPUDevice was lost.
 *
 */
expect enum class GPUDeviceLostReason {
	Unknown,
	Destroyed,
	CallbackCancelled,
	FailedCreation;
}

/**
 * Selects the error category captured by a GPUDevice error scope.
 *
 */
expect enum class GPUErrorFilter {
	Validation,
	OutOfMemory,
	Internal;
}

/**
 * Each GPUFeatureName identifies a set of functionality which, if available, allows additional usages of WebGPU that would have otherwise been invalid.
 *
 */
expect enum class GPUFeatureName {
	CoreFeaturesAndLimits,
	DepthClipControl,
	Depth32FloatStencil8,
	TextureCompressionBC,
	TextureCompressionBCSliced3D,
	TextureCompressionETC2,
	TextureCompressionASTC,
	TextureCompressionASTCSliced3D,
	TimestampQuery,
	IndirectFirstInstance,
	ShaderF16,
	RG11B10UfloatRenderable,
	BGRA8UnormStorage,
	Float32Filterable,
	Float32Blendable,
	ClipDistances,
	DualSourceBlending,
	Subgroups,
	TextureFormatsTier1,
	TextureFormatsTier2,
	PrimitiveIndex,
	TextureComponentSwizzle;
}

/**
 * Selects the filtering behavior used for minification and magnification sampling.
 *
 */
expect enum class GPUFilterMode {
	Nearest,
	Linear;
}

/**
 * Selects whether clockwise or counter-clockwise polygons are treated as front-facing.
 *
 */
expect enum class GPUFrontFace {
	CCW,
	CW;
}

/**
 * The index format determines both the data type of index values in a buffer and, when used with strip primitive topologies ("line-strip" or "triangle-strip") also specifies the primitive restart value. The primitive restart value indicates which index value indicates that a new primitive should be started rather than continuing to construct the triangle strip with the prior indexed vertices.
 *
 */
expect enum class GPUIndexFormat {
	Uint16,
	Uint32;
}

/**
 * Selects whether a render pass clears an attachment or loads its existing contents.
 *
 */
expect enum class GPULoadOp {
	Load,
	Clear;
}

/**
 * Selects how samples are filtered between mip levels.
 *
 */
expect enum class GPUMipmapFilterMode {
	Nearest,
	Linear;
}

/**
 * Hints whether adapter selection should favor lower power use or higher performance.
 *
 */
expect enum class GPUPowerPreference {
	LowPower,
	HighPerformance;
}

/**
 * Selects how vertex data is assembled into points, lines, or triangles.
 *
 */
expect enum class GPUPrimitiveTopology {
	PointList,
	LineList,
	LineStrip,
	TriangleList,
	TriangleStrip;
}

/**
 * Selects the kind of result recorded by a GPUQuerySet.
 *
 */
expect enum class GPUQueryType {
	Occlusion,
	Timestamp;
}

/**
 * Selects the sampling operations supported by a sampler binding.
 *
 */
expect enum class GPUSamplerBindingType {
	BindingNotUsed,
	Filtering,
	NonFiltering,
	Comparison;
}

/**
 * Selects the operation applied to stencil values when a stencil test succeeds or fails.
 *
 */
expect enum class GPUStencilOperation {
	Keep,
	Zero,
	Replace,
	Invert,
	IncrementClamp,
	DecrementClamp,
	IncrementWrap,
	DecrementWrap;
}

/**
 * Selects how a storage texture binding may be accessed by shaders.
 *
 */
expect enum class GPUStorageTextureAccess {
	BindingNotUsed,
	WriteOnly,
	ReadOnly,
	ReadWrite;
}

/**
 * Selects whether a render pass stores or discards an attachment's contents when the pass ends.
 *
 */
expect enum class GPUStoreOp {
	Store,
	Discard;
}

/**
 * Selects which aspect of a texture is addressed, such as color, depth, stencil, or all aspects.
 *
 */
expect enum class GPUTextureAspect {
	All,
	StencilOnly,
	DepthOnly;
}

/**
 * Selects the dimensionality of a GPUTexture: 1D, 2D, or 3D.
 *
 */
expect enum class GPUTextureDimension {
	OneD,
	TwoD,
	ThreeD;
}

/**
 * Selects the texel representation and sampling or attachment capabilities of a GPUTexture.
 *
 */
expect enum class GPUTextureFormat {
	R8Unorm,
	R8Snorm,
	R8Uint,
	R8Sint,
	R16Unorm,
	R16Snorm,
	R16Uint,
	R16Sint,
	R16Float,
	RG8Unorm,
	RG8Snorm,
	RG8Uint,
	RG8Sint,
	R32Float,
	R32Uint,
	R32Sint,
	RG16Unorm,
	RG16Snorm,
	RG16Uint,
	RG16Sint,
	RG16Float,
	RGBA8Unorm,
	RGBA8UnormSrgb,
	RGBA8Snorm,
	RGBA8Uint,
	RGBA8Sint,
	BGRA8Unorm,
	BGRA8UnormSrgb,
	RGB10A2Uint,
	RGB10A2Unorm,
	RG11B10Ufloat,
	RGB9E5Ufloat,
	RG32Float,
	RG32Uint,
	RG32Sint,
	RGBA16Unorm,
	RGBA16Snorm,
	RGBA16Uint,
	RGBA16Sint,
	RGBA16Float,
	RGBA32Float,
	RGBA32Uint,
	RGBA32Sint,
	Stencil8,
	Depth16Unorm,
	Depth24Plus,
	Depth24PlusStencil8,
	Depth32Float,
	Depth32FloatStencil8,
	BC1RGBAUnorm,
	BC1RGBAUnormSrgb,
	BC2RGBAUnorm,
	BC2RGBAUnormSrgb,
	BC3RGBAUnorm,
	BC3RGBAUnormSrgb,
	BC4RUnorm,
	BC4RSnorm,
	BC5RGUnorm,
	BC5RGSnorm,
	BC6HRGBUfloat,
	BC6HRGBFloat,
	BC7RGBAUnorm,
	BC7RGBAUnormSrgb,
	ETC2RGB8Unorm,
	ETC2RGB8UnormSrgb,
	ETC2RGB8A1Unorm,
	ETC2RGB8A1UnormSrgb,
	ETC2RGBA8Unorm,
	ETC2RGBA8UnormSrgb,
	EACR11Unorm,
	EACR11Snorm,
	EACRG11Unorm,
	EACRG11Snorm,
	ASTC4x4Unorm,
	ASTC4x4UnormSrgb,
	ASTC5x4Unorm,
	ASTC5x4UnormSrgb,
	ASTC5x5Unorm,
	ASTC5x5UnormSrgb,
	ASTC6x5Unorm,
	ASTC6x5UnormSrgb,
	ASTC6x6Unorm,
	ASTC6x6UnormSrgb,
	ASTC8x5Unorm,
	ASTC8x5UnormSrgb,
	ASTC8x6Unorm,
	ASTC8x6UnormSrgb,
	ASTC8x8Unorm,
	ASTC8x8UnormSrgb,
	ASTC10x5Unorm,
	ASTC10x5UnormSrgb,
	ASTC10x6Unorm,
	ASTC10x6UnormSrgb,
	ASTC10x8Unorm,
	ASTC10x8UnormSrgb,
	ASTC10x10Unorm,
	ASTC10x10UnormSrgb,
	ASTC12x10Unorm,
	ASTC12x10UnormSrgb,
	ASTC12x12Unorm,
	ASTC12x12UnormSrgb;
}

/**
 * Selects the value type produced when sampling a texture binding.
 *
 */
expect enum class GPUTextureSampleType {
	BindingNotUsed,
	Float,
	UnfilterableFloat,
	Depth,
	Sint,
	Uint;
}

/**
 * Selects the dimensionality and array form of a GPUTextureView.
 *
 */
expect enum class GPUTextureViewDimension {
	OneD,
	TwoD,
	TwoDArray,
	Cube,
	CubeArray,
	ThreeD;
}

/**
 * Selects the scalar type, component count, and layout of a vertex attribute.
 *
 */
expect enum class GPUVertexFormat {
	Uint8,
	Uint8x2,
	Uint8x4,
	Sint8,
	Sint8x2,
	Sint8x4,
	Unorm8,
	Unorm8x2,
	Unorm8x4,
	Snorm8,
	Snorm8x2,
	Snorm8x4,
	Uint16,
	Uint16x2,
	Uint16x4,
	Sint16,
	Sint16x2,
	Sint16x4,
	Unorm16,
	Unorm16x2,
	Unorm16x4,
	Snorm16,
	Snorm16x2,
	Snorm16x4,
	Float16,
	Float16x2,
	Float16x4,
	Float32,
	Float32x2,
	Float32x3,
	Float32x4,
	Uint32,
	Uint32x2,
	Uint32x3,
	Uint32x4,
	Sint32,
	Sint32x2,
	Sint32x3,
	Sint32x4,
	Unorm1010102,
	Unorm8x4BGRA;
}

/**
 * Selects whether a vertex buffer advances once per vertex or once per instance.
 *
 */
expect enum class GPUVertexStepMode {
	Vertex,
	Instance;
}
