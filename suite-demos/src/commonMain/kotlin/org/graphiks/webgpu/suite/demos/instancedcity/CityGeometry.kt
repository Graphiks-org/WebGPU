@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.demos.instancedcity

/** Indexed unit cube with separate face normals/UVs, standing on Y=0. */
fun cityCubeVertices(): FloatArray {
    val faces = listOf(
        floatArrayOf(0f,.5f,.5f, 1f,0f,0f, 0f,1f,0f),
        floatArrayOf(0f,.5f,-.5f, -1f,0f,0f, 0f,1f,0f),
        floatArrayOf(.5f,.5f,0f, 0f,0f,-1f, 0f,1f,0f),
        floatArrayOf(-.5f,.5f,0f, 0f,0f,1f, 0f,1f,0f),
        floatArrayOf(0f,1f,0f, 1f,0f,0f, 0f,0f,-1f),
        floatArrayOf(0f,0f,0f, 1f,0f,0f, 0f,0f,1f),
    )
    val vertices = FloatArray(24 * 8)
    val uv = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)
    for ((face, f) in faces.withIndex()) {
        val nx = f[4]*f[8]-f[5]*f[7]
        val ny = f[5]*f[6]-f[3]*f[8]
        val nz = f[3]*f[7]-f[4]*f[6]
        for ((corner, coords) in uv.withIndex()) {
            val i = (face * 4 + corner) * 8
            for (axis in 0..2) vertices[i + axis] = f[axis] + (coords.first-.5f)*f[3+axis] + (coords.second-.5f)*f[6+axis]
            vertices[i+3]=nx; vertices[i+4]=ny; vertices[i+5]=nz
            vertices[i+6]=coords.first; vertices[i+7]=coords.second
        }
    }
    return vertices
}

fun cityCubeIndices(): UShortArray = UShortArray(36) { i ->
    (i / 6 * 4 + intArrayOf(0, 1, 2, 0, 2, 3)[i % 6]).toUShort()
}
