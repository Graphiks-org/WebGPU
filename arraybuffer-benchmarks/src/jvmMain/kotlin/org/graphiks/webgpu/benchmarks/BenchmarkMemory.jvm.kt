package org.graphiks.webgpu.benchmarks

import org.graphiks.webgpu.ArrayBuffer
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

@Volatile
private var benchmarkSink: Int = 0

internal actual class BenchmarkMemory actual constructor(bytes: Int) {
    private val arena: Arena = Arena.ofConfined()
    private val segment: MemorySegment = arena.allocate(bytes.toLong())

    actual val buffer: ArrayBuffer = ArrayBuffer.wrap(segment)

    actual fun setIntReference(offset: Int, value: Int) {
        segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset.toLong(), value)
    }

    actual fun getIntReference(offset: Int): Int =
        segment.get(ValueLayout.JAVA_INT_UNALIGNED, offset.toLong())

    actual fun setFloatReference(offset: Int, value: Float) {
        segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong(), value)
    }

    actual fun setBytesReference(offset: Int, array: ByteArray) {
        MemorySegment.copy(array, 0, segment, ValueLayout.JAVA_BYTE, offset.toLong(), array.size)
    }

    actual fun setFloatsReference(offset: Int, array: FloatArray) {
        MemorySegment.copy(array, 0, segment, ValueLayout.JAVA_FLOAT_UNALIGNED, offset.toLong(), array.size)
    }

    actual fun consume(seed: Int): Int {
        val observed = segment.get(ValueLayout.JAVA_INT_UNALIGNED, 0L) xor seed
        benchmarkSink = observed
        return observed
    }

    actual fun close() {
        arena.close()
    }
}
