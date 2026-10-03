@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)

package org.graphiks.webgpu.benchmarks

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.free
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import org.graphiks.webgpu.ArrayBuffer
import platform.posix.memcpy

private var benchmarkSink: Int = 0

internal actual class BenchmarkMemory actual constructor(bytes: Int) {
    private val pointer: CPointer<ByteVar> = nativeHeap.allocArray<ByteVar>(bytes)

    actual val buffer: ArrayBuffer = ArrayBuffer.wrap(pointer.reinterpret(), bytes.toULong())

    actual fun setIntReference(offset: Int, value: Int) {
        pointer.reinterpret<IntVar>()[offset / Int.SIZE_BYTES] = value
    }

    actual fun getIntReference(offset: Int): Int =
        pointer.reinterpret<IntVar>()[offset / Int.SIZE_BYTES]

    actual fun setFloatReference(offset: Int, value: Float) {
        pointer.reinterpret<FloatVar>()[offset / Float.SIZE_BYTES] = value
    }

    actual fun setBytesReference(offset: Int, array: ByteArray) {
        array.usePinned { pinned ->
            val destination = interpretCPointer<ByteVar>(pointer.rawValue + offset.toLong())
            memcpy(destination, pinned.addressOf(0), array.size.convert())
        }
    }

    actual fun setFloatsReference(offset: Int, array: FloatArray) {
        array.usePinned { pinned ->
            val destination = interpretCPointer<ByteVar>(pointer.rawValue + offset.toLong())
            memcpy(destination, pinned.addressOf(0), (array.size * Float.SIZE_BYTES).convert())
        }
    }

    actual fun consume(seed: Int): Int {
        val observed = pointer.reinterpret<IntVar>()[0] xor seed
        benchmarkSink = observed
        return observed
    }

    actual fun close() {
        nativeHeap.free(pointer)
    }
}
