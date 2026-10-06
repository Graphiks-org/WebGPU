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
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import org.graphiks.webgpu.withMappedRange
import kotlin.test.assertEquals

/**
 * `withMappedRange` maps a write range, runs the block with the borrowed view and unmaps on
 * exit: the buffer is back to `Unmapped` afterwards, and the written pattern reads back through
 * a staging buffer.
 */
@AcidTest(
    id = AcidCaseId.BuffersMappedRangeScope,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
        ApiSymbols.GPUBuffer_mapState,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUQueue_submit,
        ApiSymbols.GPUBufferUsage_MapWrite,
        ApiSymbols.GPUBufferUsage_CopySrc,
        ApiSymbols.GPUBufferUsage_CopyDst,
    ],
)
suspend fun mappedRangeScope(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc),
    ).use { buffer ->
        buffer.withMappedRange(GPUMapMode.Write) { view ->
            assertEquals(GPUBufferMapState.Mapped, buffer.mapState)
            view.setUInts(0uL, uintArrayOf(11u, 22u, 33u, 44u))
        }
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)

        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
        ).use { staging ->
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(buffer, 0uL, staging, 0uL, 16uL)
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
            val bytes = readBufferBytes(device, staging, 16uL)
            assertEquals(11, bytes[0].toInt() and 255)
            assertEquals(22, bytes[4].toInt() and 255)
            assertEquals(33, bytes[8].toInt() and 255)
            assertEquals(44, bytes[12].toInt() and 255)
        }
    }
}
