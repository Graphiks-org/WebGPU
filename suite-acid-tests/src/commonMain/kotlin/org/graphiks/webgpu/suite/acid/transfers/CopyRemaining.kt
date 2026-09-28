package org.graphiks.webgpu.suite.acid.transfers

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

/**
 * `copyBufferToBuffer` without a size copies from the source offset to the end of the source: from
 * `[10, 20, 30, 40]` at source offset 8 it copies `[30, 40]` into the destination's first two words,
 * leaving the rest of the destination zero.
 */
@AcidTest(
    id = AcidCaseId.TransfersCopyRemaining,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUQueue_writeBuffer,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUBuffer_getMappedRange,
    ],
)
suspend fun copyRemaining(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst),
    ).use { source ->
        device.createBuffer(
            BufferDescriptor(16uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst),
        ).use { destination ->
            device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(uintArrayOf(10u, 20u, 30u, 40u)))
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(source, 8uL, destination, 0uL)
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
            val words = ArrayBuffer.of(readBufferBytes(device, destination, 16uL)).toUIntArray()
            assertContentEquals(
                uintArrayOf(30u, 40u, 0u, 0u),
                words,
                "An omitted size copies the remaining source bytes only",
            )
        }
    }
}
