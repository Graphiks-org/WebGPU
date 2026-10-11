@file:OptIn(ExperimentalUnsignedTypes::class)

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.graphiks.webgpu.ArrayBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AndroidArrayBufferByteOrderTest : FreeSpec({
    "typed factories expose native-order bytes, not merely matching CPU roundtrips" {
        ArrayBuffer.of(shortArrayOf(0x1234)).toByteArray().toList() shouldBe listOf<Byte>(0x34, 0x12)
        ArrayBuffer.of(intArrayOf(0x12345678)).toByteArray().toList() shouldBe listOf<Byte>(0x78, 0x56, 0x34, 0x12)
        ArrayBuffer.of(floatArrayOf(1f)).toByteArray().toList() shouldBe listOf<Byte>(0, 0, -128, 63)
        ArrayBuffer.of(doubleArrayOf(1.0)).toByteArray().toList() shouldBe listOf<Byte>(0, 0, 0, 0, 0, 0, -16, 63)
        ArrayBuffer.of(ushortArrayOf(0x1234u)).toByteArray().toList() shouldBe listOf<Byte>(0x34, 0x12)
        ArrayBuffer.of(uintArrayOf(0x12345678u)).toByteArray().toList() shouldBe listOf<Byte>(0x78, 0x56, 0x34, 0x12)
    }
    "bulk and scalar accesses share native order, including duplicate typed views" {
        val data = ArrayBuffer.allocate(32uL)
        data.setFloats(0uL, floatArrayOf(1f))
        data.getFloat(0uL) shouldBe 1f
        data.toFloatArray()[0] shouldBe 1f
        data.setInt(4uL, 0x12345678)
        data.toIntArray()[1] shouldBe 0x12345678
        data.setShorts(8uL, shortArrayOf(0x1234))
        data.getShort(8uL) shouldBe 0x1234
        data.toShortArray()[4] shouldBe 0x1234
        data.setDoubles(16uL, doubleArrayOf(1.0))
        data.getDouble(16uL) shouldBe 1.0
        data.toDoubleArray()[2] shouldBe 1.0
        data.toByteArray().take(4) shouldBe listOf<Byte>(0, 0, -128, 63)
        data.setUInts(24uL, uintArrayOf(0x12345678u))
        data.toUIntArray()[6] shouldBe 0x12345678u
        data.setUShorts(28uL, ushortArrayOf(0x1234u))
        data.toUShortArray()[14] shouldBe 0x1234u.toUShort()
    }
    "wrapping a default-order direct carrier interprets existing native bytes without rewriting them" {
        val raw = ByteBuffer.allocateDirect(4).order(ByteOrder.BIG_ENDIAN)
        raw.put(byteArrayOf(0, 0, -128, 63)).rewind()
        val data = ArrayBuffer.wrap(raw)
        data.getFloat(0uL) shouldBe 1f
        data.toFloatArray()[0] shouldBe 1f
        data.toByteArray().toList() shouldBe listOf<Byte>(0, 0, -128, 63)
    }
    "byte factories and byte writes preserve opaque payloads unchanged" {
        val bytes = byteArrayOf(63, -128, 0, 0)
        ArrayBuffer.of(bytes).toByteArray().toList() shouldBe bytes.toList()
        ArrayBuffer.of(bytes.asUByteArray()).toByteArray().toList() shouldBe bytes.toList()
    }
})
