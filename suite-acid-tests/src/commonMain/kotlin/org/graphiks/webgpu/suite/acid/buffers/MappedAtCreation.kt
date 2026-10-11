package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * A buffer created with [BufferDescriptor.mappedAtCreation] exposes its whole mapping
 * immediately and reports the mapped state until the case unmaps it.
 */
@AcidTest(
    id = AcidCaseId.BuffersMappedAtCreation,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
        ApiSymbols.GPUBuffer_size,
        ApiSymbols.GPUBuffer_usage,
        ApiSymbols.GPUBuffer_mapState,
    ],
)
suspend fun mappedAtCreation(device: GPUDevice) = withValidationScope(device) {
    val buffer = device.createBuffer(
        BufferDescriptor(
            size = 16uL,
            usage = GPUBufferUsage.CopySrc,
            mappedAtCreation = true,
        ),
    )
    try {
        assertEquals(16uL, buffer.size)
        assertEquals(GPUBufferUsage.CopySrc, buffer.usage)
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
