package org.graphiks.webgpu.suite.demos.instancedcity

import kotlin.test.*

class CityDataTest {
    @Test fun gridsFitTheSectorAndAreReproducible() {
        for ((density, edge, step) in listOf(Triple(CityDensity.Sparse, 60f, 8f),
            Triple(CityDensity.Medium, 62f, 4f), Triple(CityDensity.Dense, 63f, 2f))) {
            val a = initialCityInstances(density)
            assertEquals(density.count * 8, a.size)
            assertEquals(step, cityGridStep(density))
            assertContentEquals(a, initialCityInstances(density))
            assertEquals(-edge, a[0]); assertEquals(-edge, a[1])
            assertEquals(edge, a[a.size - 8]); assertEquals(edge, a[a.size - 7])
            validateCityInstances(a)
            val other = initialCityInstances(density, 2u)
            assertNotEquals(a[2], other[2])
            for (i in a.indices step 8) {
                assertEquals(a[i], other[i]); assertEquals(a[i + 1], other[i + 1])
                assertTrue(a[i + 2] in step * .45f..step * .75f)
                assertTrue(a[i + 3] in step * .45f..step * .75f)
                assertTrue(a[i + 4] in 2f..14f)
                assertTrue(a[i + 5] == 0f || a[i + 5] == 1f)
                assertTrue(a[i + 6] in 0f..65535f)
                assertEquals(a[i + 6].toInt().toFloat(), a[i + 6])
            }
            a[0] = 0f
            assertEquals(-edge, initialCityInstances(density)[0])
        }
    }
    @Test fun invalidFootprintsAndMetadataAreRejected() {
        val good = floatArrayOf(0f, 0f, 4f, 4f, 8f, 0f, 12f, 0f)
        validateCityInstances(good)
        for ((offset, bad) in listOf(0 to 64f, 1 to -64f, 2 to 0f, 3 to Float.NaN,
            4 to 15f, 4 to 1f, 5 to .5f, 6 to 65536f, 6 to 1.5f, 7 to 1f)) {
            val data = good.copyOf(); data[offset] = bad
            assertFailsWith<IllegalArgumentException> { validateCityInstances(data) }
        }
        for (i in good.indices) for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY)) {
            val data = good.copyOf(); data[i] = bad
            assertFailsWith<IllegalArgumentException> { validateCityInstances(data) }
        }
        assertFailsWith<IllegalArgumentException> { validateCityInstances(FloatArray(0)) }
        assertFailsWith<IllegalArgumentException> { validateCityInstances(FloatArray(9)) }
    }
}
