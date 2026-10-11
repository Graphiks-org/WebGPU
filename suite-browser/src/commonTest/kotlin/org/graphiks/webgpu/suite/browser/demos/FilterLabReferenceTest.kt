@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.test.*
import org.graphiks.webgpu.suite.demos.filterlab.*

class FilterLabReferenceTest {
    private val pixel = FilterImage(1, 1, ubyteArrayOf(10u, 20u, 30u, 40u))
    @Test fun inversionPreservesAlpha() {
        assertContentEquals(ubyteArrayOf(245u, 235u, 225u, 40u), filterReference(pixel, FilterSettings(ImageFilter.Invert)))
    }
    @Test fun zeroIntensityPreservesSource() {
        for (filter in ImageFilter.entries) assertContentEquals(pixel.pixels, filterReference(pixel, FilterSettings(filter, intensity = 0f)))
    }
    @Test fun constantBlurPreservesColor() {
        for (radius in listOf(1, 5)) assertContentEquals(pixel.pixels,
            filterReference(pixel, FilterSettings(ImageFilter.GaussianBlur, blurRadius = radius)))
    }
    @Test fun constantSobelIsBlack() {
        assertContentEquals(ubyteArrayOf(0u, 0u, 0u, 40u), filterReference(pixel, FilterSettings(ImageFilter.Sobel)))
    }
    @Test fun grayscaleHasKnownLuminance() {
        // 10*0.2126 + 20*0.7152 + 30*0.0722 = 18.596, rounded to 19.
        assertContentEquals(ubyteArrayOf(19u, 19u, 19u, 40u), filterReference(pixel, FilterSettings()))
    }
}
