@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * A JVM-specific implementation of the `ArrayBuffer` interface.
 *
 * `JvmArrayBuffer` provides a lightweight wrapper around the `MemorySegment` class, allowing
 * JVM platforms to manage, access, and manipulate raw binary data in a way that conforms
 * to the `ArrayBuffer` abstraction.
 *
 * This class leverages the `@JvmInline` annotation, making it a value class. This ensures
 * minimal runtime overhead and allows the `MemorySegment` instance to be used with improved
 * performance due to inlining and reduced object allocations.
 *
 * @param buffer The underlying `MemorySegment` instance that serves as the basis for this array buffer.
 */
@JvmInline
value class JvmArrayBuffer internal constructor(val buffer: MemorySegment): ArrayBuffer {
    override val size: ULong
        get() = buffer.byteSize().toULong()

    // Read methods - convert entire buffer to typed arrays

    override fun toByteArray(): ByteArray {
        checkedArrayLength(size, Byte.SIZE_BYTES)
        return buffer.toArray(ValueLayout.JAVA_BYTE)
    }

    override fun toShortArray(): ShortArray {
        checkedArrayLength(size, Short.SIZE_BYTES)
        return buffer.toArray(ValueLayout.JAVA_SHORT_UNALIGNED)
    }

    override fun toIntArray(): IntArray {
        checkedArrayLength(size, Int.SIZE_BYTES)
        return buffer.toArray(ValueLayout.JAVA_INT_UNALIGNED)
    }

    override fun toFloatArray(): FloatArray {
        checkedArrayLength(size, Float.SIZE_BYTES)
        return buffer.toArray(ValueLayout.JAVA_FLOAT_UNALIGNED)
    }

    override fun toDoubleArray(): DoubleArray {
        checkedArrayLength(size, Double.SIZE_BYTES)
        return buffer.toArray(ValueLayout.JAVA_DOUBLE_UNALIGNED)
    }

    override fun toUByteArray(): UByteArray = toByteArray().asUByteArray()

    override fun toUShortArray(): UShortArray = toShortArray().asUShortArray()

    override fun toUIntArray(): UIntArray = toIntArray().asUIntArray()


    // Indexed read methods

    override fun getByte(offset: ULong): Byte {
        checkBufferRange(size, offset, Byte.SIZE_BYTES.toULong())
        return buffer.get(ValueLayout.JAVA_BYTE, offset.toLong())
    }

    override fun getShort(offset: ULong): Short {
        checkBufferRange(size, offset, Short.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Short.SIZE_BYTES)
        return buffer.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset.toLong())
    }

    override fun getInt(offset: ULong): Int {
        checkBufferRange(size, offset, Int.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Int.SIZE_BYTES)
        return buffer.get(ValueLayout.JAVA_INT_UNALIGNED, offset.toLong())
    }

    override fun getFloat(offset: ULong): Float {
        checkBufferRange(size, offset, Float.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Float.SIZE_BYTES)
        return buffer.get(ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong())
    }

    override fun getDouble(offset: ULong): Double {
        checkBufferRange(size, offset, Double.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Double.SIZE_BYTES)
        return buffer.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset.toLong())
    }

    override fun getUByte(offset: ULong): UByte = getByte(offset).toUByte()

    override fun getUShort(offset: ULong): UShort = getShort(offset).toUShort()

    override fun getUInt(offset: ULong): UInt = getInt(offset).toUInt()


    // Indexed write methods

    override fun setByte(offset: ULong, value: Byte) {
        checkBufferRange(size, offset, Byte.SIZE_BYTES.toULong())
        buffer.set(ValueLayout.JAVA_BYTE, offset.toLong(), value)
    }

    override fun setShort(offset: ULong, value: Short) {
        checkBufferRange(size, offset, Short.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Short.SIZE_BYTES)
        buffer.set(ValueLayout.JAVA_SHORT_UNALIGNED, offset.toLong(), value)
    }

    override fun setInt(offset: ULong, value: Int) {
        checkBufferRange(size, offset, Int.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Int.SIZE_BYTES)
        buffer.set(ValueLayout.JAVA_INT_UNALIGNED, offset.toLong(), value)
    }

    override fun setFloat(offset: ULong, value: Float) {
        checkBufferRange(size, offset, Float.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Float.SIZE_BYTES)
        buffer.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong(), value)
    }

    override fun setDouble(offset: ULong, value: Double) {
        checkBufferRange(size, offset, Double.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Double.SIZE_BYTES)
        buffer.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset.toLong(), value)
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
        if (checkedBulkBytes(size, offset, array.size, Byte.SIZE_BYTES) == 0uL) return
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_BYTE, offset.toLong(), array.size)
    }

    override fun setShorts(offset: ULong, array: ShortArray) {
        if (checkedBulkBytes(size, offset, array.size, Short.SIZE_BYTES) == 0uL) return
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_SHORT_UNALIGNED, offset.toLong(), array.size)
    }

    override fun setInts(offset: ULong, array: IntArray) {
        if (checkedBulkBytes(size, offset, array.size, Int.SIZE_BYTES) == 0uL) return
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_INT_UNALIGNED, offset.toLong(), array.size)
    }

    override fun setFloats(offset: ULong, array: FloatArray) {
        if (checkedBulkBytes(size, offset, array.size, Float.SIZE_BYTES) == 0uL) return
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong(), array.size)
    }

    override fun setDoubles(offset: ULong, array: DoubleArray) {
        if (checkedBulkBytes(size, offset, array.size, Double.SIZE_BYTES) == 0uL) return
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_DOUBLE_UNALIGNED, offset.toLong(), array.size)
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

}