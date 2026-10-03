package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCommandBuffer
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
 * A finished encoder cannot be finished a second time: the second `finish` inside a Validation scope
 * reports a [GPUValidationError], while the command buffer returned by the first finish is still
 * usable and copies 7. The object the second finish returns, if any, is closed.
 */
@AcidTest(
    id = AcidCaseId.ErrorsEncoderFinishedTwice,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUCommandEncoder_finish,
        ApiSymbols.GPUCommandBuffer,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUValidationError,
        ApiSymbols.GPUQueue_submit,
    ],
)
suspend fun encoderFinishedTwice(device: GPUDevice) {
    device.createBuffer(
        BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
    ).use { source ->
        device.createBuffer(
            BufferDescriptor(4uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
        ).use { destination ->
            device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(byteArrayOf(7, 0, 0, 0)))

            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(source, 0uL, destination, 0uL, 4uL)
                val first = encoder.finish()

                // `use` owns `first` for the whole second attempt: a synchronous throw from the
                // second finish or the scope assertion closes it exactly once.
                first.use {
                    device.pushErrorScope(GPUErrorFilter.Validation)
                    var second: GPUCommandBuffer? = null
                    try {
                        // An unexpected synchronous exception must propagate: the scope assertion
                        // below does not license swallowing it. Cleanup still runs.
                        second = encoder.finish()
                    } finally {
                        second?.close()
                        assertIs<GPUValidationError>(
                            device.popErrorScope().getOrThrow(),
                            "Finishing a finished encoder must report a validation error",
                        )
                    }

                    // The first command buffer is still the valid one.
                    device.queue.submit(listOf(first))
                }
            }

            val bytes = readBufferBytes(device, destination, 4uL)
            assertEquals(7, bytes[0].toInt() and 255, "The first finish must still produce a working command buffer")
            assertEquals(0, bytes[1].toInt() and 255)
            assertEquals(0, bytes[2].toInt() and 255)
            assertEquals(0, bytes[3].toInt() and 255)
        }
    }
}
