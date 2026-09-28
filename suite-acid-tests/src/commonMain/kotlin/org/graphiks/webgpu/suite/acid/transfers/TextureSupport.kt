package org.graphiks.webgpu.suite.acid.transfers

/**
 * Opaque RGBA8 pixel bytes for the four colours the texture-transfer cases use. They are exact
 * values: a texel either holds the written colour or the explicitly initialised black.
 */
internal val OPAQUE_RED = byteArrayOf(255.toByte(), 0, 0, 255.toByte())
internal val OPAQUE_GREEN = byteArrayOf(0, 255.toByte(), 0, 255.toByte())
internal val OPAQUE_BLUE = byteArrayOf(0, 0, 255.toByte(), 255.toByte())
internal val OPAQUE_BLACK = byteArrayOf(0, 0, 0, 255.toByte())
internal val OPAQUE_WHITE = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())

/** A tightly packed RGBA8 row of [count] [color] pixels. */
internal fun rgbaRow(color: ByteArray, count: Int): ByteArray {
    val row = ByteArray(count * 4)
    for (pixel in 0 until count) color.copyInto(row, pixel * 4)
    return row
}

/** A tightly packed RGBA8 image of [color] pixels. */
internal fun rgbaImage(color: ByteArray, width: Int, height: Int): ByteArray {
    val bytes = ByteArray(width * height * 4)
    for (pixel in 0 until width * height) color.copyInto(bytes, pixel * 4)
    return bytes
}
