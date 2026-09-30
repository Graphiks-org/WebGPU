package org.graphiks.webgpu

/**
 * Internal arithmetic guards for [ArrayBuffer]. They keep size and offset conversions explicit and
 * perform no allocation on the valid path; every message is built only when a check fails.
 *
 * Validation order at an access site is: range, then alignment for a non-empty operation, then the
 * numeric conversion, then the underlying access.
 */

/**
 * Multiplies [count] elements by [width] bytes, refusing a result larger than [maximum]. The
 * comparison runs before the multiplication so an overflowing product is never evaluated.
 */
internal fun checkedByteCount(count: ULong, width: Int, maximum: ULong): ULong {
    require(width > 0) { "Element width must be positive: $width" }
    val elementWidth = width.toULong()
    require(count <= maximum / elementWidth) { "Byte count exceeds maximum $maximum" }
    return count * elementWidth
}

/** Narrows a byte size to [Int], refusing values a 32-bit signed size cannot hold. */
internal fun checkedIntSize(size: ULong): Int {
    require(size <= Int.MAX_VALUE.toULong()) { "Size $size exceeds Int.MAX_VALUE" }
    return size.toInt()
}

/** Narrows a byte size to [Long], refusing values a 64-bit signed size cannot hold. */
internal fun checkedLongSize(size: ULong): Long {
    require(size <= Long.MAX_VALUE.toULong()) { "Size $size exceeds Long.MAX_VALUE" }
    return size.toLong()
}

/**
 * Accepts a range when `offset <= size && length <= size - offset`. The subtraction is safe because
 * the first clause is checked first, so `offset + length` is never evaluated in unsigned arithmetic.
 */
internal fun checkBufferRange(size: ULong, offset: ULong, length: ULong) {
    if (offset > size || length > size - offset) {
        throw IndexOutOfBoundsException("Range offset=$offset length=$length exceeds size=$size")
    }
}

/** Requires a non-empty operation's [offset] to be a multiple of the element [width]. */
internal fun checkBufferAlignment(offset: ULong, width: Int) {
    require(width > 0) { "Element width must be positive: $width" }
    require(offset % width.toULong() == 0uL) { "Offset $offset is not aligned to $width bytes" }
}

/**
 * Number of elements of [width] bytes that fit in [size], refusing a non-divisible size and an
 * element count a Kotlin array cannot hold.
 */
internal fun checkedArrayLength(size: ULong, width: Int): Int {
    require(width > 0) { "Element width must be positive: $width" }
    val elementWidth = width.toULong()
    require(size % elementWidth == 0uL) { "Size $size is not divisible by $width" }
    return checkedIntSize(size / elementWidth)
}
