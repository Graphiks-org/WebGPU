package org.graphiks.webgpu.benchmarks

import org.graphiks.webgpu.JvmArrayBuffer
import java.lang.foreign.ValueLayout

internal actual fun fillRgbaUnchecked(
    memory: BenchmarkMemory,
    base: Int,
    width: Int,
    height: Int,
    rowStride: Int,
    seed: Int,
) {
    val segment = (memory.buffer as JvmArrayBuffer).buffer
    var index = base
    for (y in 0 until height) {
        for (x in 0 until width) {
            segment.set(ValueLayout.JAVA_BYTE, index.toLong(), ((x + seed) and 255).toByte())
            segment.set(ValueLayout.JAVA_BYTE, (index + 1).toLong(), ((y + seed) and 255).toByte())
            segment.set(ValueLayout.JAVA_BYTE, (index + 2).toLong(), ((x xor y) and 255).toByte())
            segment.set(ValueLayout.JAVA_BYTE, (index + 3).toLong(), 0xFF.toByte())
            index += 4
        }
        index += rowStride - width * 4
    }
}

internal actual fun fillVerticesUnchecked(memory: BenchmarkMemory, base: Int, count: Int, seed: Int) {
    val segment = (memory.buffer as JvmArrayBuffer).buffer
    for (vertex in 0 until count) {
        for (component in 0 until VERTEX_FLOATS) {
            segment.set(
                ValueLayout.JAVA_FLOAT_UNALIGNED,
                (base + vertex * VERTEX_BYTES + component * Float.SIZE_BYTES).toLong(),
                vertexFloat(vertex, component, seed),
            )
        }
    }
}
