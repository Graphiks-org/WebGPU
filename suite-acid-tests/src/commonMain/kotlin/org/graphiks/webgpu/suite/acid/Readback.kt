package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUOrigin3D
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.Origin3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Copies [size] bytes out of [buffer] through a staging buffer and returns a fresh copy.
 *
 * The staging copy is explicit and the array is copied before `unmap`; [buffer] is borrowed and
 * never closed here.
 */
internal suspend fun readBufferBytes(device: GPUDevice, buffer: GPUBuffer, size: ULong): ByteArray {
    device.createBuffer(
        BufferDescriptor(
            size = size,
            usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead,
        ),
    ).use { staging ->
        device.createCommandEncoder().use { encoder ->
            encoder.copyBufferToBuffer(buffer, 0uL, staging, 0uL, size)
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
        staging.mapAsync(GPUMapMode.Read).getOrThrow()
        try {
            return staging.getMappedRange().toByteArray().copyOf(size.toInt())
        } finally {
            staging.unmap()
        }
    }
}

/**
 * Reads an RGBA8 subresource back as tightly packed texels (`width * height * 4` bytes).
 *
 * The staging buffer uses the 256-byte row alignment that `copyTextureToBuffer` requires; the
 * padding is stripped on the CPU before returning. This is for one mip level, one layer and a
 * four-byte format: cases that assert the copy layout itself encode their own copy.
 */
internal suspend fun readRgba8(
    device: GPUDevice,
    texture: GPUTexture,
    width: Int,
    height: Int,
    mipLevel: UInt = 0u,
    origin: GPUOrigin3D = Origin3D(),
): ByteArray {
    require(width > 0 && height > 0) { "readRgba8 needs positive dimensions, got ${width}x$height" }
    val rowBytes = width * 4
    val stride = (rowBytes + 255) / 256 * 256

    device.createBuffer(
        BufferDescriptor(
            size = (stride * height).toULong(),
            usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead,
        ),
    ).use { staging ->
        device.createCommandEncoder().use { encoder ->
            encoder.copyTextureToBuffer(
                TexelCopyTextureInfo(texture = texture, mipLevel = mipLevel, origin = origin),
                TexelCopyBufferInfo(staging, bytesPerRow = stride.toUInt(), rowsPerImage = height.toUInt()),
                Extent3D(width.toUInt(), height.toUInt(), 1u),
            )
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
        staging.mapAsync(GPUMapMode.Read).getOrThrow()
        try {
            val padded = staging.getMappedRange().toByteArray()
            val tight = ByteArray(rowBytes * height)
            for (row in 0 until height) {
                padded.copyInto(tight, row * rowBytes, row * stride, row * stride + rowBytes)
            }
            return tight
        } finally {
            staging.unmap()
        }
    }
}

/**
 * Asserts the RGBA8 pixel at ([x], [y]) of tightly packed [pixels] equals the expected channels.
 *
 * Each channel is compared with an absolute difference no greater than [tolerance] (exact by
 * default). The message names the position, the channel and both values.
 */
internal fun assertPixel(
    pixels: ByteArray,
    width: Int,
    x: Int,
    y: Int,
    r: Int,
    g: Int,
    b: Int,
    a: Int,
    tolerance: Int = 0,
) {
    val offset = (y * width + x) * 4
    val expected = intArrayOf(r, g, b, a)
    val names = listOf("R", "G", "B", "A")
    for (channel in 0..3) {
        val observed = pixels[offset + channel].toInt() and 255
        val difference = abs(observed - expected[channel])
        assertTrue(
            difference <= tolerance,
            "Pixel ($x, $y) channel ${names[channel]}: expected ${expected[channel]} (±$tolerance) but observed $observed",
        )
    }
}
