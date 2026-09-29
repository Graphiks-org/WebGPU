package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import kotlin.test.assertEquals

internal val SOLID_RED = byteArrayOf(255.toByte(), 0, 0, 255.toByte())
internal val SOLID_GREEN = byteArrayOf(0, 255.toByte(), 0, 255.toByte())
internal val SOLID_BLUE = byteArrayOf(0, 0, 255.toByte(), 255.toByte())

/**
 * A 2×1 RGBA8 texture with a red texel at u = 0 and a blue texel at u = 1; the sampling cases read
 * the same texture through different samplers so only the sampler changes the result.
 */
internal fun createRedBlueTexture(device: GPUDevice): GPUTexture =
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 1u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
        ),
    ).also { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(SOLID_RED + SOLID_BLUE),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 8u, rowsPerImage = 1u),
            Extent3D(2u, 1u, 1u),
        )
    }

/**
 * Asserts one channel of the `RGBA` vector at output [index] of [floats]: 0 is R, 1 G, 2 B, 3 A.
 */
internal fun assertColorChannel(
    floats: FloatArray,
    index: Int,
    channel: Int,
    expected: Float,
    message: String,
    tolerance: Float = 1e-6f,
) {
    assertEquals(expected, floats[index * 4 + channel], tolerance, "$message (${"RGBA"[channel]})")
}

/**
 * Asserts a colour whose red and blue channels are filtered with [tolerance] while green must stay
 * zero and alpha must stay one. Any filtering of an opaque red/blue pair leaves those two channels
 * invariant, so they are held to [invariantTolerance], far tighter than the filtering tolerance,
 * instead of inheriting it. A backend whose mip blending quantises alpha slightly passes a small
 * [invariantTolerance] but a regression that actually changes the invariant channels fails.
 */
internal fun assertFilteredRedBlue(
    floats: FloatArray,
    index: Int,
    r: Float,
    b: Float,
    message: String,
    tolerance: Float,
    invariantTolerance: Float = 1e-6f,
) {
    assertColorChannel(floats, index, 0, r, message, tolerance)
    assertColorChannel(floats, index, 1, 0f, "$message (green stays zero)", invariantTolerance)
    assertColorChannel(floats, index, 2, b, message, tolerance)
    assertColorChannel(floats, index, 3, 1f, "$message (alpha stays one)", invariantTolerance)
}
