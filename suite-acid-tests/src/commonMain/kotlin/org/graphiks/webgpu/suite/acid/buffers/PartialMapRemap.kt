package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Maps an 8-byte sub-range, reads it, unmaps, then maps the whole buffer again.
 *
 * The first view is never reused after `unmap`; the second mapping reads the whole buffer
 * from a fresh view.
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

            staging.mapAsync(GPUMapMode.Read, 8uL, 8uL).getOrThrow()
            try {
                assertEquals(GPUBufferMapState.Mapped, staging.mapState)
                assertContentEquals(uintArrayOf(33u, 44u), staging.getMappedRange(8uL, 8uL).toUIntArray())
            } finally {
                staging.unmap()
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
