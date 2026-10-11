package org.graphiks.webgpu.suite.demos.instancedcity

import kotlin.math.abs
import kotlin.math.sqrt

const val CityInstanceFloats = 8
const val CityInstanceBytes = 32

enum class CityDensity(val count: Int, val side: Int) {
    Sparse(256, 16), Medium(1024, 32), Dense(4096, 64),
}

fun cityGridStep(density: CityDensity): Float = 128f / density.side

/** Fresh deterministic [x,z,width,depth,height,color,pattern,0] instances in a 128-unit sector. */
fun initialCityInstances(density: CityDensity, seed: UInt = 1u): FloatArray {
    var state = seed
    fun next(): UInt { state = state * 1664525u + 1013904223u; return state }
    fun unit(): Float = (next() shr 8).toFloat() / 16777216f
    val step = cityGridStep(density)
    val result = FloatArray(density.count * CityInstanceFloats)
    for (z in 0 until density.side) for (x in 0 until density.side) {
        val cx = -64f + (x + .5f) * step
        val cz = -64f + (z + .5f) * step
        val width = step * (.45f + .30f * unit())
        val depth = step * (.45f + .30f * unit())
        val centrality = (1f - sqrt(cx * cx + cz * cz) / 90.51f).coerceIn(0f, 1f)
        val height = 2f + 12f * (.25f + .75f * centrality) * unit()
        val color = (next() and 1u).toFloat()
        val pattern = (next() and 65535u).toFloat()
        floatArrayOf(cx, cz, width, depth, height, color, pattern, 0f)
            .copyInto(result, (z * density.side + x) * CityInstanceFloats)
    }
    return result
}

/** Validate before allocating or uploading. Small independently positioned fixtures are supported. */
fun validateCityInstances(instances: FloatArray) {
    require(instances.isNotEmpty() && instances.size % CityInstanceFloats == 0) { "Expected nonempty eight-float city instances" }
    require(instances.all { it.isFinite() }) { "City instances must be finite" }
    for (i in instances.indices step CityInstanceFloats) {
        require(instances[i + 2] > 0f && instances[i + 3] > 0f) { "Building dimensions must be positive" }
        require(abs(instances[i]) + instances[i + 2] / 2f <= 64f &&
            abs(instances[i + 1]) + instances[i + 3] / 2f <= 64f) { "Building footprint exceeds sector" }
        require(instances[i + 4] in 2f..14f) { "Building height must be in 2..14" }
        require(instances[i + 5] == 0f || instances[i + 5] == 1f) { "Building color must be 0 or 1" }
        val pattern = instances[i + 6]
        require(pattern in 0f..65535f && pattern == pattern.toInt().toFloat()) { "Invalid window pattern" }
        require(instances[i + 7] == 0f) { "Expected zero padding" }
    }
}
