@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu.benchmarks

import org.graphiks.webgpu.ArrayBuffer
import java.nio.ByteBuffer

/**
 * On-device mirror of the `webgpu-api` ArrayBuffer business suite.
 *
 * The KMP Android library plugin exposes no unit-test task, so `webgpu-api`'s `commonTest` is
 * compiled for the Android target but never executed on it. These cases restate that public
 * contract for the device run against the real direct `ByteBuffer` implementation. The
 * `commonTest` specs (`ArrayBufferAllocationTest`, `ArrayBufferScalarBoundsTest`,
 * `ArrayBufferBulkBoundsTest` and the wrapped-buffer cases) remain the authoritative suite on
 * JVM, JS, Wasm and Native: any case added there must be mirrored here.
 */
internal object ArrayBufferAndroidBusinessCases {

    /** Collects every failure instead of stopping at the first one, for usable device reports. */
    private class Recorder {
        val failures = mutableListOf<String>()

        fun expect(condition: Boolean, description: String) {
            if (!condition) failures += description
        }

        fun expectThrows(type: Class<*>, description: String, block: () -> Unit) {
            try {
                block()
                failures += "$description: expected ${type.simpleName} but nothing was thrown"
            } catch (failure: Throwable) {
                if (!type.isInstance(failure)) {
                    failures += "$description: expected ${type.simpleName} but got $failure"
                }
            }
        }
    }

    fun verifyAll() {
        val recorder = Recorder()
        allocationCases(recorder)
        scalarBoundsCases(recorder)
        rejectedWriteKeepsContent(recorder)
        bulkBoundsCases(recorder)
        emptyAndConversionCases(recorder)
        familyRoundTripCases(recorder)
        wrappedBufferCases(recorder)
        if (recorder.failures.isNotEmpty()) {
            throw AssertionError(
                recorder.failures.joinToString("\n", prefix = "ArrayBuffer business cases failed:\n"),
            )
        }
    }

    private fun allocationCases(r: Recorder) {
        r.expectThrows(IllegalArgumentException::class.java, "allocate(ULong.MAX_VALUE) is rejected") {
            ArrayBuffer.allocate(ULong.MAX_VALUE)
        }
        r.expect(ArrayBuffer.allocate(0uL).size == 0uL, "allocate(0) has size 0")
        r.expect(ArrayBuffer.of(ByteArray(0)).size == 0uL, "of(ByteArray(0)) has size 0")
        r.expect(ArrayBuffer.of(ShortArray(0)).size == 0uL, "of(ShortArray(0)) has size 0")
        r.expect(ArrayBuffer.of(IntArray(0)).size == 0uL, "of(IntArray(0)) has size 0")
        r.expect(ArrayBuffer.of(FloatArray(0)).size == 0uL, "of(FloatArray(0)) has size 0")
        r.expect(ArrayBuffer.of(DoubleArray(0)).size == 0uL, "of(DoubleArray(0)) has size 0")
        r.expect(ArrayBuffer.of(UByteArray(0)).size == 0uL, "of(UByteArray(0)) has size 0")
        r.expect(ArrayBuffer.of(UShortArray(0)).size == 0uL, "of(UShortArray(0)) has size 0")
        r.expect(ArrayBuffer.of(UIntArray(0)).size == 0uL, "of(UIntArray(0)) has size 0")
        r.expect(ArrayBuffer.of(ByteArray(5)).size == 5uL, "of(ByteArray(5)) keeps its size")
        r.expect(ArrayBuffer.of(IntArray(3) { it }).size == 12uL, "of(IntArray(3)) keeps its size")
        r.expect(ArrayBuffer.of(FloatArray(2)).size == 8uL, "of(FloatArray(2)) keeps its size")
        r.expect(ArrayBuffer.of(DoubleArray(2)).size == 16uL, "of(DoubleArray(2)) keeps its size")
    }

