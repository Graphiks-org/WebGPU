package org.graphiks.webgpu.benchmarks

import org.graphiks.webgpu.ArrayBuffer

/**
 * Destination buffer for one scenario plus the unchecked accessors used by the [Variant.Reference]
 * kernels. The destination is allocated once per scenario and reused for every sample so the
 * measurement does not include allocation churn; the reference accessors mirror the platform
 * primitives the library uses today, without the new Graphiks checks.
 */
internal expect class BenchmarkMemory(bytes: Int) {
    val buffer: ArrayBuffer

    fun setIntReference(offset: Int, value: Int)

    fun getIntReference(offset: Int): Int

    fun setFloatReference(offset: Int, value: Float)

    fun setBytesReference(offset: Int, array: ByteArray)

    fun setFloatsReference(offset: Int, array: FloatArray)

    /** Opaque observation used to keep the timed work alive. */
    fun consume(seed: Int): Int

    fun close()
}
