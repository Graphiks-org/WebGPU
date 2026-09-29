package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * A 32-byte buffer mapped at creation exposes two non-overlapping 16-byte views. Each view is
 * written through its own offset, the views report their exact sizes, and after `unmap` the eight
 * u32 values read back in order.
 */
@AcidTest(
    id = AcidCaseId.BuffersDisjointMappedWrites,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
        ApiSymbols.ArrayBuffer_setUInts,
        ApiSymbols.GPUBuffer_size,
        ApiSymbols.GPUDevice_createBuffer,
    ],
)
suspend fun disjointMappedWrites(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(
            size = 32uL,
            usage = GPUBufferUsage.CopySrc,
            mappedAtCreation = true,
        ),
    ).use { buffer ->
        val first = buffer.getMappedRange(0uL, 16uL)
        assertEquals(16uL, first.size, "The first mapped view spans 16 bytes")
        first.setUInts(0uL, uintArrayOf(1u, 2u, 3u, 4u))

        val second = buffer.getMappedRange(16uL, 16uL)
        assertEquals(16uL, second.size, "The second mapped view spans 16 bytes")
        second.setUInts(0uL, uintArrayOf(5u, 6u, 7u, 8u))

        buffer.unmap()
        // The views must not be used after unmap.
        assertContentEquals(
            uintArrayOf(1u, 2u, 3u, 4u, 5u, 6u, 7u, 8u),
            ArrayBuffer.of(readBufferBytes(device, buffer, 32uL)).toUIntArray(),
            "Both disjoint mapped views must reach the buffer",
        )
    }
}
