@file:OptIn(ExperimentalForeignApi::class, ExperimentalUnsignedTypes::class)

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.graphiks.webgpu.ArrayBuffer
import kotlinx.cinterop.*

/**
 * Tests for ArrayBuffer.wrap() method which is native-specific.
 * This method allows wrapping an existing native pointer without copying data.
 */
class OpaquePointerArrayBufferTest : FreeSpec({

    "ArrayBuffer.wrap() - create buffer from existing memory pointer" {
        // Given - allocate native memory and fill it with data
        val size = 5
        val ptr = nativeHeap.allocArray<ByteVar>(size)
        val sourceArray = byteArrayOf(10, 20, 30, 40, 50)
        for (i in 0 until size) {
            ptr[i] = sourceArray[i]
        }

        // When - create a buffer using of() that wraps the memory
        val wrappedBuffer = ArrayBuffer.wrap(ptr.reinterpret(), size.toULong())

        // Then - the wrapped buffer should have the same data
        wrappedBuffer.toByteArray() shouldBe sourceArray

        // Cleanup
        nativeHeap.free(ptr)
    }

    "ArrayBuffer.wrap() - modifications in wrapped buffer are visible" {
        // Given - allocate native memory
        val size = 5
        val ptr = nativeHeap.allocArray<ByteVar>(size)
        val sourceArray = byteArrayOf(1, 2, 3, 4, 5)
        for (i in 0 until size) {
            ptr[i] = sourceArray[i]
        }

        // When - create a wrapped buffer and modify it
        val wrappedBuffer = ArrayBuffer.wrap(ptr.reinterpret(), size.toULong())
        wrappedBuffer.setByte(0u, 99)
        wrappedBuffer.setByte(2u, 88)

        // Then - modifications should be visible in the wrapped buffer
        wrappedBuffer.getByte(0u) shouldBe 99
        wrappedBuffer.getByte(2u) shouldBe 88

        // Cleanup
        nativeHeap.free(ptr)
    }

    "ArrayBuffer.wrap() - wrapping IntArray memory" {
        // Given - allocate int array memory
        val size = 4
        val ptr = nativeHeap.allocArray<IntVar>(size)
        val sourceInts = intArrayOf(100, 200, 300, 400)
        for (i in 0 until size) {
            ptr[i] = sourceInts[i]
        }

        // When - wrap it using of()
        val wrappedBuffer = ArrayBuffer.wrap(
            ptr.reinterpret(),
            (size * Int.SIZE_BYTES).toULong()
        )

        // Then - should be able to read the same int values
        wrappedBuffer.toIntArray() shouldBe sourceInts

        // Cleanup
        nativeHeap.free(ptr)
    }

    "ArrayBuffer.wrap() - wrapping FloatArray memory" {
        // Given - allocate float array memory
        val size = 4
        val ptr = nativeHeap.allocArray<FloatVar>(size)
        val sourceFloats = floatArrayOf(1.5f, 2.5f, 3.5f, 4.5f)
        for (i in 0 until size) {
            ptr[i] = sourceFloats[i]
        }

        // When - wrap it using of()
        val wrappedBuffer = ArrayBuffer.wrap(
            ptr.reinterpret(),
            (size * Float.SIZE_BYTES).toULong()
        )

        // Then - should preserve float values
        wrappedBuffer.toFloatArray() shouldBe sourceFloats

        // Cleanup
        nativeHeap.free(ptr)
    }

    "ArrayBuffer.wrap() - wrapping DoubleArray memory" {
        // Given - allocate double array memory
        val size = 4
        val ptr = nativeHeap.allocArray<DoubleVar>(size)
        val sourceDoubles = doubleArrayOf(1.5, 2.5, 3.5, 4.5)
        for (i in 0 until size) {
            ptr[i] = sourceDoubles[i]
        }

        // When - wrap it using of()
        val wrappedBuffer = ArrayBuffer.wrap(
            ptr.reinterpret(),
            (size * Double.SIZE_BYTES).toULong()
        )

        // Then - should preserve double values
        wrappedBuffer.toDoubleArray() shouldBe sourceDoubles

        // Cleanup
        nativeHeap.free(ptr)
    }

    "ArrayBuffer.wrap() - partial buffer wrapping" {
        // Given - allocate 10 bytes
        val size = 10
        val ptr = nativeHeap.allocArray<ByteVar>(size)
        for (i in 0 until size) {
            ptr[i] = (i + 1).toByte()
        }

        // When - wrap only the first 5 bytes
        val partialBuffer = ArrayBuffer.wrap(ptr.reinterpret(), 5u)

        // Then - should only contain the first 5 bytes
        partialBuffer.size shouldBe 5u
        partialBuffer.toByteArray() shouldBe byteArrayOf(1, 2, 3, 4, 5)

        // Cleanup
        nativeHeap.free(ptr)
    }

    "ArrayBuffer.wrap() - read and write all data types" {
        // Given - allocate native memory large enough for all types
        val bufferSize = 64
        val ptr = nativeHeap.allocArray<ByteVar>(bufferSize)

        // When - wrap it and write various data types
        val wrappedBuffer = ArrayBuffer.wrap(ptr.reinterpret(), bufferSize.toULong())

        wrappedBuffer.setByte(0u, 42)
        wrappedBuffer.setShort(4u, 1000)
        wrappedBuffer.setInt(8u, 100000)
        wrappedBuffer.setFloat(12u, 3.14f)
        wrappedBuffer.setDouble(16u, 2.71828)
        wrappedBuffer.setUByte(24u, 255u)
        wrappedBuffer.setUShort(28u, 65535u)
        wrappedBuffer.setUInt(32u, 4294967295u)

        // Then - should be able to read all values back
        wrappedBuffer.getByte(0u) shouldBe 42
        wrappedBuffer.getShort(4u) shouldBe 1000
        wrappedBuffer.getInt(8u) shouldBe 100000
        wrappedBuffer.getFloat(12u) shouldBe 3.14f
        wrappedBuffer.getDouble(16u) shouldBe 2.71828
        wrappedBuffer.getUByte(24u) shouldBe 255u
        wrappedBuffer.getUShort(28u) shouldBe 65535u
        wrappedBuffer.getUInt(32u) shouldBe 4294967295u

        // Cleanup
        nativeHeap.free(ptr)
    }

    "ArrayBuffer.allocate() - create zero-initialized buffer" {
        // When - allocate a new buffer
        val buffer = ArrayBuffer.allocate(10u)

        // Then - buffer should have correct size and be zero-initialized
        buffer.size shouldBe 10u
        buffer.toByteArray() shouldBe ByteArray(10) { 0 }
    }

    "ArrayBuffer.allocate() - write and read values" {
        // Given - allocate a new buffer
        val buffer = ArrayBuffer.allocate(32u)

        // When - write various values
        buffer.setByte(0u, 42)
        buffer.setShort(4u, 1000)
        buffer.setInt(8u, 100000)
        buffer.setFloat(12u, 3.14f)
        buffer.setDouble(16u, 2.71828)
        buffer.setUByte(24u, 255u)
        buffer.setUShort(26u, 65535u)
        buffer.setUInt(28u, 4294967295u)

        // Then - should be able to read all values back
        buffer.getByte(0u) shouldBe 42
        buffer.getShort(4u) shouldBe 1000
        buffer.getInt(8u) shouldBe 100000
        buffer.getFloat(12u) shouldBe 3.14f
        buffer.getDouble(16u) shouldBe 2.71828
        buffer.getUByte(24u) shouldBe 255u
        buffer.getUShort(26u) shouldBe 65535u
        buffer.getUInt(28u) shouldBe 4294967295u
    }

    "ArrayBuffer.allocate() - convert to typed arrays" {
        // Given - allocate a buffer and fill it with data
        val buffer = ArrayBuffer.allocate(16u)
        buffer.setInt(0u, 100)
        buffer.setInt(4u, 200)
        buffer.setInt(8u, 300)
        buffer.setInt(12u, 400)

        // When - convert to int array
        val intArray = buffer.toIntArray()

        // Then - should contain the correct values
        intArray shouldBe intArrayOf(100, 200, 300, 400)
    }

    "ArrayBuffer.allocate() - memory is automatically managed" {
        // When - allocate multiple buffers (they should be automatically freed by GC)
        repeat(100) {
            val buffer = ArrayBuffer.allocate(1024u)
            buffer.setByte(0u, it.toByte())
            buffer.getByte(0u) shouldBe it.toByte()
        }
        // Then - no memory leaks should occur (this is verified by not crashing)
    }
})
