package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.*
import kotlin.test.*
import org.graphiks.webgpu.suite.demos.instancedcity.*

class CityControlsTest {
    @Test fun secondPointerCannotChangeAnOrbitOrEndItsDrag() {
        val c=CityControls(true)
        assertTrue(c.pointerDown(1,10.0,10.0)); assertFalse(c.pointerDown(2,100.0,100.0))
        val before=c.camera
        c.pointerMove(2,0.0,0.0); c.pointerEnd(2)
        assertEquals(before,c.camera); assertTrue(c.dragging)
        c.pointerMove(1,110.0,10.0)
        assertTrue(abs(c.camera.azimuth-before.azimuth-.5f)<.00001f)
        c.pointerMove(1,110.0,1e6); assertEquals((15*PI/180).toFloat(),c.camera.elevation)
        c.pointerMove(1,110.0,-1e6); assertEquals((75*PI/180).toFloat(),c.camera.elevation)
        val valid=c.camera; c.pointerMove(1,Double.NaN,0.0); assertEquals(valid,c.camera)
        c.clearPointer(); c.pointerMove(1,0.0,0.0); assertEquals(valid,c.camera)
        assertFalse(c.dragging); assertFalse(c.pointerDown(1,Double.NaN,0.0))
    }
    @Test fun invisibleFramesAndLongGapsDoNotJumpTheCamera() {
        val c=CityControls()
        c.advance(1000.0,true); val first=c.camera
        c.advance(2000.0,false); assertEquals(first,c.camera)
        c.advance(10000.0,true); assertEquals(first,c.camera)
        c.advance(10020.0,true)
        assertTrue(abs(c.camera.azimuth-first.azimuth-.002f)<.00001f)
        val before=c.camera; c.advance(20000.0,true)
        assertTrue(abs(c.camera.azimuth-before.azimuth-.005f)<.00001f)
        val last=c.camera; c.advance(0.0,true); c.advance(Double.NaN,true)
        c.advance(50000.0,true); assertEquals(last,c.camera)
        c.pointerDown(1,0.0,0.0); c.advance(60000.0,true); assertEquals(last,c.camera)
        c.pointerEnd(1); c.advance(61000.0,true); assertEquals(last,c.camera)
    }
    @Test fun wheelModesNormalizeAndClampWithoutAcceptingNonfiniteInput() {
        for ((delta,mode) in listOf(40.0 to 0,2.5 to 1,.1 to 2)) {
            val c=CityControls(true); c.wheel(delta,mode,400.0); assertEquals(184f,c.camera.distance)
        }
        val c=CityControls(true)
        c.wheel(1e6,0,400.0); assertEquals(300f,c.camera.distance)
        c.wheel(-1e6,0,400.0); assertEquals(100f,c.camera.distance)
        c.wheel(Double.NaN,0,400.0); c.wheel(40.0,3,400.0)
        c.wheel(40.0,2,-1.0); c.wheel(40.0,2,Double.NaN)
        assertEquals(100f,c.camera.distance)
        assertFailsWith<IllegalArgumentException> { c.setDistance(Float.NaN) }
    }
    @Test fun resetPreservesControlsAndReducedMotionIsOnlyTheInitialPreference() {
        val c=CityControls(true); assertFalse(c.rotating)
        c.density=CityDensity.Sparse; c.heightScale=2f; c.setDistance(100f)
        c.rotating=true; c.advance(1000.0,true); val first=c.camera
        c.advance(1020.0,true); assertNotEquals(first,c.camera)
        c.resetView(); assertEquals(CityCamera(),c.camera)
        assertEquals(CityDensity.Sparse,c.density); assertEquals(2f,c.heightScale); assertTrue(c.rotating)
        c.rotating=false; c.rotating=true; c.advance(3000.0,true); assertEquals(CityCamera(),c.camera)
        for(bad in listOf(.4f,2.1f,Float.NaN)) assertFailsWith<IllegalArgumentException> { c.heightScale=bad }
    }
}
