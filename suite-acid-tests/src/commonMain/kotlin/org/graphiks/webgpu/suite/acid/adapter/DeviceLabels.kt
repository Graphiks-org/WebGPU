package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.CommandBufferDescriptor
import org.graphiks.webgpu.descriptors.CommandEncoderDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals

/**
 * A buffer, a command encoder and a finished command buffer each expose the label from their
 * descriptor and accept a rewrite, including non-ASCII labels. The queue's label is saved, changed
 * and restored. A real copy through the relabelled objects proves they stay usable, so a label that
 * were recorded at the expense of the object would be caught.
 *
 * The device's and default queue's initial labels are checked by the context cases' descriptors,
 * not by mutating the borrowed device.
 */
@AcidTest(
    id = AcidCaseId.AdapterDeviceLabels,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    contract = [
        ApiSymbols.GPUObjectBase,
        ApiSymbols.GPUObjectBase_label,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createCommandEncoder,
        ApiSymbols.GPUCommandEncoder_finish,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUQueue_submit,
        ApiSymbols.GPUBufferUsage_CopySrc,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUQueue_writeBuffer,
    ],
)
suspend fun deviceLabels(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc, label = "acid-étiquette-λ"),
    ).use { source ->
        assertEquals("acid-étiquette-λ", source.label, "The buffer keeps its descriptor label")
        source.label = "renamed-λ"
        assertEquals("renamed-λ", source.label, "The buffer label accepts a rewrite")

        device.createBuffer(
            BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc, label = "acid-destination-λ"),
        ).use { destination ->
            device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(byteArrayOf(0xAB.toByte(), 0, 0, 0)))

            device.createCommandEncoder(CommandEncoderDescriptor(label = "acid-encoder-λ")).use { encoder ->
                assertEquals("acid-encoder-λ", encoder.label, "The encoder keeps its descriptor label")
                encoder.label = "renamed-encoder-λ"
                assertEquals("renamed-encoder-λ", encoder.label, "The encoder label accepts a rewrite")

                encoder.copyBufferToBuffer(source, 0uL, destination, 0uL, 4uL)
                encoder.finish(CommandBufferDescriptor(label = "acid-command-λ")).use { commandBuffer ->
                    assertEquals("acid-command-λ", commandBuffer.label, "The command buffer keeps its descriptor label")
                    commandBuffer.label = "renamed-command-λ"
                    assertEquals("renamed-command-λ", commandBuffer.label, "The command buffer label accepts a rewrite")
                    device.queue.submit(listOf(commandBuffer))
                }
            }

            val savedQueueLabel = device.queue.label
            try {
                device.queue.label = "acid-queue-λ"
                assertEquals("acid-queue-λ", device.queue.label, "The queue label accepts a rewrite")
            } finally {
                device.queue.label = savedQueueLabel
                assertEquals(savedQueueLabel, device.queue.label, "The queue label is restored")
            }

            val bytes = readBufferBytes(device, destination, 4uL)
            assertEquals(0xAB, bytes[0].toInt() and 255, "The relabelled objects still copy the byte through")
            assertEquals(0, bytes[1].toInt() and 255)
            assertEquals(0, bytes[2].toInt() and 255)
            assertEquals(0, bytes[3].toInt() and 255)
        }
    }
}