    private fun scalarBoundsCases(r: Recorder) {
        val bytes = ArrayBuffer.of(ByteArray(16))
        bytes.setByte(15uL, 1)
        r.expect(bytes.getByte(15uL) == 1.toByte(), "setByte/getByte accept the last byte")
        r.expectThrows(IndexOutOfBoundsException::class.java, "getByte(16) is out of range") { bytes.getByte(16uL) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "setByte(16) is out of range") { bytes.setByte(16uL, 1) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "getByte(ULong.MAX_VALUE) is out of range") { bytes.getByte(ULong.MAX_VALUE) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "getByte(4_294_967_296) is out of range") { bytes.getByte(4_294_967_296uL) }

        val shorts = ArrayBuffer.of(ByteArray(16))
        shorts.setShort(14uL, 7)
        r.expect(shorts.getShort(14uL) == 7.toShort(), "setShort/getShort accept the last aligned offset")
        r.expectThrows(IndexOutOfBoundsException::class.java, "getShort(15) is out of range") { shorts.getShort(15uL) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "setShort(16) is out of range") { shorts.setShort(16uL, 1) }
        r.expectThrows(IllegalArgumentException::class.java, "getShort(1) is misaligned") { shorts.getShort(1uL) }
        r.expectThrows(IllegalArgumentException::class.java, "setShort(1) is misaligned") { shorts.setShort(1uL, 1) }

        val ints = ArrayBuffer.of(ByteArray(16))
        ints.setInt(12uL, 7)
        r.expect(ints.getInt(12uL) == 7, "setInt/getInt accept the last aligned offset")
        r.expectThrows(IndexOutOfBoundsException::class.java, "getInt(13) is out of range") { ints.getInt(13uL) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "setInt(16) is out of range") { ints.setInt(16uL, 1) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "getInt(4_294_967_296) is out of range") { ints.getInt(4_294_967_296uL) }
        r.expectThrows(IllegalArgumentException::class.java, "getInt(1) is misaligned") { ints.getInt(1uL) }
        r.expectThrows(IllegalArgumentException::class.java, "setInt(1) is misaligned") { ints.setInt(1uL, 1) }

        val floats = ArrayBuffer.of(ByteArray(16))
        floats.setFloat(12uL, 1.5f)
        r.expect(floats.getFloat(12uL) == 1.5f, "setFloat/getFloat accept the last aligned offset")
        r.expectThrows(IndexOutOfBoundsException::class.java, "getFloat(13) is out of range") { floats.getFloat(13uL) }
        r.expectThrows(IllegalArgumentException::class.java, "getFloat(1) is misaligned") { floats.getFloat(1uL) }
        r.expectThrows(IllegalArgumentException::class.java, "setFloat(2) is misaligned") { floats.setFloat(2uL, 1f) }

        val doubles = ArrayBuffer.of(ByteArray(16))
        doubles.setDouble(8uL, 2.5)
        r.expect(doubles.getDouble(8uL) == 2.5, "setDouble/getDouble accept the last aligned offset")
        r.expectThrows(IndexOutOfBoundsException::class.java, "getDouble(9) is out of range") { doubles.getDouble(9uL) }
        r.expectThrows(IllegalArgumentException::class.java, "getDouble(4) is misaligned") { doubles.getDouble(4uL) }
        r.expectThrows(IllegalArgumentException::class.java, "setDouble(4) is misaligned") { doubles.setDouble(4uL, 1.0) }

        val unsigned = ArrayBuffer.of(ByteArray(16))
        unsigned.setUByte(15uL, 255u)
        r.expect(unsigned.getUByte(15uL) == 255u.toUByte(), "setUByte/getUByte accept the last byte")
        unsigned.setUShort(14uL, 65535u)
        r.expect(unsigned.getUShort(14uL) == 65535u.toUShort(), "setUShort/getUShort accept the last aligned offset")
        unsigned.setUInt(12uL, 4294967295u)
        r.expect(unsigned.getUInt(12uL) == 4294967295u, "setUInt/getUInt accept the last aligned offset")
        r.expectThrows(IndexOutOfBoundsException::class.java, "getUByte(16) is out of range") { unsigned.getUByte(16uL) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "getUShort(15) is out of range") { unsigned.getUShort(15uL) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "getUInt(13) is out of range") { unsigned.getUInt(13uL) }
        r.expectThrows(IllegalArgumentException::class.java, "getUShort(1) is misaligned") { unsigned.getUShort(1uL) }
        r.expectThrows(IllegalArgumentException::class.java, "getUInt(2) is misaligned") { unsigned.getUInt(2uL) }
    }

