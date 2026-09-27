@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

/**
 * A platform-independent representation of a fixed-length raw binary data buffer.
 *
 * The `ArrayBuffer` interface provides the ability to efficiently handle and store
 * binary data in memory, often as the underlying storage for typed arrays. It is
 * immutable, meaning its size cannot be adjusted once created.
 *
 * This abstraction is commonly used for processing low-level binary data, enabling
 * tasks such as file handling, network communication, or interfacing with Web APIs
 * that require binary data storage.
 */
actual sealed interface ArrayBuffer {
    /**
     * The size of the buffer in bytes.
     */
    actual val size: ULong

    // Read methods - convert entire buffer to typed arrays

    /**
     * Converts the buffer to a ByteArray.
     * @return a ByteArray containing the buffer's data
     */
    actual fun toByteArray(): ByteArray

    /**
     * Converts the buffer to a ShortArray.
     * @return a ShortArray containing the buffer's data (size must be multiple of 2)
     */
    actual fun toShortArray(): ShortArray

    /**
     * Converts the buffer to an IntArray.
     * @return an IntArray containing the buffer's data (size must be multiple of 4)
     */
    actual fun toIntArray(): IntArray

    /**
     * Converts the buffer to a FloatArray.
     * @return a FloatArray containing the buffer's data (size must be multiple of 4)
     */
    actual fun toFloatArray(): FloatArray

    /**
     * Converts the buffer to a DoubleArray.
     * @return a DoubleArray containing the buffer's data (size must be multiple of 8)
     */
    actual fun toDoubleArray(): DoubleArray

    /**
     * Converts the buffer to a UByteArray.
     * @return a UByteArray containing the buffer's data
     */
    actual fun toUByteArray(): UByteArray

    /**
     * Converts the buffer to a UShortArray.
     * @return a UShortArray containing the buffer's data (size must be multiple of 2)
     */
    actual fun toUShortArray(): UShortArray

    /**
     * Converts the buffer to a UIntArray.
     * @return a UIntArray containing the buffer's data (size must be multiple of 4)
     */
    actual fun toUIntArray(): UIntArray

    // Indexed read methods

    /**
     * Reads a byte at the specified offset.
     * @param offset the byte offset
     * @return the byte value
     */
    actual fun getByte(offset: ULong): Byte

    /**
     * Reads a short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @return the short value
     */
    actual fun getShort(offset: ULong): Short

    /**
     * Reads an int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @return the int value
     */
    actual fun getInt(offset: ULong): Int

    /**
     * Reads a float at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @return the float value
     */
    actual fun getFloat(offset: ULong): Float

    /**
     * Reads a double at the specified offset.
     * @param offset the byte offset (must be aligned to 8 bytes)
     * @return the double value
     */
    actual fun getDouble(offset: ULong): Double

    /**
     * Reads an unsigned byte at the specified offset.
     * @param offset the byte offset
     * @return the unsigned byte value
     */
    actual fun getUByte(offset: ULong): UByte

    /**
     * Reads an unsigned short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @return the unsigned short value
     */
    actual fun getUShort(offset: ULong): UShort

    /**
     * Reads an unsigned int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @return the unsigned int value
     */
    actual fun getUInt(offset: ULong): UInt

    // Indexed write methods

    /**
     * Writes a byte at the specified offset.
     * @param offset the byte offset
     * @param value the byte value to write
     */
    actual fun setByte(offset: ULong, value: Byte)

    /**
     * Writes a short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @param value the short value to write
     */
    actual fun setShort(offset: ULong, value: Short)

    /**
     * Writes an int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @param value the int value to write
     */
    actual fun setInt(offset: ULong, value: Int)

    /**
     * Writes a float at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @param value the float value to write
     */
    actual fun setFloat(offset: ULong, value: Float)

    /**
     * Writes a double at the specified offset.
     * @param offset the byte offset (must be aligned to 8 bytes)
     * @param value the double value to write
     */
    actual fun setDouble(offset: ULong, value: Double)

    /**
     * Writes an unsigned byte at the specified offset.
     * @param offset the byte offset
     * @param value the unsigned byte value to write
     */
    actual fun setUByte(offset: ULong, value: UByte)

    /**
     * Writes an unsigned short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @param value the unsigned short value to write
     */
    actual fun setUShort(offset: ULong, value: UShort)

    /**
     * Writes an unsigned int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @param value the unsigned int value to write
     */
    actual fun setUInt(offset: ULong, value: UInt)

    // Array write methods

    /**
     * Writes a ByteArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing
     * @param array the byte array to write
     */
    actual fun setBytes(offset: ULong, array: ByteArray)

    /**
     * Writes a ShortArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 2 bytes)
     * @param array the short array to write
     */
    actual fun setShorts(offset: ULong, array: ShortArray)

    /**
     * Writes an IntArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 4 bytes)
     * @param array the int array to write
     */
    actual fun setInts(offset: ULong, array: IntArray)

    /**
     * Writes a FloatArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 4 bytes)
     * @param array the float array to write
     */
    actual fun setFloats(offset: ULong, array: FloatArray)

    /**
     * Writes a DoubleArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 8 bytes)
     * @param array the double array to write
     */
    actual fun setDoubles(offset: ULong, array: DoubleArray)

    /**
     * Writes a UByteArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing
     * @param array the unsigned byte array to write
     */
    actual fun setUBytes(offset: ULong, array: UByteArray)

    /**
     * Writes a UShortArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 2 bytes)
     * @param array the unsigned short array to write
     */
    actual fun setUShorts(offset: ULong, array: UShortArray)

    /**
     * Writes a UIntArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 4 bytes)
     * @param array the unsigned int array to write
     */
    actual fun setUInts(offset: ULong, array: UIntArray)

    actual companion object {
        /**
         * Allocates a new ArrayBuffer with the specified size in bytes.
         * The buffer is zero-initialized and memory is managed automatically.
         *
         * @param sizeInBytes the size of the buffer in bytes
         * @return a new ArrayBuffer with the specified size
         */
        actual fun allocate(sizeInBytes: ULong): ArrayBuffer {
            return WebArrayBuffer(js.buffer.ArrayBuffer(sizeInBytes.toInt()))
        }

        /**
         * Creates an ArrayBuffer from a JavaScript ArrayBuffer.
         * @param buffer the JavaScript array buffer to wrap
         * @return an ArrayBuffer backed by the JavaScript array buffer
         */
        fun wrap(buffer: js.buffer.ArrayBuffer)
            = WebArrayBuffer(buffer)

        /**
         * Creates an ArrayBuffer from a ByteArray.
         * @param array the byte array to convert
         * @return an ArrayBuffer containing the data from the byte array
         */
        actual fun of(array: ByteArray): ArrayBuffer
            = array.toArrayBuffer()

        /**
         * Creates an ArrayBuffer from a ShortArray.
         * @param array the short array to convert
         * @return an ArrayBuffer containing the data from the short array
         */
        actual fun of(array: ShortArray): ArrayBuffer
            = array.toArrayBuffer()

        /**
         * Creates an ArrayBuffer from an IntArray.
         * @param array the int array to convert
         * @return an ArrayBuffer containing the data from the int array
         */
        actual fun of(array: IntArray): ArrayBuffer
            = array.toArrayBuffer()

        /**
         * Creates an ArrayBuffer from a FloatArray.
         * @param array the float array to convert
         * @return an ArrayBuffer containing the data from the float array
         */
        actual fun of(array: FloatArray): ArrayBuffer
            = array.toArrayBuffer()

        /**
         * Creates an ArrayBuffer from a DoubleArray.
         * @param array the double array to convert
         * @return an ArrayBuffer containing the data from the double array
         */
        actual fun of(array: DoubleArray): ArrayBuffer
            = array.toArrayBuffer()

        /**
         * Creates an ArrayBuffer from a UByteArray.
         * @param array the unsigned byte array to convert
         * @return an ArrayBuffer containing the data from the unsigned byte array
         */
        actual fun of(array: UByteArray): ArrayBuffer
            = array.toArrayBuffer()

        /**
         * Creates an ArrayBuffer from a UShortArray.
         * @param array the unsigned short array to convert
         * @return an ArrayBuffer containing the data from the unsigned short array
         */
        actual fun of(array: UShortArray): ArrayBuffer
            = array.toArrayBuffer()

        /**
         * Creates an ArrayBuffer from a UIntArray.
         * @param array the unsigned int array to convert
         * @return an ArrayBuffer containing the data from the unsigned int array
         */
        actual fun of(array: UIntArray): ArrayBuffer
            = array.toArrayBuffer()
    }
}

