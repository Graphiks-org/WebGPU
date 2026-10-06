@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import java.nio.ByteBuffer

/**
 * Represents a platform-specific abstraction for handling raw binary data buffers.
 *
 * `ArrayBuffer` is a sealed interface that provides a common type for working with
 * binary data across multiple platforms. It is typically associated with use cases
 * such as data transfer, WebGPU operations, or interfacing with native libraries.
 *
 * This interface is intended to be implemented by platform-specific classes
 * or value types that wrap the underlying buffer implementation, such as direct
 * `ByteBuffer` on Android.
 *
 * Example usage:
 * ```kotlin
 * val buffer = ArrayBuffer.from(byteArrayOf(1, 2, 3, 4))
 * val intBuffer = ArrayBuffer.from(intArrayOf(100, 200, 300))
 * ```
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
            val buffer = ByteBuffer.allocateDirect(sizeInBytes.toInt())
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from a ByteBuffer.
         * @param buffer the byte buffer to wrap (must be a direct buffer)
         * @return an ArrayBuffer backed by the byte buffer
         */
        fun wrap(buffer: ByteBuffer): ArrayBuffer = AndroidArrayBuffer(buffer)

        /**
         * Wraps borrowed native memory as an ArrayBuffer.
         *
         * The returned view reads and writes the memory at [address] directly, in
         * the platform's native byte order: it never copies and never frees — the
         * caller keeps owning the memory and must keep it valid for the view's
         * whole lifetime. The intended use is a range a native library lends the
         * caller for a bounded time, such as a GPU-mapped buffer between map and
         * unmap.
         *
         * @param address the native address of the borrowed memory (must not be null when [size] is non-zero)
         * @param size the size of the borrowed range in bytes
         * @return an ArrayBuffer view over the borrowed native memory
         * @throws IllegalStateException when a non-zero size is paired with a null address
         */
        fun wrap(address: Long, size: ULong): ArrayBuffer = BorrowedNativeArrayBuffer(address, size)

        /**
         * Creates an ArrayBuffer from a ByteArray.
         * @param array the byte array to convert
         * @return an ArrayBuffer containing the data from the byte array
         */
        actual fun of(array: ByteArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size)
            buffer.put(array)
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from a ShortArray.
         * @param array the short array to convert
         * @return an ArrayBuffer containing the data from the short array
         */
        actual fun of(array: ShortArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size * Short.SIZE_BYTES)
            buffer.asShortBuffer().put(array)
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from an IntArray.
         * @param array the int array to convert
         * @return an ArrayBuffer containing the data from the int array
         */
        actual fun of(array: IntArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size * Int.SIZE_BYTES)
            buffer.asIntBuffer().put(array)
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from a FloatArray.
         * @param array the float array to convert
         * @return an ArrayBuffer containing the data from the float array
         */
        actual fun of(array: FloatArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size * Float.SIZE_BYTES)
            buffer.asFloatBuffer().put(array)
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from a DoubleArray.
         * @param array the double array to convert
         * @return an ArrayBuffer containing the data from the double array
         */
        actual fun of(array: DoubleArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size * Double.SIZE_BYTES)
            buffer.asDoubleBuffer().put(array)
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from a UByteArray.
         * @param array the unsigned byte array to convert
         * @return an ArrayBuffer containing the data from the unsigned byte array
         */
        actual fun of(array: UByteArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size)
            buffer.put(array.asByteArray())
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from a UShortArray.
         * @param array the unsigned short array to convert
         * @return an ArrayBuffer containing the data from the unsigned short array
         */
        actual fun of(array: UShortArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size * Short.SIZE_BYTES)
            buffer.asShortBuffer().put(array.asShortArray())
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

        /**
         * Creates an ArrayBuffer from a UIntArray.
         * @param array the unsigned int array to convert
         * @return an ArrayBuffer containing the data from the unsigned int array
         */
        actual fun of(array: UIntArray): ArrayBuffer {
            val buffer = ByteBuffer.allocateDirect(array.size * Int.SIZE_BYTES)
            buffer.asIntBuffer().put(array.asIntArray())
            buffer.rewind()
            return AndroidArrayBuffer(buffer)
        }

    }
}

