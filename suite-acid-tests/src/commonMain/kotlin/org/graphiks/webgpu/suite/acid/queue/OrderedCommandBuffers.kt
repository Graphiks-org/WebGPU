package org.graphiks.webgpu.suite.acid.queue

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
import kotlin.test.assertEquals

/**
 * Two command buffers submitted in one call run in list order: CB1 copies the source (7) into the
 * intermediate, CB2 copies the intermediate into the output, so the output reads 7 and not the
 * intermediate's initial 99. Reversing the list would read 99, so the order is load-bearing.
 */
@AcidTest(
    id = AcidCaseId.CommandOrderedCommandBuffers,
    family = AcidFamily.QueueCommands,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUQueue_writeBuffer,
        ApiSymbols.GPUDevice_createCommandEncoder,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUCommandEncoder_finish,
        ApiSymbols.GPUQueue_submit,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUBufferUsage_CopySrc,
    ],
)
suspend fun orderedCommandBuffers(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
    ).use { source ->
        device.createBuffer(
            BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
        ).use { intermediate ->
            device.createBuffer(
                BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
            ).use { output ->
                device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(byteArrayOf(7, 0, 0, 0)))
                device.queue.writeBuffer(intermediate, 0uL, ArrayBuffer.of(byteArrayOf(99, 0, 0, 0)))
                device.queue.writeBuffer(output, 0uL, ArrayBuffer.of(byteArrayOf(123, 0, 0, 0)))

                val first = device.createCommandEncoder().use { encoder ->
                    encoder.copyBufferToBuffer(source, 0uL, intermediate, 0uL, 4uL)
                    encoder.finish()
                }
                val second = device.createCommandEncoder().use { encoder ->
                    encoder.copyBufferToBuffer(intermediate, 0uL, output, 0uL, 4uL)
                    encoder.finish()
                }
                first.use {
                    second.use { device.queue.submit(listOf(first, second)) }
                }

                val bytes = readBufferBytes(device, output, 4uL)
                assertEquals(7, bytes[0].toInt() and 255, "The second command buffer must read the first's result")
                assertEquals(0, bytes[1].toInt() and 255)
                assertEquals(0, bytes[2].toInt() and 255)
                assertEquals(0, bytes[3].toInt() and 255)
            }
        }
    }
}
