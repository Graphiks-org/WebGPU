package org.graphiks.webgpu.benchmarks

/**
 * Deterministic word pattern. The `Int` overflow is intentional and part of the fixture.
 */
internal fun word(index: Int, seed: Int): Int = index xor (0x13579bdf + seed)

internal fun scalarFloat(index: Int, seed: Int): Float = ((index + seed) and 1023).toFloat()

/**
 * Position of `index` in a fixed permutation of `0 until count`. `count` must be a power of two so
 * that the odd multiplier forms a permutation.
 */
internal fun scatteredIndex(index: Int, count: Int): Int = (index * 40503) and (count - 1)

internal fun vertexFloat(vertex: Int, component: Int, seed: Int): Float =
    ((vertex * 8 + component + seed) and 1023).toFloat()

/** RGBA8 bytes for a `width x height` image. Four bytes are written per pixel. */
internal fun rgbaBytes(width: Int, height: Int, seed: Int): ByteArray {
    val out = ByteArray(width * height * 4)
    var i = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            out[i++] = ((x + seed) and 255).toByte()
            out[i++] = ((y + seed) and 255).toByte()
            out[i++] = ((x xor y) and 255).toByte()
            out[i++] = 0xFF.toByte()
        }
    }
    return out
}

internal fun vertexFloats(count: Int, seed: Int): FloatArray =
    FloatArray(count * VERTEX_FLOATS) { vertexFloat(it / VERTEX_FLOATS, it % VERTEX_FLOATS, seed) }
