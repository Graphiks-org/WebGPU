package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.Origin3D
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
 * A copy whose origin plus size leaves the texture fails validation: a 2×2 copy at origin (3, 3)
 * runs past the 4×4 target even though the buffer layout itself is valid.
 */
@AcidTest(
    id = AcidCaseId.ErrorsTextureCopyBounds,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUCommandEncoder_copyBufferToTexture,
        ApiSymbols.GPUTexelCopyTextureInfo_origin,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun textureCopyBounds(device: GPUDevice) {
    device.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.CopySrc)).use { source ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(4u, 4u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.CopyDst,
            ),
        ).use { target ->
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                device.createCommandEncoder().use { encoder ->
                    encoder.copyBufferToTexture(
                        TexelCopyBufferInfo(buffer = source, bytesPerRow = 256u, rowsPerImage = 2u),
                        TexelCopyTextureInfo(texture = target, origin = Origin3D(3u, 3u, 0u)),
                        Extent3D(2u, 2u, 1u),
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
