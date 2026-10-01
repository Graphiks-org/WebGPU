package org.graphiks.webgpu.benchmarks

import org.graphiks.webgpu.AndroidArrayBuffer

internal actual fun fillRgbaUnchecked(
    memory: BenchmarkMemory,
    base: Int,
    width: Int,
    height: Int,
    rowStride: Int,
    seed: Int,
) {
    val buffer = (memory.buffer as AndroidArrayBuffer).buffer
    var index = base
    for (y in 0 until height) {
        for (x in 0 until width) {
            buffer.put(index, ((x + seed) and 255).toByte())
            buffer.put(index + 1, ((y + seed) and 255).toByte())
            buffer.put(index + 2, ((x xor y) and 255).toByte())
            buffer.put(index + 3, 0xFF.toByte())
            index += 4
        }
        index += rowStride - width * 4
    }
}

internal actual fun fillVerticesUnchecked(memory: BenchmarkMemory, base: Int, count: Int, seed: Int) {
    val buffer = (memory.buffer as AndroidArrayBuffer).buffer
    for (vertex in 0 until count) {
        for (component in 0 until VERTEX_FLOATS) {
            buffer.putFloat(
                base + vertex * VERTEX_BYTES + component * Float.SIZE_BYTES,
                vertexFloat(vertex, component, seed),
            )
        }
    }
}
