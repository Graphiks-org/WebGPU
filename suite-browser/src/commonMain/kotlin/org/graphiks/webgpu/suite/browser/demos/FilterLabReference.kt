@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.*
import org.graphiks.webgpu.suite.demos.filterlab.*

/** Independent Double-precision oracle, not used by the interactive page. */
internal fun filterReference(image: FilterImage, settings: FilterSettings): UByteArray {
    validateFilterImage(image, Int.MAX_VALUE)
    validateFilterSettings(settings)
    fun rgb(x: Int, y: Int): DoubleArray {
        val i = (y.coerceIn(0, image.height - 1) * image.width + x.coerceIn(0, image.width - 1)) * 4
        return DoubleArray(3) { image.pixels[i + it].toDouble() / 255.0 }
    }
    fun luma(x: Int, y: Int): Double = rgb(x, y).let { it[0] * 0.2126 + it[1] * 0.7152 + it[2] * 0.0722 }
    val output = UByteArray(image.pixels.size)
    for (y in 0 until image.height) for (x in 0 until image.width) {
        val original = rgb(x, y)
        val filtered = when (settings.filter) {
            ImageFilter.Grayscale -> DoubleArray(3) { luma(x, y) }
            ImageFilter.Sepia -> doubleArrayOf(
                original[0]*0.393 + original[1]*0.769 + original[2]*0.189,
                original[0]*0.349 + original[1]*0.686 + original[2]*0.168,
                original[0]*0.272 + original[1]*0.534 + original[2]*0.131,
            ).map { it.coerceIn(0.0, 1.0) }.toDoubleArray()
            ImageFilter.Invert -> DoubleArray(3) { 1.0 - original[it] }
            ImageFilter.GaussianBlur -> {
                val radius = settings.blurRadius
                val sigma = max(0.5, radius / 2.0)
                val sum = DoubleArray(3)
                var total = 0.0
                for (dy in -radius..radius) for (dx in -radius..radius) {
                    val weight = exp(-(dx*dx + dy*dy) / (2 * sigma * sigma))
                    val sample = rgb(x + dx, y + dy)
                    for (c in 0..2) sum[c] += sample[c] * weight
                    total += weight
                }
                DoubleArray(3) { sum[it] / total }
            }
            ImageFilter.Sobel -> {
                val gx = -luma(x-1,y-1) + luma(x+1,y-1) - 2*luma(x-1,y) + 2*luma(x+1,y) - luma(x-1,y+1) + luma(x+1,y+1)
                val gy = -luma(x-1,y-1) - 2*luma(x,y-1) - luma(x+1,y-1) + luma(x-1,y+1) + 2*luma(x,y+1) + luma(x+1,y+1)
                val magnitude = (sqrt(gx*gx + gy*gy) / 4).coerceIn(0.0, 1.0)
                DoubleArray(3) { if (magnitude < settings.edgeThreshold) 0.0 else magnitude }
            }
        }
        val i = (y * image.width + x) * 4
        for (c in 0..2) output[i+c] = ((original[c] + (filtered[c]-original[c])*settings.intensity) * 255).roundToInt().coerceIn(0,255).toUByte()
        output[i+3] = image.pixels[i+3]
    }
    return output
}
