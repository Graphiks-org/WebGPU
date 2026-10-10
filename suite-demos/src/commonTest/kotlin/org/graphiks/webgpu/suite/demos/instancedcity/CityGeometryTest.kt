@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.demos.instancedcity

import kotlin.test.*

class CityGeometryTest {
    @Test fun indexedTrianglesFaceOutwardsAndCoverSixFaces() {
        val v = cityCubeVertices(); val indices = cityCubeIndices()
        assertEquals(192, v.size); assertEquals(36, indices.size)
        val normals = mutableSetOf<List<Float>>()
        for (i in v.indices step 8) {
            assertTrue(v[i] in -.5f.. .5f); assertTrue(v[i + 1] in 0f..1f)
            assertTrue(v[i + 2] in -.5f.. .5f)
            assertTrue(v[i + 6] in 0f..1f && v[i + 7] in 0f..1f)
            normals.add(listOf(v[i + 3], v[i + 4], v[i + 5]).map { if (it == 0f) 0f else it })
        }
        assertEquals(setOf(listOf(1f,0f,0f),listOf(-1f,0f,0f),listOf(0f,1f,0f),
            listOf(0f,-1f,0f),listOf(0f,0f,1f),listOf(0f,0f,-1f)), normals)
        for (i in indices.indices step 3) {
            val a = indices[i].toInt() * 8; val b = indices[i + 1].toInt() * 8
            val c = indices[i + 2].toInt() * 8
            assertTrue(a < 192 && b < 192 && c < 192)
            val abx=v[b]-v[a]; val aby=v[b+1]-v[a+1]; val abz=v[b+2]-v[a+2]
            val acx=v[c]-v[a]; val acy=v[c+1]-v[a+1]; val acz=v[c+2]-v[a+2]
            val dot=(aby*acz-abz*acy)*v[a+3]+(abz*acx-abx*acz)*v[a+4]+(abx*acy-aby*acx)*v[a+5]
            assertTrue(dot > 0f, "Triangle $i must face outward")
        }
        v[0] = 99f; indices[0] = 99u
        assertTrue(cityCubeVertices()[0] in -.5f.. .5f)
        assertTrue(cityCubeIndices()[0] < 24u)
    }
}