internal expect inline fun ByteArray.toArrayBuffer(): ArrayBuffer
internal expect inline fun ShortArray.toArrayBuffer(): ArrayBuffer
internal expect inline fun IntArray.toArrayBuffer(): ArrayBuffer
internal expect inline fun FloatArray.toArrayBuffer(): ArrayBuffer
internal expect inline fun DoubleArray.toArrayBuffer(): ArrayBuffer
internal expect inline fun UByteArray.toArrayBuffer(): ArrayBuffer
internal expect inline fun UShortArray.toArrayBuffer(): ArrayBuffer
internal expect inline fun UIntArray.toArrayBuffer(): ArrayBuffer

// Platform-specific extension methods
internal expect fun js.buffer.ArrayBuffer.readByteArray(): ByteArray
internal expect fun js.buffer.ArrayBuffer.readShortArray(): ShortArray
internal expect fun js.buffer.ArrayBuffer.readIntArray(): IntArray
internal expect fun js.buffer.ArrayBuffer.readFloatArray(): FloatArray
internal expect fun js.buffer.ArrayBuffer.readDoubleArray(): DoubleArray
internal expect fun js.buffer.ArrayBuffer.readUByteArray(): UByteArray
internal expect fun js.buffer.ArrayBuffer.readUShortArray(): UShortArray
internal expect fun js.buffer.ArrayBuffer.readUIntArray(): UIntArray

