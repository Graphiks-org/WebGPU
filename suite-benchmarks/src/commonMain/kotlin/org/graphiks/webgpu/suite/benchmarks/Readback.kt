package org.graphiks.webgpu.suite.benchmarks

import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode

/**
 * Copies [byteSize] bytes from [source] into [staging] and returns them as u32 words.
 *
 * [staging] must be created with `MapRead or CopyDst`, be at least [byteSize] bytes and stay
 * unmapped when this is called. The copy is submitted, then the staging range is mapped, read and
 * unmapped in a `finally`. This helper never times anything: callers use it outside their
 * measurement window, before and after a campaign.
 */
internal suspend fun readUints(
    device: GPUDevice,
    source: GPUBuffer,
    staging: GPUBuffer,
    byteSize: ULong,
): UIntArray {
    val encoder = device.createCommandEncoder()
    try {
        encoder.copyBufferToBuffer(source, 0uL, staging, 0uL, byteSize)
        val commands = encoder.finish()
        try {
            device.queue.submit(listOf(commands))
        } finally {
            commands.close()
        }
    } finally {
        encoder.close()
    }

    staging.mapAsync(GPUMapMode.Read).getOrThrow()
    try {
        return staging.getMappedRange().toUIntArray()
    } finally {
        staging.unmap()
    }
}
