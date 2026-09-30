package org.graphiks.webgpu.benchmarks

import org.graphiks.webgpu.ArrayBuffer
import java.nio.ByteBuffer

@Volatile
private var benchmarkSink: Int = 0

internal actual class BenchmarkMemory actual constructor(bytes: Int) {
    private val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(bytes)

    actual val buffer: ArrayBuffer = ArrayBuffer.wrap(byteBuffer)

    actual fun setIntReference(offset: Int, value: Int) {
        byteBuffer.putInt(offset, value)
    }

    actual fun getIntReference(offset: Int): Int = byteBuffer.getInt(offset)

    actual fun setFloatReference(offset: Int, value: Float) {
        byteBuffer.putFloat(offset, value)
    }

    actual fun setBytesReference(offset: Int, array: ByteArray) {
        val duplicate = byteBuffer.duplicate()
        duplicate.position(offset)
        duplicate.put(array)
    }

    actual fun setFloatsReference(offset: Int, array: FloatArray) {
        val duplicate = byteBuffer.duplicate()
        duplicate.position(offset)
        duplicate.asFloatBuffer().put(array)
    }

    actual fun consume(seed: Int): Int {
        val observed = byteBuffer.getInt(0) xor seed
        benchmarkSink = observed
        return observed
    }

    actual fun close() = Unit
}
