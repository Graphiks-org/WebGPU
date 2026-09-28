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
 * `clearBuffer(offset = 8, size = 12)` zeroes only the three words it covers: of eight words filled
 * with `0x11223344`, words 2, 3 and 4 become zero and the others are unchanged.
 */
@AcidTest(
    id = AcidCaseId.TransfersClearBufferRange,
    family = AcidFamily.TransfersBufferTexture,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUQueue_writeBuffer,
        ApiSymbols.GPUCommandEncoder_clearBuffer,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUBuffer_getMappedRange,
    ],
)
suspend fun clearBufferRange(device: GPUDevice) = withValidationScope(device) {
    val pattern = UIntArray(8) { 0x11223344u }
    device.createBuffer(
        BufferDescriptor(32uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst),
    ).use { buffer ->
        device.queue.writeBuffer(buffer, 0uL, ArrayBuffer.of(pattern))
        device.createCommandEncoder().use { encoder ->
            encoder.clearBuffer(buffer, 8uL, 12uL)
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
        val words = ArrayBuffer.of(readBufferBytes(device, buffer, 32uL)).toUIntArray()
        assertContentEquals(
            uintArrayOf(0x11223344u, 0x11223344u, 0u, 0u, 0u, 0x11223344u, 0x11223344u, 0x11223344u),
            words,
            "Only the words covered by the clear range may change",
        )
    }
}
