@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.benchmarks

import js.numbers.JsNumbers.toJsByte
import js.numbers.JsNumbers.toJsFloat
import js.typedarrays.Float32Array
import js.typedarrays.Int8Array
import org.graphiks.webgpu.WebArrayBuffer

internal actual fun fillRgbaUnchecked(
    memory: BenchmarkMemory,
    base: Int,
    width: Int,
    height: Int,
    rowStride: Int,
    seed: Int,
) {
    val bytes = Int8Array<js.buffer.ArrayBuffer>((memory.buffer as WebArrayBuffer).buffer)
    var index = base
    for (y in 0 until height) {
        for (x in 0 until width) {
            bytes[index] = ((x + seed) and 255).toByte().toJsByte()
            bytes[index + 1] = ((y + seed) and 255).toByte().toJsByte()
            bytes[index + 2] = ((x xor y) and 255).toByte().toJsByte()
            bytes[index + 3] = 0xFF.toByte().toJsByte()
            index += 4
        }
        index += rowStride - width * 4
    }
}

internal actual fun fillVerticesUnchecked(memory: BenchmarkMemory, base: Int, count: Int, seed: Int) {
    val floats = Float32Array<js.buffer.ArrayBuffer>((memory.buffer as WebArrayBuffer).buffer)
    var index = base / Float.SIZE_BYTES
    for (vertex in 0 until count) {
        for (component in 0 until VERTEX_FLOATS) {
            floats[index + component] = vertexFloat(vertex, component, seed).toJsFloat()
        }
        index += VERTEX_FLOATS
    }
}