internal expect fun js.buffer.ArrayBuffer.readByte(offset: Int): Byte
internal expect fun js.buffer.ArrayBuffer.readShort(offset: Int): Short
internal expect fun js.buffer.ArrayBuffer.readInt(offset: Int): Int
internal expect fun js.buffer.ArrayBuffer.readFloat(offset: Int): Float
internal expect fun js.buffer.ArrayBuffer.readDouble(offset: Int): Double
internal expect fun js.buffer.ArrayBuffer.readUByte(offset: Int): UByte
internal expect fun js.buffer.ArrayBuffer.readUShort(offset: Int): UShort
internal expect fun js.buffer.ArrayBuffer.readUInt(offset: Int): UInt

internal expect fun js.buffer.ArrayBuffer.writeByte(offset: Int, value: Byte)
internal expect fun js.buffer.ArrayBuffer.writeShort(offset: Int, value: Short)
internal expect fun js.buffer.ArrayBuffer.writeInt(offset: Int, value: Int)
internal expect fun js.buffer.ArrayBuffer.writeFloat(offset: Int, value: Float)
internal expect fun js.buffer.ArrayBuffer.writeDouble(offset: Int, value: Double)
internal expect fun js.buffer.ArrayBuffer.writeUByte(offset: Int, value: UByte)
internal expect fun js.buffer.ArrayBuffer.writeUShort(offset: Int, value: UShort)
internal expect fun js.buffer.ArrayBuffer.writeUInt(offset: Int, value: UInt)

internal expect fun js.buffer.ArrayBuffer.writeByteArray(offset: Int, array: ByteArray)
internal expect fun js.buffer.ArrayBuffer.writeShortArray(offset: Int, array: ShortArray)
internal expect fun js.buffer.ArrayBuffer.writeIntArray(offset: Int, array: IntArray)
internal expect fun js.buffer.ArrayBuffer.writeFloatArray(offset: Int, array: FloatArray)
internal expect fun js.buffer.ArrayBuffer.writeDoubleArray(offset: Int, array: DoubleArray)
internal expect fun js.buffer.ArrayBuffer.writeUByteArray(offset: Int, array: UByteArray)
internal expect fun js.buffer.ArrayBuffer.writeUShortArray(offset: Int, array: UShortArray)
internal expect fun js.buffer.ArrayBuffer.writeUIntArray(offset: Int, array: UIntArray)

