@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import java.nio.ByteBuffer
import java.nio.ByteOrder
import sun.misc.Unsafe

/**
 * An [ArrayBuffer] view over borrowed native memory, for Android.
 *
 * The view reads and writes the raw memory at [address] directly through the
 * platform's [Unsafe] accessors — it never copies and never frees: the caller
 * keeps owning the memory and must keep it valid for the whole lifetime of the
 * view. The intended use is a range a native library lends the caller for a
 * bounded time, such as a GPU-mapped buffer between map and unmap.
 *
 * Multi-byte accessors use the platform's native byte order, matching the raw
 * layout the native memory exposes. Offsets are checked against [size], so an
 * out-of-range access throws [IndexOutOfBoundsException] instead of reaching
 * outside the borrowed range.
 *
 * @param address the native address of the borrowed memory; must be non-null
 * when [size] is non-zero
 * @param size the size of the borrowed range in bytes
 * @throws IllegalStateException when a non-zero size is paired with a null address
 */
@Suppress("DEPRECATION_ERROR", "DEPRECATION") // sun.misc.Unsafe accessors are deprecated in recent JDKs
internal class BorrowedNativeArrayBuffer(
    private val address: Long,
    override val size: ULong,
) : ArrayBuffer {

    init {
        if (size > 0uL && address == 0L) error("borrowed memory address must not be null")
    }

    // Read methods - convert entire buffer to typed arrays

    override fun toByteArray(): ByteArray {
        val array = ByteArray(size.toInt())
        readBytes(0uL, array, 0, array.size)
        return array
    }

    override fun toShortArray(): ShortArray {
        val shorts = ShortArray((size / Short.SIZE_BYTES.toULong()).toInt())
        val bytes = ByteArray(shorts.size * Short.SIZE_BYTES)
        readBytes(0uL, bytes, 0, bytes.size)
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts)
        return shorts
    }

    override fun toIntArray(): IntArray {
        val ints = IntArray((size / Int.SIZE_BYTES.toULong()).toInt())
        val bytes = ByteArray(ints.size * Int.SIZE_BYTES)
        readBytes(0uL, bytes, 0, bytes.size)
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asIntBuffer().get(ints)
        return ints
    }

    override fun toFloatArray(): FloatArray {
        val floats = FloatArray((size / Float.SIZE_BYTES.toULong()).toInt())
        val bytes = ByteArray(floats.size * Float.SIZE_BYTES)
        readBytes(0uL, bytes, 0, bytes.size)
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer().get(floats)
        return floats
    }

    override fun toDoubleArray(): DoubleArray {
        val doubles = DoubleArray((size / Double.SIZE_BYTES.toULong()).toInt())
        val bytes = ByteArray(doubles.size * Double.SIZE_BYTES)
        readBytes(0uL, bytes, 0, bytes.size)
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asDoubleBuffer().get(doubles)
        return doubles
    }

    override fun toUByteArray(): UByteArray = toByteArray().asUByteArray()

    override fun toUShortArray(): UShortArray = toShortArray().asUShortArray()

    override fun toUIntArray(): UIntArray = toIntArray().asUIntArray()

    // Indexed read methods

    override fun getByte(offset: ULong): Byte {
        checkOffset(offset, Byte.SIZE_BYTES)
        return unsafe.getByte(address + offset.toLong())
    }

    override fun getShort(offset: ULong): Short {
        checkOffset(offset, Short.SIZE_BYTES)
        return unsafe.getShort(address + offset.toLong())
    }

    override fun getInt(offset: ULong): Int {
        checkOffset(offset, Int.SIZE_BYTES)
        return unsafe.getInt(address + offset.toLong())
    }

    override fun getFloat(offset: ULong): Float {
        checkOffset(offset, Float.SIZE_BYTES)
        return unsafe.getFloat(address + offset.toLong())
    }

    override fun getDouble(offset: ULong): Double {
        checkOffset(offset, Double.SIZE_BYTES)
        return unsafe.getDouble(address + offset.toLong())
    }

    override fun getUByte(offset: ULong): UByte = getByte(offset).toUByte()

    override fun getUShort(offset: ULong): UShort = getShort(offset).toUShort()

    override fun getUInt(offset: ULong): UInt = getInt(offset).toUInt()

    // Indexed write methods

    override fun setByte(offset: ULong, value: Byte) {
        checkOffset(offset, Byte.SIZE_BYTES)
        unsafe.putByte(address + offset.toLong(), value)
    }

    override fun setShort(offset: ULong, value: Short) {
        checkOffset(offset, Short.SIZE_BYTES)
        unsafe.putShort(address + offset.toLong(), value)
    }

    override fun setInt(offset: ULong, value: Int) {
        checkOffset(offset, Int.SIZE_BYTES)
        unsafe.putInt(address + offset.toLong(), value)
    }

    override fun setFloat(offset: ULong, value: Float) {
        checkOffset(offset, Float.SIZE_BYTES)
        unsafe.putFloat(address + offset.toLong(), value)
    }

    override fun setDouble(offset: ULong, value: Double) {
        checkOffset(offset, Double.SIZE_BYTES)
        unsafe.putDouble(address + offset.toLong(), value)
    }

    override fun setUByte(offset: ULong, value: UByte) {
        setByte(offset, value.toByte())
    }

    override fun setUShort(offset: ULong, value: UShort) {
        setShort(offset, value.toShort())
    }

    override fun setUInt(offset: ULong, value: UInt) {
        setInt(offset, value.toInt())
    }

    // Array write methods

    override fun setBytes(offset: ULong, array: ByteArray) {
        writeBytes(offset, array, 0, array.size)
    }

    override fun setShorts(offset: ULong, array: ShortArray) {
        for ((index, value) in array.withIndex()) {
            setShort(offset + index.toULong() * Short.SIZE_BYTES.toULong(), value)
        }
    }

    override fun setInts(offset: ULong, array: IntArray) {
        for ((index, value) in array.withIndex()) {
            setInt(offset + index.toULong() * Int.SIZE_BYTES.toULong(), value)
        }
    }

    override fun setFloats(offset: ULong, array: FloatArray) {
        for ((index, value) in array.withIndex()) {
            setFloat(offset + index.toULong() * Float.SIZE_BYTES.toULong(), value)
        }
    }

    override fun setDoubles(offset: ULong, array: DoubleArray) {
        for ((index, value) in array.withIndex()) {
            setDouble(offset + index.toULong() * Double.SIZE_BYTES.toULong(), value)
        }
    }

    override fun setUBytes(offset: ULong, array: UByteArray) {
        setBytes(offset, array.asByteArray())
    }

    override fun setUShorts(offset: ULong, array: UShortArray) {
        setShorts(offset, array.asShortArray())
    }

    override fun setUInts(offset: ULong, array: UIntArray) {
        setInts(offset, array.asIntArray())
    }

    /**
     * Refuses any offset that would let an access of [width] bytes start
     * outside the borrowed range. The comparison never adds the width to the
     * offset, so it cannot wrap around `ULong` arithmetic.
     */
    private fun checkOffset(offset: ULong, width: Int) {
        if (offset >= size) {
            throw IndexOutOfBoundsException("offset $offset is outside the borrowed range of $size bytes")
        }
        if (size - offset < width.toULong()) {
            throw IndexOutOfBoundsException(
                "an access of $width bytes at offset $offset crosses the end of the borrowed $size-byte range",
            )
        }
    }

    /** Bulk-reads [length] bytes into [destination] starting at [destinationIndex]. */
    private fun readBytes(offset: ULong, destination: ByteArray, destinationIndex: Int, length: Int) {
        if (length == 0) return
        checkOffset(offset, length)
        unsafe.copyMemory(
            null,
            address + offset.toLong(),
            destination,
            Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong() + destinationIndex.toLong(),
            length.toLong(),
        )
    }

    /** Bulk-writes [length] bytes from [source] starting at [sourceIndex]. */
    private fun writeBytes(offset: ULong, source: ByteArray, sourceIndex: Int, length: Int) {
        if (length == 0) return
        checkOffset(offset, length)
        unsafe.copyMemory(
            source,
            Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong() + sourceIndex.toLong(),
            null,
            address + offset.toLong(),
            length.toLong(),
        )
    }

    private companion object {

        /** The platform [Unsafe] handle; Android tolerates this well-known access. */
        @Suppress("UNCHECKED_CAST")
        private val unsafe: Unsafe = try {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            field.get(null) as Unsafe
        } catch (failure: ReflectiveOperationException) {
            throw IllegalStateException("sun.misc.Unsafe is not accessible on this platform", failure)
        }
    }
}
