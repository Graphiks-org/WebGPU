package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * `copyBufferToTexture` requires `bytesPerRow` to be a multiple of 256; a 12-byte stride fails
 * validation. (The same stride is valid for `writeTexture`, covered in Lot B.)
 */
@AcidTest(
    id = AcidCaseId.ErrorsTextureCopyRowAlignment,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUCommandEncoder_copyBufferToTexture,
        ApiSymbols.GPUTexelCopyBufferLayout_bytesPerRow,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun textureCopyRowAlignment(device: GPUDevice) {
    device.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.CopySrc)).use { source ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(3u, 2u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.CopyDst,
            ),
        ).use { target ->
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                device.createCommandEncoder().use { encoder ->
                    encoder.copyBufferToTexture(
                        TexelCopyBufferInfo(buffer = source, bytesPerRow = 12u, rowsPerImage = 2u),
                        TexelCopyTextureInfo(texture = target),
                        Extent3D(3u, 2u, 1u),
                    )
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            } finally {
                val result = device.popErrorScope()
                assertTrue(result.isSuccess)
                assertIs<GPUValidationError>(result.getOrThrow())
            }
        }
    }
}
