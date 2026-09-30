package org.graphiks.webgpu

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ArrayBufferChecksTest : FreeSpec({

    "range accepts the exact end and rejects unsigned overflow" {
        checkBufferRange(16uL, 12uL, 4uL)
        checkBufferRange(16uL, 16uL, 0uL)
        shouldThrow<IndexOutOfBoundsException> { checkBufferRange(16uL, 13uL, 4uL) }
        shouldThrow<IndexOutOfBoundsException> { checkBufferRange(16uL, ULong.MAX_VALUE, 2uL) }
        shouldThrow<IndexOutOfBoundsException> { checkBufferRange(16uL, 17uL, 0uL) }
    }

    "multiplication is checked before evaluation" {
        checkedByteCount(536870911uL, 4, Int.MAX_VALUE.toULong()) shouldBe 2147483644uL
        shouldThrow<IllegalArgumentException> {
            checkedByteCount(536870912uL, 4, Int.MAX_VALUE.toULong())
        }
        shouldThrow<IllegalArgumentException> { checkedByteCount(ULong.MAX_VALUE, 8, ULong.MAX_VALUE) }
        checkedByteCount(0uL, 8, 0uL) shouldBe 0uL
    }

    "checkedByteCount allows the exact maximum" {
        checkedByteCount(Int.MAX_VALUE.toULong(), 1, Int.MAX_VALUE.toULong()) shouldBe Int.MAX_VALUE.toULong()
        shouldThrow<IllegalArgumentException> {
            checkedByteCount(Int.MAX_VALUE.toULong() + 1uL, 1, Int.MAX_VALUE.toULong())
        }
    }

    "narrowing and array conversion reject unrepresentable values" {
        checkedIntSize(Int.MAX_VALUE.toULong()) shouldBe Int.MAX_VALUE
        shouldThrow<IllegalArgumentException> { checkedIntSize(4_294_967_296uL) }
        checkedLongSize(Long.MAX_VALUE.toULong()) shouldBe Long.MAX_VALUE
        // A JVM segment may exceed Int.MAX_VALUE; only the Long boundary is checked there.
        checkedLongSize(4_294_967_296uL) shouldBe 4_294_967_296L
        shouldThrow<IllegalArgumentException> { checkedLongSize(Long.MAX_VALUE.toULong() + 1uL) }
        shouldThrow<IllegalArgumentException> { checkedLongSize(ULong.MAX_VALUE) }
        checkedArrayLength(0uL, 4) shouldBe 0
        shouldThrow<IllegalArgumentException> { checkedArrayLength(3uL, 4) }
        shouldThrow<IllegalArgumentException> { checkedArrayLength(8_589_934_592uL, 4) }
    }

    "element width must be positive" {
        shouldThrow<IllegalArgumentException> { checkedByteCount(1uL, 0, 100uL) }
        shouldThrow<IllegalArgumentException> { checkedByteCount(1uL, -1, 100uL) }
        shouldThrow<IllegalArgumentException> { checkedArrayLength(4uL, 0) }
        shouldThrow<IllegalArgumentException> { checkedArrayLength(4uL, -4) }
        shouldThrow<IllegalArgumentException> { checkBufferAlignment(0uL, 0) }
    }

    "alignment accepts multiples and rejects the rest" {
        checkBufferAlignment(0uL, 1)
        checkBufferAlignment(1uL, 1)
        checkBufferAlignment(8uL, 8)
        shouldThrow<IllegalArgumentException> { checkBufferAlignment(1uL, 2) }
        shouldThrow<IllegalArgumentException> { checkBufferAlignment(2uL, 4) }
        shouldThrow<IllegalArgumentException> { checkBufferAlignment(4uL, 8) }
        shouldThrow<IllegalArgumentException> { checkBufferAlignment(ULong.MAX_VALUE, 2) }
    }
})
