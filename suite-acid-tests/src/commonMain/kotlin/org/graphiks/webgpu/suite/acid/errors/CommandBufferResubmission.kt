package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * A command buffer is submitted once and copies 7; the same command buffer is then submitted again
 * without being re-recorded and must report a validation error. The command buffer is never rebuilt
 * between the two submissions, so the rejection is about reuse, not about a new recording.
 */
@AcidTest(
    id = AcidCaseId.ErrorsCommandBufferResubmission,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUQueue_submit,
        ApiSymbols.GPUCommandEncoder_finish,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUValidationError,
    ],
)
suspend fun commandBufferResubmission(device: GPUDevice) {
    device.createBuffer(
        BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
    ).use { source ->
        device.createBuffer(
            BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
        ).use { destination ->
            device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(byteArrayOf(7, 0, 0, 0)))

            val commandBuffer = device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(source, 0uL, destination, 0uL, 4uL)
                encoder.finish()
            }
            commandBuffer.use {
                device.queue.submit(listOf(it))
                device.queue.onSubmittedWorkDone().getOrThrow()

                val bytes = readBufferBytes(device, destination, 4uL)
                assertEquals(7, bytes[0].toInt() and 255, "The first submission must copy 7")

                device.pushErrorScope(GPUErrorFilter.Validation)
                try {
                    device.queue.submit(listOf(it))
                } finally {
                    assertIs<GPUValidationError>(
                        device.popErrorScope().getOrThrow(),
                        "Submitting the same command buffer twice must report a validation error",
                    )
                }
            }
        }
    }
}
