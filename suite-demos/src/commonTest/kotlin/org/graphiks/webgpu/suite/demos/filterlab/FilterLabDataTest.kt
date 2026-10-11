@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.demos.filterlab

import kotlin.test.*

class FilterLabDataTest {
    @Test fun rejectsInvalidSettings() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.1f, 1.1f)) {
            assertFailsWith<IllegalArgumentException> { validateFilterSettings(FilterSettings(intensity = value)) }
            assertFailsWith<IllegalArgumentException> { validateFilterSettings(FilterSettings(edgeThreshold = value)) }
        }
        for (radius in listOf(0, 6)) assertFailsWith<IllegalArgumentException> {
            validateFilterSettings(FilterSettings(blurRadius = radius))
        }
        for (filter in ImageFilter.entries) validateFilterSettings(FilterSettings(filter))
    }

    @Test fun fitsExtremeAspectRatios() {
        assertEquals(2048 to 1024, fittedImageSize(4096, 2048, 2048))
        assertEquals(1 to 2048, fittedImageSize(1, 5000, 2048))
        assertEquals(17 to 9, fittedImageSize(17, 9, 2048))
        for ((w, h, limit) in listOf(Triple(0, 1, 2048), Triple(1, -1, 2048), Triple(1, 1, 0))) {
            assertFailsWith<IllegalArgumentException> { fittedImageSize(w, h, limit) }
        }
    }

    @Test fun rejectsInvalidImagesWithoutIntegerOverflow() {
        for (image in listOf(FilterImage(0, 1, ubyteArrayOf()), FilterImage(1, 1, ubyteArrayOf(1u)),
            FilterImage(65536, 65536, ubyteArrayOf()), FilterImage(Int.MAX_VALUE, Int.MAX_VALUE, ubyteArrayOf()))) {
            assertFailsWith<IllegalArgumentException> { validateFilterImage(image, Int.MAX_VALUE) }
        }
        assertFailsWith<IllegalArgumentException> { validateFilterImage(FilterImage(2, 1, UByteArray(8)), 1) }
        validateFilterImage(FilterImage(1, 1, ubyteArrayOf(1u, 2u, 3u, 4u)), 2048)
    }

    @Test fun embeddedSourceIsReproducibleAndIndependent() {
        val a = embeddedFilterImage(); val b = embeddedFilterImage()
        assertContentEquals(a.pixels, b.pixels)
        assertEquals(a.width.toLong() * a.height * 4, a.pixels.size.toLong())
        assertTrue(a.pixels.indices.filter { it % 4 == 3 }.all { a.pixels[it] == 255.toUByte() })
        a.pixels[0] = (a.pixels[0].toInt() xor 255).toUByte()
        assertNotEquals(a.pixels[0], b.pixels[0])
    }
}
