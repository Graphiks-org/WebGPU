package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * A buffer created with [BufferDescriptor.mappedAtCreation] exposes its whole mapping
 * immediately and reports the mapped state until the case unmaps it.
 */
suspend fun mappedAtCreation(device: GPUDevice) {
    val buffer = device.createBuffer(
        BufferDescriptor(
            size = 16uL,
            usage = GPUBufferUsage.CopySrc,
            mappedAtCreation = true,
        ),
    )
    try {
        assertEquals(16uL, buffer.size)
        assertEquals(setOf(GPUBufferUsage.CopySrc), buffer.usage)
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)

        val range = buffer.getMappedRange()
        range.setUInts(0uL, uintArrayOf(11u, 22u, 33u, 44u))
        assertContentEquals(uintArrayOf(11u, 22u, 33u, 44u), range.toUIntArray())

        buffer.unmap()
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
    } finally {
        buffer.close()
    }
}
