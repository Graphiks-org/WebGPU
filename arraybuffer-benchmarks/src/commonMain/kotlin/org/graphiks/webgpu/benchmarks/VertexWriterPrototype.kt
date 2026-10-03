package org.graphiks.webgpu.benchmarks

/** Bytes touched by `count` interleaved `p3n3uv2` vertices (eight floats each). */
internal fun vertexTouchedBytes(count: Int): Long {
    require(count >= 0) { "count must not be negative" }
    return count.toLong() * VERTEX_BYTES
}

/**
 * Prevalidates the whole layout once and requires a 4-byte aligned base, then performs the eight
 * float stores per vertex unchecked.
 */
internal fun fillVerticesPrototype(memory: BenchmarkMemory, base: Int, count: Int, seed: Int) {
    require(base >= 0) { "base must not be negative" }
    require(base % Float.SIZE_BYTES == 0) { "base must be ${Float.SIZE_BYTES}-byte aligned" }
    val touched = vertexTouchedBytes(count)
    checkWriterRange(memory.buffer.size, base.toULong(), touched.toULong())
    if (touched == 0L) return
    fillVerticesUnchecked(memory, base, count, seed)
}

internal expect fun fillVerticesUnchecked(memory: BenchmarkMemory, base: Int, count: Int, seed: Int)
