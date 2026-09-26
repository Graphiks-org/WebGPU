package io.ygdrasil.webgpu

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
 * Maps the red, green, blue, and alpha output channels to source components.
 *
 * The default value preserves the texture's original `rgba` channels. Non-default values require the
 * WebGPU `texture-component-swizzle` feature on the device used to create the view.
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
