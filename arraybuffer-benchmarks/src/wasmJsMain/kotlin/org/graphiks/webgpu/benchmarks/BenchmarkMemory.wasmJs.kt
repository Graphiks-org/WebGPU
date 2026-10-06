@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.benchmarks

import js.numbers.JsNumbers.toJsByte
import js.numbers.JsNumbers.toJsFloat
import js.numbers.JsNumbers.toJsInt
import js.typedarrays.Float32Array
import js.typedarrays.Int32Array
import js.typedarrays.Int8Array
import org.graphiks.webgpu.ArrayBuffer

internal actual class BenchmarkMemory actual constructor(bytes: Int) {
    private val jsBuffer = js.buffer.ArrayBuffer(bytes)

    actual val buffer: ArrayBuffer = ArrayBuffer.wrap(jsBuffer)

    actual fun setIntReference(offset: Int, value: Int) {
        Int32Array<js.buffer.ArrayBuffer>(jsBuffer, offset, 1)[0] = value.toJsInt()
    }

    actual fun getIntReference(offset: Int): Int =
        Int32Array<js.buffer.ArrayBuffer>(jsBuffer, offset, 1)[0].toInt()

    actual fun setFloatReference(offset: Int, value: Float) {
        Float32Array<js.buffer.ArrayBuffer>(jsBuffer, offset, 1)[0] = value.toJsFloat()
    }

    actual fun setBytesReference(offset: Int, array: ByteArray) {
        val view = Int8Array<js.buffer.ArrayBuffer>(jsBuffer, offset, array.size)
        array.forEachIndexed { index, value -> view[index] = value.toJsByte() }
    }

    actual fun setFloatsReference(offset: Int, array: FloatArray) {
        val view = Float32Array<js.buffer.ArrayBuffer>(jsBuffer, offset, array.size)
        array.forEachIndexed { index, value -> view[index] = value.toJsFloat() }
    }

    actual fun consume(seed: Int): Int {
        val observed = Int32Array<js.buffer.ArrayBuffer>(jsBuffer, 0, 1)[0].toInt() xor seed
        publishSink(observed)
        return observed
    }

    actual fun close() = Unit
}

private fun publishSink(value: Int) {
    js("globalThis.graphiksArrayBufferSink = value")
}
