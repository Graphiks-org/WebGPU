package org.graphiks.webgpu.suite.demos.instancedcity

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class CityCamera(
    val azimuth: Float = (PI / 4).toFloat(),
    val elevation: Float = (35 * PI / 180).toFloat(),
    val distance: Float = 180f,
)

fun validateCityCamera(camera: CityCamera) {
    require(camera.azimuth.isFinite() && camera.elevation.isFinite() && camera.distance.isFinite()) { "Camera must be finite" }
    require(camera.distance in 100f..300f) { "Camera distance must be in 100..300" }
    require(camera.elevation in (15 * PI / 180).toFloat()..(75 * PI / 180).toFloat()) { "Camera elevation must be in 15..75 degrees" }
}

/** Right-handed Y-up orbit; target (0,8,0). Returned column-major matrix maps depth to [0,1]. */
fun cityViewProjection(camera: CityCamera, width: Int, height: Int): FloatArray {
    validateCityCamera(camera)
    require(width > 0 && height > 0) { "Frame dimensions must be positive" }
    val sa=sin(camera.azimuth); val ca=cos(camera.azimuth)
    val se=sin(camera.elevation); val ce=cos(camera.elevation)
    val eyeX=camera.distance*ce*sa; val eyeY=8f+camera.distance*se; val eyeZ=camera.distance*ce*ca
    // z points from target to eye; x=cross(up,z), y=cross(z,x).
    val xx=ca; val xz=-sa
    val yx=-se*sa; val yy=ce; val yz=-se*ca
    val zx=ce*sa; val zy=se; val zz=ce*ca
    val view=floatArrayOf(xx,yx,zx,0f, 0f,yy,zy,0f, xz,yz,zz,0f,
        -(xx*eyeX+xz*eyeZ), -(yx*eyeX+yy*eyeY+yz*eyeZ), -(zx*eyeX+zy*eyeY+zz*eyeZ),1f)
    return multiplyCityMatrices(cityPerspective(width.toFloat()/height), view)
}

internal fun cityPerspective(aspect: Float): FloatArray {
    require(aspect.isFinite() && aspect > 0f)
    val f=1f/tan((25*PI/180).toFloat()); val near=.1f; val far=1024f
    return floatArrayOf(f/aspect,0f,0f,0f, 0f,f,0f,0f, 0f,0f,far/(near-far),-1f,
        0f,0f,far*near/(near-far),0f)
}

internal fun multiplyCityMatrices(a: FloatArray, b: FloatArray): FloatArray = FloatArray(16) { i ->
    val col=i/4; val row=i%4
    var value=0f
    for (k in 0..3) value+=a[k*4+row]*b[col*4+k]
    value
}
