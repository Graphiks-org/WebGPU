package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

/**
 * Two CPU-visible mapped ranges may not overlap: after mapping `[0, 16)`, requesting `[8, 24)` is
 * rejected synchronously and is not an error-scope event. An adjacent `[16, 32)` view is still
 * valid, and the sentinels written through it are readable after `unmap`.
 */
@AcidTest(
    id = AcidCaseId.ErrorsOverlappingMappedRanges,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.ArrayBuffer_setUInts,
    ],
)
suspend fun overlappingMappedRanges(device: GPUDevice) {
    device.createBuffer(
        BufferDescriptor(
            size = 32uL,
            usage = GPUBufferUsage.CopySrc,
            mappedAtCreation = true,
        ),
    ).use { buffer ->
        device.pushErrorScope(GPUErrorFilter.Validation)
        val adjacent = try {
            buffer.getMappedRange(0uL, 16uL)
            assertFails("An overlapping CPU-visible range must be rejected") {
                buffer.getMappedRange(8uL, 16uL)
            }
            // Keep the adjacent view for the sentinel writes; do not request it twice.
            buffer.getMappedRange(16uL, 16uL).also { view ->
                assertEquals(16uL, view.size, "The adjacent view spans 16 bytes")
            }
        } finally {
            assertNull(device.popErrorScope().getOrThrow(), "Overlapping range rejection is not a scope error")
        }

        adjacent.setUInts(0uL, uintArrayOf(9u, 9u, 9u, 9u))
        buffer.unmap()

        assertContentEquals(
            uintArrayOf(0u, 0u, 0u, 0u, 9u, 9u, 9u, 9u),
            ArrayBuffer.of(readBufferBytes(device, buffer, 32uL)).toUIntArray(),
            "The untouched first half stays zero and the adjacent view's sentinels survive",
        )
    }
}
