@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

/**
 * A Kotlin/JS value class that serves as a wrapper for the JavaScript `ArrayBuffer`.
 *
 * This class allows for interoperability between Kotlin and JavaScript by embedding
 * the `js.buffer.ArrayBuffer` instance, enabling the handling of binary data in
 * scenarios such as Web API interactions, file operations, or low-level binary data
 * processing.
 *
 * @property buffer The underlying `js.buffer.ArrayBuffer` instance being wrapped.
 */
value class WebArrayBuffer internal constructor(val buffer: js.buffer.ArrayBuffer): ArrayBuffer {
    override val size: ULong
        get() = buffer.byteLength.toULong()

    // Read methods - convert entire buffer to typed arrays
    override fun toByteArray(): ByteArray = buffer.readByteArray()
    override fun toShortArray(): ShortArray = buffer.readShortArray()
    override fun toIntArray(): IntArray = buffer.readIntArray()
    override fun toFloatArray(): FloatArray = buffer.readFloatArray()
    override fun toDoubleArray(): DoubleArray = buffer.readDoubleArray()
    override fun toUByteArray(): UByteArray = buffer.readUByteArray()
    override fun toUShortArray(): UShortArray = buffer.readUShortArray()
    override fun toUIntArray(): UIntArray = buffer.readUIntArray()

    // Indexed read methods

    override fun getByte(offset: ULong): Byte {
        checkBufferRange(size, offset, Byte.SIZE_BYTES.toULong())
        return buffer.readByte(offset.toInt())
    }

    override fun getShort(offset: ULong): Short {
        checkBufferRange(size, offset, Short.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Short.SIZE_BYTES)
        return buffer.readShort(offset.toInt())
    }

    override fun getInt(offset: ULong): Int {
        checkBufferRange(size, offset, Int.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Int.SIZE_BYTES)
        return buffer.readInt(offset.toInt())
    }

    override fun getFloat(offset: ULong): Float {
        checkBufferRange(size, offset, Float.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Float.SIZE_BYTES)
        return buffer.readFloat(offset.toInt())
    }

    override fun getDouble(offset: ULong): Double {
        checkBufferRange(size, offset, Double.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Double.SIZE_BYTES)
        return buffer.readDouble(offset.toInt())
    }

    override fun getUByte(offset: ULong): UByte = getByte(offset).toUByte()
    override fun getUShort(offset: ULong): UShort = getShort(offset).toUShort()
    override fun getUInt(offset: ULong): UInt = getInt(offset).toUInt()

    // Indexed write methods

    override fun setByte(offset: ULong, value: Byte) {
        checkBufferRange(size, offset, Byte.SIZE_BYTES.toULong())
        buffer.writeByte(offset.toInt(), value)
    }

    override fun setShort(offset: ULong, value: Short) {
        checkBufferRange(size, offset, Short.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Short.SIZE_BYTES)
        buffer.writeShort(offset.toInt(), value)
    }

    override fun setInt(offset: ULong, value: Int) {
        checkBufferRange(size, offset, Int.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Int.SIZE_BYTES)
        buffer.writeInt(offset.toInt(), value)
    }

    override fun setFloat(offset: ULong, value: Float) {
        checkBufferRange(size, offset, Float.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Float.SIZE_BYTES)
        buffer.writeFloat(offset.toInt(), value)
    }

    override fun setDouble(offset: ULong, value: Double) {
        checkBufferRange(size, offset, Double.SIZE_BYTES.toULong())
        checkBufferAlignment(offset, Double.SIZE_BYTES)
        buffer.writeDouble(offset.toInt(), value)
    }

    override fun setUByte(offset: ULong, value: UByte) = setByte(offset, value.toByte())
    override fun setUShort(offset: ULong, value: UShort) = setShort(offset, value.toShort())
    override fun setUInt(offset: ULong, value: UInt) = setInt(offset, value.toInt())

    // Array write methods
    override fun setBytes(offset: ULong, array: ByteArray) = buffer.writeByteArray(offset.toInt(), array)
    override fun setShorts(offset: ULong, array: ShortArray) = buffer.writeShortArray(offset.toInt(), array)
    override fun setInts(offset: ULong, array: IntArray) = buffer.writeIntArray(offset.toInt(), array)
    override fun setFloats(offset: ULong, array: FloatArray) = buffer.writeFloatArray(offset.toInt(), array)
    override fun setDoubles(offset: ULong, array: DoubleArray) = buffer.writeDoubleArray(offset.toInt(), array)
    override fun setUBytes(offset: ULong, array: UByteArray) = buffer.writeUByteArray(offset.toInt(), array)
    override fun setUShorts(offset: ULong, array: UShortArray) = buffer.writeUShortArray(offset.toInt(), array)
    override fun setUInts(offset: ULong, array: UIntArray) = buffer.writeUIntArray(offset.toInt(), array)
}
