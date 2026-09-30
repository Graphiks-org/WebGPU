package org.graphiks.webgpu

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.free
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner
import platform.posix.memcpy
import platform.posix.memset

/**
 * Refuses a borrowed size this implementation cannot address. It is intentionally explicit: large
 * external pointers would need a different addressing scheme and their own campaign.
 */
private fun checkedNativeSize(sizeInBytes: ULong): ULong {
    checkedIntSize(sizeInBytes)
    return sizeInBytes
}

/** Allocates a zero-initialized owned buffer; a zero-length buffer gets a one-byte sentinel. */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
private fun allocateZeroed(sizeInBytes: ULong): COpaquePointer {
    val bytes = checkedIntSize(sizeInBytes)
    val allocationSize = if (bytes == 0) 1 else bytes
    val pointer = nativeHeap.allocArray<ByteVar>(allocationSize)
    val raw: COpaquePointer = pointer.reinterpret()
    memset(raw, 0, allocationSize.convert())
    return raw
}

/**
 * Represents a native array buffer backed by an opaque C pointer, providing direct access
 * to unmanaged memory for efficient interop with native C libraries.
 *
 * This class wraps a raw C pointer (COpaquePointer) and manages the lifecycle of the allocated
 * memory. It implements the `ArrayBuffer` interface to provide a unified API for buffer operations
 * while allowing direct manipulation of native memory.
 *
 * The buffer automatically manages memory allocation and deallocation using Kotlin/Native's
 * Arena or manual memory management. This is particularly useful when interfacing with WebGPU
 * or other graphics APIs that expect native memory pointers.
 *
 * @param pointer The opaque C pointer to the native memory
 * @param size The size of the buffer in bytes
 * @param ownsMemory Whether this buffer owns the memory and should free it on cleanup
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
class OpaquePointerArrayBuffer private constructor(
    val pointer: COpaquePointer,
    override val size: ULong,
    private val ownsMemory: Boolean = true
) : ArrayBuffer {

    /**
     * Cleans up the native memory when the buffer is no longer needed.
     */
    @Suppress("UNUSED_PARAMETER")
    private val cleaner = if (ownsMemory) {
        createCleaner(pointer.reinterpret<ByteVar>()) { ptr ->
            nativeHeap.free(ptr)
        }
    } else null

    internal constructor(sizeInBytes: ULong) : this(
        pointer = allocateZeroed(sizeInBytes),
        size = sizeInBytes,
        ownsMemory = true
    )

    /**
     * Creates a new buffer from an existing pointer without taking ownership.
     * @param pointer The opaque C pointer
     * @param sizeInBytes The size of the buffer in bytes
     */
    internal constructor(pointer: COpaquePointer, sizeInBytes: ULong) : this(
        pointer = pointer,
        size = checkedNativeSize(sizeInBytes),
        ownsMemory = false
    )

    private val bytePtr: CPointer<ByteVar>
        get() = pointer.reinterpret()

    // Read methods - convert entire buffer to typed arrays

    @OptIn(UnsafeNumber::class)
    override fun toByteArray(): ByteArray {
        val array = ByteArray(size.toInt())
        array.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytePtr, size.convert())
        }
        return array
    }

    @OptIn(UnsafeNumber::class)
    override fun toShortArray(): ShortArray {
        require(size % Short.SIZE_BYTES.toUInt() == 0uL) { "Buffer size must be multiple of ${Short.SIZE_BYTES}" }
        val array = ShortArray((size / Short.SIZE_BYTES.toUInt()).toInt())
        array.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytePtr, size.convert())
        }
        return array
    }

    @OptIn(UnsafeNumber::class)
    override fun toIntArray(): IntArray {
        require(size % Int.SIZE_BYTES.toUInt() == 0uL) { "Buffer size must be multiple of ${Int.SIZE_BYTES}" }
        val array = IntArray((size / Int.SIZE_BYTES.toUInt()).toInt())
        array.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytePtr, size.convert())
        }
        return array
    }

    @OptIn(UnsafeNumber::class)
    override fun toFloatArray(): FloatArray {
        require(size % Float.SIZE_BYTES.toUInt() == 0uL) { "Buffer size must be multiple of ${Float.SIZE_BYTES}" }
        val array = FloatArray((size / Float.SIZE_BYTES.toUInt()).toInt())
        array.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytePtr, size.convert())
        }
        return array
    }

    @OptIn(UnsafeNumber::class)
    override fun toDoubleArray(): DoubleArray {
        require(size % Double.SIZE_BYTES.toUInt() == 0uL) { "Buffer size must be multiple of ${Double.SIZE_BYTES}" }
        val array = DoubleArray((size / Double.SIZE_BYTES.toUInt()).toInt())
        array.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytePtr, size.convert())
        }
        return array
    }

    override fun toUByteArray(): UByteArray {
        return toByteArray().asUByteArray()
    }

    override fun toUShortArray(): UShortArray {
        return toShortArray().asUShortArray()
    }

    override fun toUIntArray(): UIntArray {
        return toIntArray().asUIntArray()
    }

    // Indexed read methods

    override fun getByte(offset: ULong): Byte {
        return bytePtr[offset.toInt()]
    }

    override fun getShort(offset: ULong): Short {
        return pointer.reinterpret<ShortVar>()[(offset / Short.SIZE_BYTES.toUInt()).toInt()]
    }

    override fun getInt(offset: ULong): Int {
        return pointer.reinterpret<IntVar>()[(offset / Int.SIZE_BYTES.toUInt()).toInt()]
    }

    override fun getFloat(offset: ULong): Float {
        return pointer.reinterpret<FloatVar>()[(offset / Float.SIZE_BYTES.toUInt()).toInt()]
    }

    override fun getDouble(offset: ULong): Double {
        return pointer.reinterpret<DoubleVar>()[(offset / Double.SIZE_BYTES.toUInt()).toInt()]
    }

    override fun getUByte(offset: ULong): UByte {
        return getByte(offset).toUByte()
    }

    override fun getUShort(offset: ULong): UShort {
        return getShort(offset).toUShort()
    }

    override fun getUInt(offset: ULong): UInt {
        return getInt(offset).toUInt()
    }

    // Indexed write methods

    override fun setByte(offset: ULong, value: Byte) {
        bytePtr[offset.toInt()] = value
    }

    override fun setShort(offset: ULong, value: Short) {
        pointer.reinterpret<ShortVar>()[(offset / Short.SIZE_BYTES.toUInt()).toInt()] = value
    }

    override fun setInt(offset: ULong, value: Int) {
        pointer.reinterpret<IntVar>()[(offset / Int.SIZE_BYTES.toUInt()).toInt()] = value
    }

    override fun setFloat(offset: ULong, value: Float) {
        pointer.reinterpret<FloatVar>()[(offset / Float.SIZE_BYTES.toUInt()).toInt()] = value
    }

    override fun setDouble(offset: ULong, value: Double) {
        pointer.reinterpret<DoubleVar>()[(offset / Double.SIZE_BYTES.toUInt()).toInt()] = value
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

    @OptIn(UnsafeNumber::class)
    override fun setBytes(offset: ULong, array: ByteArray) {
        array.usePinned { pinned ->
            val destPtr = interpretCPointer<ByteVar>(bytePtr.rawValue + offset.toLong())
            memcpy(destPtr, pinned.addressOf(0), array.size.convert())
        }
    }

    @OptIn(UnsafeNumber::class)
    override fun setShorts(offset: ULong, array: ShortArray) {
        array.usePinned { pinned ->
            val destPtr = interpretCPointer<ByteVar>(bytePtr.rawValue + offset.toLong())
            memcpy(destPtr, pinned.addressOf(0), (array.size * Short.SIZE_BYTES).convert())
        }
    }

    @OptIn(UnsafeNumber::class)
    override fun setInts(offset: ULong, array: IntArray) {
        array.usePinned { pinned ->
            val destPtr = interpretCPointer<ByteVar>(bytePtr.rawValue + offset.toLong())
            memcpy(destPtr, pinned.addressOf(0), (array.size * Int.SIZE_BYTES).convert())
        }
    }

    @OptIn(UnsafeNumber::class)
    override fun setFloats(offset: ULong, array: FloatArray) {
        array.usePinned { pinned ->
            val destPtr = interpretCPointer<ByteVar>(bytePtr.rawValue + offset.toLong())
            memcpy(destPtr, pinned.addressOf(0), (array.size * Float.SIZE_BYTES).convert())
        }
    }

    @OptIn(UnsafeNumber::class)
    override fun setDoubles(offset: ULong, array: DoubleArray) {
        array.usePinned { pinned ->
            val destPtr = interpretCPointer<ByteVar>(bytePtr.rawValue + offset.toLong())
            memcpy(destPtr, pinned.addressOf(0), (array.size * Double.SIZE_BYTES).convert())
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

}