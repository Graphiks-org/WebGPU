@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.demos.filterlab

import kotlin.math.min

enum class ImageFilter { Grayscale, Sepia, Invert, GaussianBlur, Sobel }

data class FilterSettings(
    val filter: ImageFilter = ImageFilter.Grayscale,
    val intensity: Float = 1f,
    val blurRadius: Int = 3,
    val edgeThreshold: Float = 0.2f,
)

/** Straight-alpha RGBA8 pixels in row-major order. The caller retains the supplied array. */
data class FilterImage(val width: Int, val height: Int, val pixels: UByteArray)

fun validateFilterSettings(settings: FilterSettings) {
    require(settings.intensity.isFinite() && settings.intensity in 0f..1f) { "Intensity must be in 0..1." }
    require(settings.edgeThreshold.isFinite() && settings.edgeThreshold in 0f..1f) { "Edge threshold must be in 0..1." }
    require(settings.blurRadius in 1..5) { "Blur radius must be in 1..5." }
}

fun validateFilterImage(image: FilterImage, maxDimension: Int) {
    require(maxDimension > 0 && image.width in 1..maxDimension && image.height in 1..maxDimension) {
        "Image dimensions must fit the device limit."
    }
    // Dividing before multiplication also avoids overflow for Int.MAX_VALUE squared times four.
    val pixels = image.width.toLong() * image.height
    require(pixels <= Int.MAX_VALUE / 4 && image.pixels.size.toLong() == pixels * 4) {
        "Image must contain exactly width * height * 4 RGBA bytes."
    }
}

fun fittedImageSize(width: Int, height: Int, maxDimension: Int): Pair<Int, Int> {
    require(width > 0 && height > 0 && maxDimension > 0)
    val scale = min(1.0, maxDimension.toDouble() / maxOf(width, height))
    return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
}

/** Original illustration with gradients, sharp edges and colored shapes; no external asset. */
fun embeddedFilterImage(): FilterImage {
    val width = 512
    val height = 384
    val bytes = UByteArray(width * height * 4)
    for (y in 0 until height) for (x in 0 until width) {
        var r = x * 255 / (width - 1)
        var g = y * 255 / (height - 1)
        var b = if ((x / 32 + y / 32) % 2 == 0) 210 else 40
        if ((x - 150) * (x - 150) + (y - 160) * (y - 160) < 80 * 80) { r = 255; g = 190; b = 30 }
        if (x in 290..420 && y in 100..250) { r = 30; g = 220; b = 240 }
        val i = (y * width + x) * 4
        bytes[i] = r.toUByte(); bytes[i + 1] = g.toUByte(); bytes[i + 2] = b.toUByte(); bytes[i + 3] = 255u
    }
    return FilterImage(width, height, bytes)
}
