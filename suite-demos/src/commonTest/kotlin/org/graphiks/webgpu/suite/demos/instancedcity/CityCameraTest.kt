package org.graphiks.webgpu.suite.demos.instancedcity

import kotlin.math.*
import kotlin.test.*

class CityCameraTest {
    @Test fun perspectiveMapsNearAndFarToWebGpuDepth() {
        val p=cityPerspective(1f)
        fun depth(z: Float)=(p[10]*z+p[14])/(p[11]*z+p[15])
        assertTrue(abs(depth(-.1f)) < .00001f)
        assertTrue(abs(depth(-1024f)-1f) < .00001f)
        assertTrue(abs(cityPerspective(2f)[0]*2f-p[0]) < .00001f)
    }
    @Test fun orbitTargetProjectsToCenterWithPositiveUpAndRight() {
        val m=cityViewProjection(CityCamera(0f,(PI/4).toFloat(),180f),256,256)
        assertTrue(abs(m[4]*8f+m[12]) < .0001f)
        assertTrue(abs(m[5]*8f+m[13]) < .0001f)
        assertTrue(abs(m[7]*8f+m[15]-180f) < .0001f)
        assertTrue(m[0]*10f+m[4]*8f+m[12] > 0f)
        assertTrue(m[5]*10f+m[13] > 0f)
        for (e in listOf(15f,75f)) for (d in listOf(100f,300f)) {
            assertTrue(cityViewProjection(CityCamera(0f,(e*PI/180).toFloat(),d),390,844).all { it.isFinite() })
        }
    }
    @Test fun invalidCameraAndDimensionsAreRejectedBeforeDivision() {
        for (bad in listOf(CityCamera(distance=99f),CityCamera(distance=301f),
            CityCamera(elevation=14f*PI.toFloat()/180f),CityCamera(elevation=76f*PI.toFloat()/180f),
            CityCamera(azimuth=Float.NaN),CityCamera(distance=Float.POSITIVE_INFINITY))) {
            assertFailsWith<IllegalArgumentException> { cityViewProjection(bad,256,256) }
        }
        assertFailsWith<IllegalArgumentException> { cityViewProjection(CityCamera(),0,256) }
        assertFailsWith<IllegalArgumentException> { cityViewProjection(CityCamera(),256,0) }
    }
}
