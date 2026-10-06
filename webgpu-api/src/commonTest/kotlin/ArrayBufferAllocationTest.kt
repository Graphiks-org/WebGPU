@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ArrayBufferAllocationTest : FreeSpec({

    "an unrepresentable allocation size is rejected before any allocation" {
        shouldThrow<IllegalArgumentException> { ArrayBuffer.allocate(ULong.MAX_VALUE) }
    }

    "zero-sized allocation works on every target" {
        ArrayBuffer.allocate(0uL).size shouldBe 0u
    }

    "empty arrays convert to zero-sized buffers" {
        ArrayBuffer.of(ByteArray(0)).size shouldBe 0u
        ArrayBuffer.of(ShortArray(0)).size shouldBe 0u
        ArrayBuffer.of(IntArray(0)).size shouldBe 0u
        ArrayBuffer.of(FloatArray(0)).size shouldBe 0u
        ArrayBuffer.of(DoubleArray(0)).size shouldBe 0u
        ArrayBuffer.of(UByteArray(0)).size shouldBe 0u
        ArrayBuffer.of(UShortArray(0)).size shouldBe 0u
        ArrayBuffer.of(UIntArray(0)).size shouldBe 0u
    }

    "non-empty arrays keep their byte size" {
        ArrayBuffer.of(ByteArray(5)).size shouldBe 5u
        ArrayBuffer.of(IntArray(3) { it }).size shouldBe 12u
        ArrayBuffer.of(FloatArray(2)).size shouldBe 8u
        ArrayBuffer.of(DoubleArray(2)).size shouldBe 16u
    }
})
