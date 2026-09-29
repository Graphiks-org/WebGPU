package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs

/**
 * A copy out of a texture is encoded and the command buffer is finished; the texture is then closed
 * before the submission. Submitting the command buffer in a Validation scope reports a captured
 * error. The case does not require a synchronous exception from `close` nor a defined readback after
 * the invalid work.
 */
@AcidTest(
    id = AcidCaseId.ErrorsDestroyedTextureSubmission,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUTexture_close,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUCommandEncoder_finish,
        ApiSymbols.GPUQueue_submit,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUValidationError,
    ],
)
suspend fun destroyedTextureSubmission(device: GPUDevice) {
    val texture = device.createTexture(
        TextureDescriptor(
            size = Extent3D(1u, 1u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopySrc or GPUTextureUsage.CopyDst,
        ),
    )
    var textureClosed = false
    try {
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture),
            ArrayBuffer.of(byteArrayOf(255.toByte(), 0, 0, 255.toByte())),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 4u, rowsPerImage = 1u),
            Extent3D(1u, 1u, 1u),
        )

        device.createBuffer(
            BufferDescriptor(256uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
        ).use { staging ->
            device.createCommandEncoder().use { encoder ->
                encoder.copyTextureToBuffer(
                    TexelCopyTextureInfo(texture = texture),
                    TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 1u),
                    Extent3D(1u, 1u, 1u),
                )
                encoder.finish().use { commandBuffer ->
                    texture.close()
                    textureClosed = true

                    device.pushErrorScope(GPUErrorFilter.Validation)
                    try {
                        device.queue.submit(listOf(commandBuffer))
                    } finally {
                        assertIs<GPUValidationError>(
                            device.popErrorScope().getOrThrow(),
                            "Submitting work that reads a destroyed texture must report a validation error",
                        )
                    }
                }
            }
        }
    } finally {
        if (!textureClosed) texture.close()
    }
}