    private fun rejectedWriteKeepsContent(r: Recorder) {
        val buffer = ArrayBuffer.of(ByteArray(16) { 0x5a.toByte() })
        val before = buffer.toByteArray().toList()
        r.expectThrows(IndexOutOfBoundsException::class.java, "setInt(4_294_967_296) is out of range") {
            buffer.setInt(4_294_967_296uL, 7)
        }
        r.expect(buffer.toByteArray().toList() == before, "an out-of-range write keeps every byte")
        r.expectThrows(IndexOutOfBoundsException::class.java, "setInt(13) is out of range") { buffer.setInt(13uL, 7) }
        r.expect(buffer.toByteArray().toList() == before, "an out-of-range write keeps every byte")
        r.expectThrows(IllegalArgumentException::class.java, "setInt(1) is misaligned") { buffer.setInt(1uL, 7) }
        r.expect(buffer.toByteArray().toList() == before, "a misaligned write keeps every byte")
    }

    private fun bulkBoundsCases(r: Recorder) {
        val buffer = ArrayBuffer.of(ByteArray(16) { 0x33.toByte() })
        val before = buffer.toByteArray().toList()
        r.expectThrows(IndexOutOfBoundsException::class.java, "setInts(12, [1, 2]) is out of range") {
            buffer.setInts(12uL, intArrayOf(1, 2))
        }
        r.expect(buffer.toByteArray().toList() == before, "a rejected bulk write keeps every byte")
        buffer.setInts(8uL, intArrayOf(1, 2))
        r.expect(buffer.getInt(8uL) == 1, "bulk write stores the first element")
        r.expect(buffer.getInt(12uL) == 2, "bulk write stores the last element")

        r.expectThrows(IllegalArgumentException::class.java, "setInts(1, [1]) is misaligned") { buffer.setInts(1uL, intArrayOf(1)) }
        r.expect(buffer.toByteArray().toList() != before, "the accepted bulk write did change the buffer")
        r.expectThrows(IllegalArgumentException::class.java, "setShorts(1, [1]) is misaligned") { buffer.setShorts(1uL, shortArrayOf(1)) }
        r.expectThrows(IllegalArgumentException::class.java, "setDoubles(4, [1.0]) is misaligned") { buffer.setDoubles(4uL, doubleArrayOf(1.0)) }
    }

    private fun emptyAndConversionCases(r: Recorder) {
        val empty = ArrayBuffer.allocate(0uL)
        empty.setBytes(0uL, byteArrayOf())
        empty.setInts(0uL, intArrayOf())
        empty.setShorts(0uL, shortArrayOf())
        empty.setFloats(0uL, floatArrayOf())
        empty.setDoubles(0uL, doubleArrayOf())
        r.expect(empty.toByteArray().isEmpty(), "empty buffer converts to an empty ByteArray")
        r.expect(empty.toShortArray().isEmpty(), "empty buffer converts to an empty ShortArray")
        r.expect(empty.toIntArray().isEmpty(), "empty buffer converts to an empty IntArray")
        r.expect(empty.toFloatArray().isEmpty(), "empty buffer converts to an empty FloatArray")
        r.expect(empty.toDoubleArray().isEmpty(), "empty buffer converts to an empty DoubleArray")
        r.expectThrows(IndexOutOfBoundsException::class.java, "setBytes(1, []) on an empty buffer is out of range") {
            empty.setBytes(1uL, byteArrayOf())
        }
        ArrayBuffer.allocate(3uL).setInts(3uL, intArrayOf())

        r.expectThrows(IllegalArgumentException::class.java, "allocate(1).toShortArray() refuses a non-divisible size") { ArrayBuffer.allocate(1uL).toShortArray() }
        r.expectThrows(IllegalArgumentException::class.java, "allocate(3).toIntArray() refuses a non-divisible size") { ArrayBuffer.allocate(3uL).toIntArray() }
        r.expectThrows(IllegalArgumentException::class.java, "allocate(3).toFloatArray() refuses a non-divisible size") { ArrayBuffer.allocate(3uL).toFloatArray() }
        r.expectThrows(IllegalArgumentException::class.java, "allocate(3).toUIntArray() refuses a non-divisible size") { ArrayBuffer.allocate(3uL).toUIntArray() }
        r.expectThrows(IllegalArgumentException::class.java, "allocate(3).toUShortArray() refuses a non-divisible size") { ArrayBuffer.allocate(3uL).toUShortArray() }
        r.expectThrows(IllegalArgumentException::class.java, "allocate(4).toDoubleArray() refuses a non-divisible size") { ArrayBuffer.allocate(4uL).toDoubleArray() }
    }

