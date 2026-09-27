package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Copies a byte sub-range between two buffers and reads the destination back by mapping it.
 *
 * The source is written through its mapped-at-creation range; the copy starts at byte 4 of
 * the source and lands at byte 8 of the destination, so the surrounding words stay as the
 * sentinels the destination was created with.
 */
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

/**
 * Writes two words into the middle of a buffer from a data offset, then copies the whole
 * buffer into a staging buffer to read the result.
 */
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

/**
 * The same write as [queueWriteOffsets] with the size omitted: everything from the data
 * offset to the end of the data is written.
 */
suspend fun queueWriteRemaining(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
    ).use { buffer ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
        ).use { staging ->
            val data = ArrayBuffer.of(uintArrayOf(10u, 20u, 30u, 40u))
            device.queue.writeBuffer(buffer, 0uL, data, 8uL)
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(buffer, 0uL, staging, 0uL, 16uL)
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                assertContentEquals(
                    uintArrayOf(30u, 40u, 0u, 0u),
                    staging.getMappedRange().toUIntArray(),
                )
            } finally {
                staging.unmap()
            }
        }
    }
}

/**
 * Maps an 8-byte sub-range, reads it, unmaps, then maps the whole buffer again.
 *
 * The first view is never reused after `unmap`; the second mapping reads the whole buffer
 * from a fresh view.
 */
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
