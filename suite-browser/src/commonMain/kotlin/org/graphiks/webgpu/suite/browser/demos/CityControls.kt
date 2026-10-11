package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.PI
import org.graphiks.webgpu.suite.demos.instancedcity.CityCamera
import org.graphiks.webgpu.suite.demos.instancedcity.CityDensity

/** Pure input and animation state. DOM capture and GPU ownership remain in the page. */
internal class CityControls(reducedMotion: Boolean=false) {
    var density=CityDensity.Dense
    var heightScale=1f
        set(value) { require(value.isFinite() && value in .5f..2f); field=value }
    var rotating=!reducedMotion
        set(value) { field=value; resetClock() }
    var camera=CityCamera()
        private set
    private var pointer: Int?=null
    private var lastX=0.0
    private var lastY=0.0
    private var previous: Double?=null
    val dragging: Boolean get()=pointer!=null

    fun setDistance(distance: Float) {
        require(distance.isFinite())
        camera=camera.copy(distance=distance.coerceIn(100f,300f))
    }
    fun resetView() { camera=CityCamera(); resetClock() }
    fun pointerDown(id: Int,x: Double,y: Double): Boolean {
        if(pointer!=null || !x.isFinite() || !y.isFinite()) return false
        pointer=id; lastX=x; lastY=y; resetClock(); return true
    }
    fun pointerMove(id: Int,x: Double,y: Double) {
        if(pointer!=id || !x.isFinite() || !y.isFinite()) return
        val azimuth=normalizeAngle(camera.azimuth.toDouble()+(x-lastX)*.005)
        val elevation=(camera.elevation.toDouble()-(y-lastY)*.005)
            .coerceIn(15*PI/180,75*PI/180).toFloat()
        camera=camera.copy(azimuth=azimuth,elevation=elevation)
        lastX=x; lastY=y
    }
    fun pointerEnd(id: Int) { if(pointer==id) clearPointer() }
    fun clearPointer() { pointer=null; resetClock() }
    fun wheel(deltaY: Double,deltaMode: Int,viewportHeight: Double) {
        if(!deltaY.isFinite()) return
        val pixels=when(deltaMode) {
            0->deltaY
            1->deltaY*16
            2->if(viewportHeight.isFinite() && viewportHeight>0) deltaY*viewportHeight else return
            else->return
        }
        if(!pixels.isFinite()) return
        setDistance((camera.distance+pixels*.1).coerceIn(100.0,300.0).toFloat())
    }
    fun advance(timestampMs: Double,visible: Boolean): Boolean {
        if(!timestampMs.isFinite() || !visible || dragging || !rotating) { resetClock(); return false }
        val last=previous
        if(last!=null && timestampMs<last) { resetClock(); return false }
        previous=timestampMs
        if(last==null) return false
        val delta=((timestampMs-last)/1000).coerceIn(0.0,.05)*.1
        if(delta==0.0) return false
        camera=camera.copy(azimuth=normalizeAngle(camera.azimuth+delta))
        return true
    }
    fun resetClock() { previous=null }
}

private fun normalizeAngle(angle: Double): Float = (((angle % (2*PI)) + 2*PI) % (2*PI)).toFloat()
