package org.graphiks.webgpu.suite.acid.transfers

import org.graphiks.webgpu.ArrayBuffer
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

/**
 * Writes two words into the middle of a buffer from a data offset, then copies the whole
 * buffer into a staging buffer to read the result.
 */
@AcidTest(
    id = AcidCaseId.TransfersWriteOffsets,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUQueue_writeBuffer,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
    ],
)
suspend fun queueWriteOffsets(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
    ).use { buffer ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
        ).use { staging ->
            val data = ArrayBuffer.of(uintArrayOf(10u, 20u, 30u, 40u))
            device.queue.writeBuffer(buffer, 4uL, data, 8uL, 8uL)
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(buffer, 0uL, staging, 0uL, 16uL)
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                assertContentEquals(
                    uintArrayOf(0u, 30u, 40u, 0u),
                    staging.getMappedRange().toUIntArray(),
                )
            } finally {
                staging.unmap()
            }
        }
    }
}
