package org.graphiks.webgpu

/** A source component used to construct one output channel of a texture view. */
enum class GPUTextureSwizzleSource(internal val token: Char) {
    Red('r'),
    Green('g'),
    Blue('b'),
    Alpha('a'),
    Zero('0'),
    One('1'),
}

/**
 * Maps each output channel to a component of the texture view.
 *
 * The properties correspond to the red, green, blue, and alpha output channels, in that order. Each
 * channel can select red, green, blue, alpha, constant zero, or constant one; selections may repeat.
 * The default value is the identity mapping `rgba`.
 *
 * A non-identity mapping requires the WebGPU `texture-component-swizzle` feature on the device used
 * to create the view. WebGPU requires the identity mapping when a view is used as a storage texture
 * or as a render attachment.
 *
 * Use [toWebGpuString] to convert this value to the four-character `DOMString` expected by the Web
 * binding. For example, the mapping `b01r` selects blue, zero, one, and red for the output channels.
 */
data class GPUTextureSwizzle(
    val red: GPUTextureSwizzleSource = GPUTextureSwizzleSource.Red,
    val green: GPUTextureSwizzleSource = GPUTextureSwizzleSource.Green,
    val blue: GPUTextureSwizzleSource = GPUTextureSwizzleSource.Blue,
    val alpha: GPUTextureSwizzleSource = GPUTextureSwizzleSource.Alpha,
) {
    /** Returns the four-character `DOMString` expected by WebGPU. */
    fun toWebGpuString(): String = buildString(4) {
        append(red.token)
        append(green.token)
        append(blue.token)
        append(alpha.token)
    }
}
