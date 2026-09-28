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
 * A texture created with only `TextureBinding` cannot be a copy source: `copyTextureToBuffer` fails
 * validation even though every layout parameter is valid.
 */
@AcidTest(
    id = AcidCaseId.ErrorsMissingCopyUsage,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTextureUsage,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun missingCopyUsage(device: GPUDevice) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.TextureBinding,
        ),
    ).use { source ->
        device.createBuffer(
            BufferDescriptor(1024uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
        ).use { destination ->
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                device.createCommandEncoder().use { encoder ->
                    encoder.copyTextureToBuffer(
                        TexelCopyTextureInfo(texture = source),
                        TexelCopyBufferInfo(buffer = destination, bytesPerRow = 256u, rowsPerImage = 4u),
                        Extent3D(4u, 4u, 1u),
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
