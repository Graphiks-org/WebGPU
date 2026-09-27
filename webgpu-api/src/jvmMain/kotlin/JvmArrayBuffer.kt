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

    override fun toByteArray(): ByteArray = buffer.toArray(ValueLayout.JAVA_BYTE)

    override fun toShortArray(): ShortArray = buffer.toArray(ValueLayout.JAVA_SHORT_UNALIGNED)

    override fun toIntArray(): IntArray = buffer.toArray(ValueLayout.JAVA_INT_UNALIGNED)


    override fun toFloatArray(): FloatArray = buffer.toArray(ValueLayout.JAVA_FLOAT_UNALIGNED)

    override fun toDoubleArray(): DoubleArray = buffer.toArray(ValueLayout.JAVA_DOUBLE_UNALIGNED)

    override fun toUByteArray(): UByteArray = buffer.toArray(ValueLayout.JAVA_BYTE).asUByteArray()

    override fun toUShortArray(): UShortArray = buffer.toArray(ValueLayout.JAVA_SHORT_UNALIGNED).asUShortArray()

    override fun toUIntArray(): UIntArray = buffer.toArray(ValueLayout.JAVA_INT_UNALIGNED).asUIntArray()


    // Indexed read methods

    override fun getByte(offset: ULong): Byte = buffer.get(ValueLayout.JAVA_BYTE, offset.toLong())

    override fun getShort(offset: ULong): Short = buffer.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset.toLong())

    override fun getInt(offset: ULong): Int = buffer.get(ValueLayout.JAVA_INT_UNALIGNED, offset.toLong())


    override fun getFloat(offset: ULong): Float = buffer.get(ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong())

    override fun getDouble(offset: ULong): Double = buffer.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset.toLong())

    override fun getUByte(offset: ULong): UByte = getByte(offset).toUByte()

    override fun getUShort(offset: ULong): UShort = getShort(offset).toUShort()

    override fun getUInt(offset: ULong): UInt = getInt(offset).toUInt()


    // Indexed write methods

    override fun setByte(offset: ULong, value: Byte) {
        buffer.set(ValueLayout.JAVA_BYTE, offset.toLong(), value)
    }

    override fun setShort(offset: ULong, value: Short) {
        buffer.set(ValueLayout.JAVA_SHORT_UNALIGNED, offset.toLong(), value)
    }

    override fun setInt(offset: ULong, value: Int) {
        buffer.set(ValueLayout.JAVA_INT_UNALIGNED, offset.toLong(), value)
    }


    override fun setFloat(offset: ULong, value: Float) {
        buffer.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong(), value)
    }

    override fun setDouble(offset: ULong, value: Double) {
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
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_BYTE, offset.toLong(), array.size)
    }

    override fun setShorts(offset: ULong, array: ShortArray) {
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_SHORT_UNALIGNED, offset.toLong(), array.size)
    }

    override fun setInts(offset: ULong, array: IntArray) {
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_INT_UNALIGNED, offset.toLong(), array.size)
    }

    override fun setFloats(offset: ULong, array: FloatArray) {
        MemorySegment.copy(array, 0, buffer, ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong(), array.size)
    }

    override fun setDoubles(offset: ULong, array: DoubleArray) {
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