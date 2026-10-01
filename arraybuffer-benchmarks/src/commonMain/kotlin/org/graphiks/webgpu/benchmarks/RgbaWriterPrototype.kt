package org.graphiks.webgpu.benchmarks

/**
 * Bytes touched by a `width x height` RGBA8 image laid out with `rowStride` bytes per row. The
 * trailing padding of the last row is not written; a zero dimension touches nothing.
 */
internal fun rgbaTouchedBytes(width: Int, height: Int, rowStride: Int): Long {
    require(width >= 0 && height >= 0 && rowStride >= 0) { "Layout must not be negative" }
    require(rowStride.toLong() >= width.toLong() * 4) {
        "rowStride $rowStride is smaller than a $width-pixel row"
    }
    if (width == 0 || height == 0) return 0
    return (height - 1).toLong() * rowStride + width.toLong() * 4
}

/** Local guard: the `webgpu-api` helpers are internal and must not be made public for the harness. */
internal fun checkWriterRange(size: ULong, offset: ULong, length: ULong) {
    if (offset > size || length > size - offset) {
        throw IndexOutOfBoundsException("Writer range offset=$offset length=$length exceeds size=$size")
    }
}

/**
 * Prevalidates the whole layout once, then performs the pixel stores unchecked. It exposes no
 * handle and no user callback, so no arbitrary index can be reached.
 */
internal fun fillRgbaPrototype(
    memory: BenchmarkMemory,
    base: Int,
    width: Int,
    height: Int,
    rowStride: Int,
    seed: Int,
) {
    require(base >= 0) { "base must not be negative" }
    val touched = rgbaTouchedBytes(width, height, rowStride)
    checkWriterRange(memory.buffer.size, base.toULong(), touched.toULong())
    if (touched == 0L) return
    fillRgbaUnchecked(memory, base, width, height, rowStride, seed)
}

internal expect fun fillRgbaUnchecked(
    memory: BenchmarkMemory,
    base: Int,
    width: Int,
    height: Int,
    rowStride: Int,
    seed: Int,
)
