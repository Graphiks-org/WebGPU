@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu


/**
 * Represents a buffer of raw binary data, which can be used in various WebGPU operations.
 * Provides a mechanism to work directly with raw memory for performance-critical computations
 * or data manipulation when working with GPU resources.
 *
 * This class is an abstraction for handling binary data in a format compatible with WebGPU,
 * allowing interoperability with GPU resources like buffers and textures.
 */
expect sealed interface ArrayBuffer {
    /**
     * The size of the buffer in bytes.
     */
    val size: ULong

    // Read methods - convert entire buffer to typed arrays

    /**
     * Converts the buffer to a ByteArray.
     * @return a ByteArray containing the buffer's data
     */
    fun toByteArray(): ByteArray

    /**
     * Converts the buffer to a ShortArray.
     * @return a ShortArray containing the buffer's data (size must be multiple of 2)
     */
    fun toShortArray(): ShortArray

    /**
     * Converts the buffer to an IntArray.
     * @return an IntArray containing the buffer's data (size must be multiple of 4)
     */
    fun toIntArray(): IntArray

    /**
     * Converts the buffer to a FloatArray.
     * @return a FloatArray containing the buffer's data (size must be multiple of 4)
     */
    fun toFloatArray(): FloatArray

    /**
     * Converts the buffer to a DoubleArray.
     * @return a DoubleArray containing the buffer's data (size must be multiple of 8)
     */
    fun toDoubleArray(): DoubleArray

    /**
     * Converts the buffer to a UByteArray.
     * @return a UByteArray containing the buffer's data
     */
    fun toUByteArray(): UByteArray

    /**
     * Converts the buffer to a UShortArray.
     * @return a UShortArray containing the buffer's data (size must be multiple of 2)
     */
    fun toUShortArray(): UShortArray

    /**
     * Converts the buffer to a UIntArray.
     * @return a UIntArray containing the buffer's data (size must be multiple of 4)
     */
    fun toUIntArray(): UIntArray

    // Indexed read methods

    /**
     * Reads a byte at the specified offset.
     * @param offset the byte offset
     * @return the byte value
     */
    fun getByte(offset: ULong): Byte

    /**
     * Reads a short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @return the short value
     */
    fun getShort(offset: ULong): Short

    /**
     * Reads an int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @return the int value
     */
    fun getInt(offset: ULong): Int

    /**
     * Reads a float at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @return the float value
     */
    fun getFloat(offset: ULong): Float

    /**
     * Reads a double at the specified offset.
     * @param offset the byte offset (must be aligned to 8 bytes)
     * @return the double value
     */
    fun getDouble(offset: ULong): Double

    /**
     * Reads an unsigned byte at the specified offset.
     * @param offset the byte offset
     * @return the unsigned byte value
     */
    fun getUByte(offset: ULong): UByte

    /**
     * Reads an unsigned short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @return the unsigned short value
     */
    fun getUShort(offset: ULong): UShort

    /**
     * Reads an unsigned int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @return the unsigned int value
     */
    fun getUInt(offset: ULong): UInt

    // Indexed write methods

    /**
     * Writes a byte at the specified offset.
     * @param offset the byte offset
     * @param value the byte value to write
     */
    fun setByte(offset: ULong, value: Byte)

    /**
     * Writes a short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @param value the short value to write
     */
    fun setShort(offset: ULong, value: Short)

    /**
     * Writes an int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @param value the int value to write
     */
    fun setInt(offset: ULong, value: Int)

    /**
     * Writes a float at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @param value the float value to write
     */
    fun setFloat(offset: ULong, value: Float)

    /**
     * Writes a double at the specified offset.
     * @param offset the byte offset (must be aligned to 8 bytes)
     * @param value the double value to write
     */
    fun setDouble(offset: ULong, value: Double)

    /**
     * Writes an unsigned byte at the specified offset.
     * @param offset the byte offset
     * @param value the unsigned byte value to write
     */
    fun setUByte(offset: ULong, value: UByte)

    /**
     * Writes an unsigned short at the specified offset.
     * @param offset the byte offset (must be aligned to 2 bytes)
     * @param value the unsigned short value to write
     */
    fun setUShort(offset: ULong, value: UShort)

    /**
     * Writes an unsigned int at the specified offset.
     * @param offset the byte offset (must be aligned to 4 bytes)
     * @param value the unsigned int value to write
     */
    fun setUInt(offset: ULong, value: UInt)

    // Array write methods

    /**
     * Writes a ByteArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing
     * @param array the byte array to write
     */
    fun setBytes(offset: ULong, array: ByteArray)

    /**
     * Writes a ShortArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 2 bytes)
     * @param array the short array to write
     */
    fun setShorts(offset: ULong, array: ShortArray)

    /**
     * Writes an IntArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 4 bytes)
     * @param array the int array to write
     */
    fun setInts(offset: ULong, array: IntArray)

    /**
     * Writes a FloatArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 4 bytes)
     * @param array the float array to write
     */
    fun setFloats(offset: ULong, array: FloatArray)

    /**
     * Writes a DoubleArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 8 bytes)
     * @param array the double array to write
     */
    fun setDoubles(offset: ULong, array: DoubleArray)

    /**
     * Writes a UByteArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing
     * @param array the unsigned byte array to write
     */
    fun setUBytes(offset: ULong, array: UByteArray)

    /**
     * Writes a UShortArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 2 bytes)
     * @param array the unsigned short array to write
     */
    fun setUShorts(offset: ULong, array: UShortArray)

    /**
     * Writes a UIntArray into the buffer at the specified offset.
     * @param offset the byte offset where to start writing (must be aligned to 4 bytes)
     * @param array the unsigned int array to write
     */
    fun setUInts(offset: ULong, array: UIntArray)

    companion object {
        /**
         * Allocates a new ArrayBuffer with the specified size in bytes.
         * The buffer is zero-initialized and memory is managed automatically.
         *
         * @param sizeInBytes the size of the buffer in bytes
         * @return a new ArrayBuffer with the specified size
         */
        fun allocate(sizeInBytes: ULong): ArrayBuffer

        /**
         * Creates an ArrayBuffer from a ByteArray.
         * @param array the byte array to convert
         * @return an ArrayBuffer containing the data from the byte array
         */
        fun of(array: ByteArray): ArrayBuffer

        /**
         * Creates an ArrayBuffer from a ShortArray.
         * @param array the short array to convert
         * @return an ArrayBuffer containing the data from the short array
         */
        fun of(array: ShortArray): ArrayBuffer

        /**
         * Creates an ArrayBuffer from an IntArray.
         * @param array the int array to convert
         * @return an ArrayBuffer containing the data from the int array
         */
        fun of(array: IntArray): ArrayBuffer

        /**
         * Creates an ArrayBuffer from a FloatArray.
         * @param array the float array to convert
         * @return an ArrayBuffer containing the data from the float array
         */
        fun of(array: FloatArray): ArrayBuffer

        /**
         * Creates an ArrayBuffer from a DoubleArray.
         * @param array the double array to convert
         * @return an ArrayBuffer containing the data from the double array
         */
        fun of(array: DoubleArray): ArrayBuffer

        /**
         * Creates an ArrayBuffer from a UByteArray.
         * @param array the unsigned byte array to convert
         * @return an ArrayBuffer containing the data from the unsigned byte array
         */
        fun of(array: UByteArray): ArrayBuffer

        /**
         * Creates an ArrayBuffer from a UShortArray.
         * @param array the unsigned short array to convert
         * @return an ArrayBuffer containing the data from the unsigned short array
         */
        fun of(array: UShortArray): ArrayBuffer

        /**
         * Creates an ArrayBuffer from a UIntArray.
         * @param array the unsigned int array to convert
         * @return an ArrayBuffer containing the data from the unsigned int array
         */
        fun of(array: UIntArray): ArrayBuffer

    }
}