    private fun familyRoundTripCases(r: Recorder) {
        val bytes = ArrayBuffer.of(ByteArray(4))
        bytes.setBytes(0uL, byteArrayOf(1, 2, 3))
        r.expect(bytes.getByte(2uL) == 3.toByte(), "byte bulk copy round-trips")

        val shorts = ArrayBuffer.of(ByteArray(8))
        shorts.setShorts(0uL, shortArrayOf(1, 2, 3, 4))
        r.expect(shorts.getShort(6uL) == 4.toShort(), "short bulk copy round-trips")

        val ints = ArrayBuffer.of(ByteArray(16))
        ints.setInts(0uL, intArrayOf(1, 2, 3))
        r.expect(ints.getInt(0uL) == 1 && ints.getInt(4uL) == 2 && ints.getInt(8uL) == 3, "int bulk copy round-trips")

        val floats = ArrayBuffer.of(ByteArray(8))
        floats.setFloats(0uL, floatArrayOf(1f, 2f))
        r.expect(floats.getFloat(4uL) == 2f, "float bulk copy round-trips")

        val doubles = ArrayBuffer.of(ByteArray(16))
        doubles.setDoubles(0uL, doubleArrayOf(1.0, 2.0))
        r.expect(doubles.getDouble(8uL) == 2.0, "double bulk copy round-trips")

        val ubytes = ArrayBuffer.of(ByteArray(4))
        ubytes.setUBytes(0uL, ubyteArrayOf(1u, 2u))
        r.expect(ubytes.getUByte(1uL) == 2u.toUByte(), "unsigned byte bulk copy round-trips")

        val ushorts = ArrayBuffer.of(ByteArray(4))
        ushorts.setUShorts(0uL, ushortArrayOf(1u, 2u))
        r.expect(ushorts.getUShort(2uL) == 2u.toUShort(), "unsigned short bulk copy round-trips")

        val uints = ArrayBuffer.of(ByteArray(8))
        uints.setUInts(0uL, uintArrayOf(1u, 2u))
        r.expect(uints.getUInt(0uL) == 1u && uints.getUInt(4uL) == 2u, "unsigned int bulk copy round-trips")
    }

    private fun wrappedBufferCases(r: Recorder) {
        // A 24-byte direct allocation whose central 16 bytes are wrapped: the margins stay outside
        // the declared range, mirroring the JVM and Native wrapped-buffer cases.
        val full = ByteBuffer.allocateDirect(24)
        val positioned = full.duplicate()
        positioned.position(4)
        positioned.limit(20)
        val buffer = ArrayBuffer.wrap(positioned.slice())

        r.expect(buffer.size == 16uL, "the wrapped slice declares 16 bytes")
        buffer.setInt(12uL, 1)
        r.expect(buffer.getInt(12uL) == 1, "a valid access inside the slice works")
        r.expectThrows(IndexOutOfBoundsException::class.java, "setInt(13) past the slice is out of range") { buffer.setInt(13uL, 1) }
        r.expectThrows(IndexOutOfBoundsException::class.java, "getInt(16) past the slice is out of range") { buffer.getInt(16uL) }
        r.expectThrows(IllegalArgumentException::class.java, "getInt(1) is misaligned") { buffer.getInt(1uL) }
        r.expect(full.getInt(0) == 0, "the left margin is untouched")
        r.expect(full.getInt(20) == 0, "the right margin is untouched")
    }
}
