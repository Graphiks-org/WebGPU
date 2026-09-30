@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import java.nio.ByteBuffer

/**
 * An Android-specific implementation of the `ArrayBuffer` interface.
 *
 * `AndroidArrayBuffer` provides a lightweight wrapper around a direct `ByteBuffer`,
 * allowing Android platforms to manage, access, and manipulate raw binary data in a way
 * that conforms to the `ArrayBuffer` abstraction.
 *
 * This class leverages the `@JvmInline` annotation, making it a value class. This ensures
 * minimal runtime overhead and allows the `ByteBuffer` instance to be used with improved
 * performance due to inlining and reduced object allocations.
 *
 * @param buffer The underlying direct `ByteBuffer` instance that serves as the basis for this array buffer.
 * @throws IllegalStateException if the provided ByteBuffer is not a direct buffer
 */
@JvmInline
value class AndroidArrayBuffer internal constructor(val buffer: ByteBuffer): ArrayBuffer {
    init {
        if (buffer.isDirect.not()) error("ByteBuffer must be direct")
    }

    override val size: ULong
        get() = buffer.capacity().toULong()

    // Read methods - convert entire buffer to typed arrays

    override fun toByteArray(): ByteArray {
        val array = ByteArray(buffer.capacity())
        buffer.duplicate().get(array)
        return array
    }

    override fun toShortArray(): ShortArray {
        val array = ShortArray(buffer.capacity() / Short.SIZE_BYTES)
        buffer.duplicate().asShortBuffer().get(array)
        return array
    }

    override fun toIntArray(): IntArray {
        val array = IntArray(buffer.capacity() / Int.SIZE_BYTES)
        buffer.duplicate().asIntBuffer().get(array)
        return array
    }

    override fun toFloatArray(): FloatArray {
        val array = FloatArray(buffer.capacity() / Float.SIZE_BYTES)
        buffer.duplicate().asFloatBuffer().get(array)
        return array
    }

    override fun toDoubleArray(): DoubleArray {
        val array = DoubleArray(buffer.capacity() / Double.SIZE_BYTES)
        buffer.duplicate().asDoubleBuffer().get(array)
        return array
    }

    override fun toUByteArray(): UByteArray = toByteArray().asUByteArray()

    override fun toUShortArray(): UShortArray = toShortArray().asUShortArray()

    override fun toUIntArray(): UIntArray = toIntArray().asUIntArray()

    // Indexed read methods

    override fun getByte(offset: ULong): Byte {
        checkBufferRange(size, offset, Byte.SIZE_BYTES.toULong())
        return buffer.get(offset.toInt())
    }

    override fun getShort(offset: ULong): Short {
        checkBufferRange(size, offset, Short.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Short.SIZE_BYTES)
        return buffer.getShort(offset.toInt())
    }

    override fun getInt(offset: ULong): Int {
        checkBufferRange(size, offset, Int.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Int.SIZE_BYTES)
        return buffer.getInt(offset.toInt())
    }

    override fun getFloat(offset: ULong): Float {
        checkBufferRange(size, offset, Float.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Float.SIZE_BYTES)
        return buffer.getFloat(offset.toInt())
    }

    override fun getDouble(offset: ULong): Double {
        checkBufferRange(size, offset, Double.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Double.SIZE_BYTES)
        return buffer.getDouble(offset.toInt())
    }

    override fun getUByte(offset: ULong): UByte = getByte(offset).toUByte()

    override fun getUShort(offset: ULong): UShort = getShort(offset).toUShort()

    override fun getUInt(offset: ULong): UInt = getInt(offset).toUInt()

    // Indexed write methods

    override fun setByte(offset: ULong, value: Byte) {
        checkBufferRange(size, offset, Byte.SIZE_BYTES.toULong())
        buffer.put(offset.toInt(), value)
    }

    override fun setShort(offset: ULong, value: Short) {
        checkBufferRange(size, offset, Short.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Short.SIZE_BYTES)
        buffer.putShort(offset.toInt(), value)
    }

    override fun setInt(offset: ULong, value: Int) {
        checkBufferRange(size, offset, Int.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Int.SIZE_BYTES)
        buffer.putInt(offset.toInt(), value)
    }

    override fun setFloat(offset: ULong, value: Float) {
        checkBufferRange(size, offset, Float.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Float.SIZE_BYTES)
        buffer.putFloat(offset.toInt(), value)
    }

    override fun setDouble(offset: ULong, value: Double) {
        checkBufferRange(size, offset, Double.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Double.SIZE_BYTES)
        buffer.putDouble(offset.toInt(), value)
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
        val duplicate = buffer.duplicate()
        duplicate.position(offset.toInt())
        duplicate.put(array)
    }

    override fun setShorts(offset: ULong, array: ShortArray) {
        val duplicate = buffer.duplicate()
        duplicate.position(offset.toInt())
        duplicate.asShortBuffer().put(array)
    }

    override fun setInts(offset: ULong, array: IntArray) {
        val duplicate = buffer.duplicate()
        duplicate.position(offset.toInt())
        duplicate.asIntBuffer().put(array)
    }

    override fun setFloats(offset: ULong, array: FloatArray) {
        val duplicate = buffer.duplicate()
        duplicate.position(offset.toInt())
        duplicate.asFloatBuffer().put(array)
    }

    override fun setDoubles(offset: ULong, array: DoubleArray) {
        val duplicate = buffer.duplicate()
        duplicate.position(offset.toInt())
        duplicate.asDoubleBuffer().put(array)
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