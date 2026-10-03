@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ArrayBufferScalarBoundsTest : FreeSpec({

    "byte access accepts the last byte and rejects one past it" {
        val buffer = ArrayBuffer.of(ByteArray(16))
        buffer.setByte(15uL, 1)
        buffer.getByte(15uL) shouldBe 1
        shouldThrow<IndexOutOfBoundsException> { buffer.getByte(16uL) }
        shouldThrow<IndexOutOfBoundsException> { buffer.setByte(16uL, 1) }
        shouldThrow<IndexOutOfBoundsException> { buffer.getByte(ULong.MAX_VALUE) }
        shouldThrow<IndexOutOfBoundsException> { buffer.getByte(4_294_967_296uL) }
    }

    "short access checks range then alignment" {
        val buffer = ArrayBuffer.of(ByteArray(16))
        buffer.setShort(14uL, 7)
        buffer.getShort(14uL) shouldBe 7
        shouldThrow<IndexOutOfBoundsException> { buffer.getShort(15uL) }
        shouldThrow<IndexOutOfBoundsException> { buffer.setShort(16uL, 1) }
        shouldThrow<IllegalArgumentException> { buffer.getShort(1uL) }
        shouldThrow<IllegalArgumentException> { buffer.setShort(1uL, 1) }
    }

    "int access checks range then alignment" {
        val buffer = ArrayBuffer.of(ByteArray(16))
        buffer.setInt(12uL, 7)
        buffer.getInt(12uL) shouldBe 7
        shouldThrow<IndexOutOfBoundsException> { buffer.getInt(13uL) }
        shouldThrow<IndexOutOfBoundsException> { buffer.setInt(16uL, 1) }
        shouldThrow<IndexOutOfBoundsException> { buffer.getInt(4_294_967_296uL) }
        shouldThrow<IllegalArgumentException> { buffer.getInt(1uL) }
        shouldThrow<IllegalArgumentException> { buffer.setInt(1uL, 1) }
    }

    "float access checks range then alignment" {
        val buffer = ArrayBuffer.of(ByteArray(16))
        buffer.setFloat(12uL, 1.5f)
        buffer.getFloat(12uL) shouldBe 1.5f
        shouldThrow<IndexOutOfBoundsException> { buffer.getFloat(13uL) }
        shouldThrow<IllegalArgumentException> { buffer.getFloat(1uL) }
        shouldThrow<IllegalArgumentException> { buffer.setFloat(2uL, 1f) }
    }

    "double access checks range then alignment" {
        val buffer = ArrayBuffer.of(ByteArray(16))
        buffer.setDouble(8uL, 2.5)
        buffer.getDouble(8uL) shouldBe 2.5
        shouldThrow<IndexOutOfBoundsException> { buffer.getDouble(9uL) }
        shouldThrow<IllegalArgumentException> { buffer.getDouble(4uL) }
        shouldThrow<IllegalArgumentException> { buffer.setDouble(4uL, 1.0) }
    }

    "unsigned access mirrors the signed path" {
        val buffer = ArrayBuffer.of(ByteArray(16))
        buffer.setUByte(15uL, 255u)
        buffer.getUByte(15uL) shouldBe 255u
        buffer.setUShort(14uL, 65535u)
        buffer.getUShort(14uL) shouldBe 65535u
        buffer.setUInt(12uL, 4294967295u)
        buffer.getUInt(12uL) shouldBe 4294967295u
        shouldThrow<IndexOutOfBoundsException> { buffer.getUByte(16uL) }
        shouldThrow<IndexOutOfBoundsException> { buffer.getUShort(15uL) }
        shouldThrow<IndexOutOfBoundsException> { buffer.getUInt(13uL) }
        shouldThrow<IllegalArgumentException> { buffer.getUShort(1uL) }
        shouldThrow<IllegalArgumentException> { buffer.getUInt(2uL) }
    }

    "a write rejected for range or alignment keeps every byte unchanged" {
        val buffer = ArrayBuffer.of(ByteArray(16) { 0x5a.toByte() })
        val before = buffer.toByteArray().toList()
        shouldThrow<IndexOutOfBoundsException> { buffer.setInt(4_294_967_296uL, 7) }
        buffer.toByteArray().toList() shouldBe before
        shouldThrow<IndexOutOfBoundsException> { buffer.setInt(13uL, 7) }
        buffer.toByteArray().toList() shouldBe before
        shouldThrow<IllegalArgumentException> { buffer.setInt(1uL, 7) }
        buffer.toByteArray().toList() shouldBe before
    }
})
