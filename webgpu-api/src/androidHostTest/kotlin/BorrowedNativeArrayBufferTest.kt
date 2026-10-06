@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("DEPRECATION_ERROR", "DEPRECATION") // sun.misc.Unsafe is deprecated in recent JDKs

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.graphiks.webgpu.ArrayBuffer
import sun.misc.Unsafe

class BorrowedNativeArrayBufferTest : FreeSpec({

    fun unsafe(): Unsafe {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(null) as Unsafe
    }

    "wrap exposes the borrowed size" {
        val unsafe = unsafe()
        val address = unsafe.allocateMemory(16)
        try {
            val buffer = ArrayBuffer.wrap(address, 16uL)
            buffer.size shouldBe 16uL
        } finally {
            unsafe.freeMemory(address)
        }
    }

    "the view reads and writes the borrowed memory in place" {
        val unsafe = unsafe()
        val address = unsafe.allocateMemory(8)
        try {
            unsafe.putInt(address, 0x0A0B0C0D)
            val buffer = ArrayBuffer.wrap(address, 8uL)

            // Reads see the memory a native writer laid out.
            buffer.getInt(0uL) shouldBe 0x0A0B0C0D

            // Writes reach the borrowed memory itself, not a copy.
            buffer.setUInt(4uL, 0xDEADBEEFu)
            unsafe.getInt(address + 4) shouldBe 0xDEADBEEF.toInt()
        } finally {
            unsafe.freeMemory(address)
        }
    }

    "typed accessors round-trip in native order" {
        val unsafe = unsafe()
        val address = unsafe.allocateMemory(24)
        try {
            val buffer = ArrayBuffer.wrap(address, 24uL)

            buffer.setShort(0uL, 0x0102)
            buffer.setInt(2uL, 0x0A0B0C0D)
            buffer.setFloat(8uL, 3.5f)
            buffer.setDouble(16uL, 1.0e300)

            buffer.getShort(0uL) shouldBe 0x0102
            buffer.getInt(2uL) shouldBe 0x0A0B0C0D
            buffer.getFloat(8uL) shouldBe 3.5f
            buffer.getDouble(16uL) shouldBe 1.0e300

            // The raw bytes match the platform layout, not a fixed endianness.
            unsafe.getByte(address) shouldBe 0x02
            unsafe.getByte(address + 1) shouldBe 0x01
        } finally {
            unsafe.freeMemory(address)
        }
    }

    "bulk operations reach the borrowed memory" {
        val unsafe = unsafe()
        val address = unsafe.allocateMemory(16)
        try {
            val buffer = ArrayBuffer.wrap(address, 16uL)

            buffer.setBytes(4uL, byteArrayOf(5, 7, 11, 13))
            buffer.toByteArray()[4] shouldBe 5
            buffer.toByteArray()[7] shouldBe 13
            unsafe.getInt(address + 4) shouldBe 0x0D0B0705

            buffer.setUInts(0uL, uintArrayOf(1u, 2u))
            buffer.toUIntArray().take(2) shouldBe listOf(1u, 2u)
            unsafe.getInt(address) shouldBe 1
        } finally {
            unsafe.freeMemory(address)
        }
    }

    "a zero-size view is empty and accepts a null address" {
        val buffer = ArrayBuffer.wrap(0L, 0uL)
        buffer.size shouldBe 0uL
        buffer.toByteArray() shouldBe byteArrayOf()
    }

    "out-of-range access throws instead of reaching outside the range" {
        val unsafe = unsafe()
        val address = unsafe.allocateMemory(4)
        try {
            val buffer = ArrayBuffer.wrap(address, 4uL)

            shouldThrow<IndexOutOfBoundsException> { buffer.getByte(4uL) }
            shouldThrow<IndexOutOfBoundsException> { buffer.getInt(1uL) }
            shouldThrow<IndexOutOfBoundsException> { buffer.setByte(ULong.MAX_VALUE - 1uL, 1) }
            shouldThrow<IndexOutOfBoundsException> { buffer.setBytes(2uL, byteArrayOf(1, 2, 3)) }
        } finally {
            unsafe.freeMemory(address)
        }
    }

    "a null address with a non-zero size is refused" {
        shouldThrow<IllegalStateException> { ArrayBuffer.wrap(0L, 4uL) }
    }
})
