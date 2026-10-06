@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ArrayBufferBulkBoundsTest : FreeSpec({

    "bulk writes validate the entire range before writing" {
        val buffer = ArrayBuffer.of(ByteArray(16) { 0x33.toByte() })
        val before = buffer.toByteArray().toList()

        shouldThrow<IndexOutOfBoundsException> { buffer.setInts(12uL, intArrayOf(1, 2)) }
        buffer.toByteArray().toList() shouldBe before

        buffer.setInts(8uL, intArrayOf(1, 2))
        buffer.getInt(8uL) shouldBe 1
        buffer.getInt(12uL) shouldBe 2
    }

    "a misaligned non-empty bulk write is rejected without mutation" {
        val buffer = ArrayBuffer.of(ByteArray(16))
        val before = buffer.toByteArray().toList()
        shouldThrow<IllegalArgumentException> { buffer.setInts(1uL, intArrayOf(1)) }
        buffer.toByteArray().toList() shouldBe before
        shouldThrow<IllegalArgumentException> { buffer.setShorts(1uL, shortArrayOf(1)) }
        shouldThrow<IllegalArgumentException> { buffer.setDoubles(4uL, doubleArrayOf(1.0)) }
    }

    "empty operations do not dereference memory" {
        val empty = ArrayBuffer.allocate(0uL)
        empty.setBytes(0uL, byteArrayOf())
        empty.setInts(0uL, intArrayOf())
        empty.setShorts(0uL, shortArrayOf())
        empty.setFloats(0uL, floatArrayOf())
        empty.setDoubles(0uL, doubleArrayOf())
        empty.toByteArray().size shouldBe 0
        empty.toShortArray().size shouldBe 0
        empty.toIntArray().size shouldBe 0
        empty.toFloatArray().size shouldBe 0
        empty.toDoubleArray().size shouldBe 0
        shouldThrow<IndexOutOfBoundsException> { empty.setBytes(1uL, byteArrayOf()) }
        // A zero-length operation exactly at the end of a non-empty buffer is valid.
        ArrayBuffer.allocate(3uL).setInts(3uL, intArrayOf())
    }

    "array conversions reject non-divisible sizes on every target" {
        shouldThrow<IllegalArgumentException> { ArrayBuffer.allocate(1uL).toShortArray() }
        shouldThrow<IllegalArgumentException> { ArrayBuffer.allocate(3uL).toIntArray() }
        shouldThrow<IllegalArgumentException> { ArrayBuffer.allocate(3uL).toFloatArray() }
        shouldThrow<IllegalArgumentException> { ArrayBuffer.allocate(3uL).toUIntArray() }
        shouldThrow<IllegalArgumentException> { ArrayBuffer.allocate(3uL).toUShortArray() }
        shouldThrow<IllegalArgumentException> { ArrayBuffer.allocate(4uL).toDoubleArray() }
    }

    "every family round-trips a bulk copy" {
        val bytes = ArrayBuffer.of(ByteArray(4))
        bytes.setBytes(0uL, byteArrayOf(1, 2, 3))
        bytes.getByte(2uL) shouldBe 3

        val shorts = ArrayBuffer.of(ByteArray(8))
        shorts.setShorts(0uL, shortArrayOf(1, 2, 3, 4))
        shorts.getShort(6uL) shouldBe 4

        val ints = ArrayBuffer.of(ByteArray(16))
        ints.setInts(0uL, intArrayOf(1, 2, 3))
        ints.getInt(0uL) shouldBe 1
        ints.getInt(4uL) shouldBe 2
        ints.getInt(8uL) shouldBe 3

        val floats = ArrayBuffer.of(ByteArray(8))
        floats.setFloats(0uL, floatArrayOf(1f, 2f))
        floats.getFloat(4uL) shouldBe 2f

        val doubles = ArrayBuffer.of(ByteArray(16))
        doubles.setDoubles(0uL, doubleArrayOf(1.0, 2.0))
        doubles.getDouble(8uL) shouldBe 2.0

        val ubytes = ArrayBuffer.of(ByteArray(4))
        ubytes.setUBytes(0uL, ubyteArrayOf(1u, 2u))
        ubytes.getUByte(1uL) shouldBe 2u

        val ushorts = ArrayBuffer.of(ByteArray(4))
        ushorts.setUShorts(0uL, ushortArrayOf(1u, 2u))
        ushorts.getUShort(2uL) shouldBe 2u

        val uints = ArrayBuffer.of(ByteArray(8))
        uints.setUInts(0uL, uintArrayOf(1u, 2u))
        uints.getUInt(0uL) shouldBe 1u
        uints.getUInt(4uL) shouldBe 2u
    }
})
