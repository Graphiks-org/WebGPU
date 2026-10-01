@file:OptIn(ExperimentalForeignApi::class)

package org.graphiks.webgpu.benchmarks

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import org.graphiks.webgpu.OpaquePointerArrayBuffer

internal actual fun fillRgbaUnchecked(
    memory: BenchmarkMemory,
    base: Int,
    width: Int,
    height: Int,
    rowStride: Int,
    seed: Int,
) {
    val bytes = (memory.buffer as OpaquePointerArrayBuffer).pointer.reinterpret<ByteVar>()
    var index = base
    for (y in 0 until height) {
        for (x in 0 until width) {
            bytes[index] = ((x + seed) and 255).toByte()
            bytes[index + 1] = ((y + seed) and 255).toByte()
            bytes[index + 2] = ((x xor y) and 255).toByte()
            bytes[index + 3] = 0xFF.toByte()
            index += 4
        }
        index += rowStride - width * 4
    }
}

internal actual fun fillVerticesUnchecked(memory: BenchmarkMemory, base: Int, count: Int, seed: Int) {
    val floats = (memory.buffer as OpaquePointerArrayBuffer).pointer.reinterpret<FloatVar>()
    val first = base / Float.SIZE_BYTES
    for (vertex in 0 until count) {
        for (component in 0 until VERTEX_FLOATS) {
            floats[first + vertex * VERTEX_FLOATS + component] = vertexFloat(vertex, component, seed)
        }
    }
}
