package org.graphiks.webgpu.suite.acid.transfers

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
 * Copies a byte sub-range between two buffers and reads the destination back by mapping it.
 *
 * The source is written through its mapped-at-creation range; the copy starts at byte 4 of
 * the source and lands at byte 8 of the destination, so the surrounding words stay as the
 * sentinels the destination was created with.
 */
@AcidTest(
    id = AcidCaseId.TransfersCopyOffsets,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUCommandEncoder_finish,
        ApiSymbols.GPUQueue_submit,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_unmap,
    ],
)
suspend fun bufferCopyOffsets(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.CopySrc, mappedAtCreation = true),
    ).use { source ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
        ).use { destination ->
            source.getMappedRange().setUInts(0uL, uintArrayOf(10u, 20u, 30u, 40u))
            source.unmap()

            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(source, 4uL, destination, 8uL, 8uL)
                encoder.finish().use { commands -> device.queue.submit(listOf(commands)) }
            }

            destination.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                assertContentEquals(
                    uintArrayOf(0u, 0u, 20u, 30u),
                    destination.getMappedRange().toUIntArray(),
                )
            } finally {
                destination.unmap()
            }
        }
    }
}
