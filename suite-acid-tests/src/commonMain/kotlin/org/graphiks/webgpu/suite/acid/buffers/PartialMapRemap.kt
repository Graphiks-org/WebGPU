package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUSize64
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

/**
 * Mapped-range offsets are absolute buffer offsets, not offsets into the current mapping.
 *
 * An omitted size extends to the end of the buffer, not the end of the mapped sub-range.
 * Each successful view belongs to a separate mapping and is never reused after `unmap`.
 * Finally, mapping the whole buffer again verifies that all four original words survived.
 *
 * See [WebGPU buffer mapping](https://www.w3.org/TR/webgpu/#dom-gpubuffer-getmappedrange).
 */
@AcidTest(
    id = AcidCaseId.BuffersPartialMapRemap,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
        ApiSymbols.GPUBuffer_mapState,
    ],
)
suspend fun partialMapping(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.CopySrc, mappedAtCreation = true),
    ).use { source ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
        ).use { staging ->
            source.getMappedRange().setUInts(0uL, uintArrayOf(11u, 22u, 33u, 44u))
            source.unmap()
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(source, 0uL, staging, 0uL, 16uL)
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            staging.checkPartialMapping(8uL) {
                assertFails("Offset zero is before the mapped range [8, 16), not relative to it") {
                    staging.getMappedRange(0uL, 8uL)
                }
                assertFails("The default offset zero is outside the mapped range [8, 16)") {
                    staging.getMappedRange()
                }
                assertContentEquals(uintArrayOf(33u, 44u), staging.getMappedRange(8uL, 8uL).toUIntArray())
            }

            // Remap before requesting another view of the same bytes: mapped views may not overlap.
            staging.checkPartialMapping(8uL) {
                assertContentEquals(
                    uintArrayOf(33u, 44u),
                    staging.getMappedRange(8uL).toUIntArray(),
                    "Omitting size at offset 8 exposes the remaining eight buffer bytes",
                )
            }

            staging.checkPartialMapping(4uL) {
                assertFails("An explicit range within the buffer must still fit the mapping [8, 12)") {
                    staging.getMappedRange(8uL, 8uL)
                }
                assertFails("Omitting size requests [8, 16), not just the mapped range [8, 12)") {
                    staging.getMappedRange(8uL)
                }
                assertContentEquals(uintArrayOf(33u), staging.getMappedRange(8uL, 4uL).toUIntArray())
            }

            assertEquals(GPUBufferMapState.Unmapped, staging.mapState)
            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                assertContentEquals(uintArrayOf(11u, 22u, 33u, 44u), staging.getMappedRange().toUIntArray())
            } finally {
                staging.unmap()
            }
        }
    }
}

private suspend fun GPUBuffer.checkPartialMapping(size: GPUSize64, assertions: () -> Unit) {
    mapAsync(GPUMapMode.Read, 8uL, size).getOrThrow()
    try {
        assertEquals(GPUBufferMapState.Mapped, mapState)
        assertions()
    } finally {
        unmap()
    }
}
