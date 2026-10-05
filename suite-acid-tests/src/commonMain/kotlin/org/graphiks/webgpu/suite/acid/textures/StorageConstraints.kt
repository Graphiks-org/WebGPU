package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertIs

/**
 * The `StorageBinding` usage is a creation constraint, not a property every format has: an
 * RGBA8Unorm texture created with `StorageBinding` alongside `TextureBinding` is valid, while the
 * same usage pair fails validation on Depth24Plus and on Stencil8 because depth and stencil formats
 * are never storage textures. The negatives keep `TextureBinding` in the pair, so an
 * implementation that dropped the storage bit would leave a valid sampled texture and the expected
 * error would not arrive — the refusals prove the bit is honoured.
 */
@AcidTest(
    id = AcidCaseId.TexturesStorageConstraints,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureDescriptor_usage,
        ApiSymbols.GPUTextureUsage_StorageBinding,
        ApiSymbols.GPUTextureUsage_TextureBinding,
        ApiSymbols.GPUTextureFormat_RGBA8Unorm,
        ApiSymbols.GPUTextureFormat_Depth24Plus,
        ApiSymbols.GPUTextureFormat_Stencil8,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun storageConstraints(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.StorageBinding or GPUTextureUsage.TextureBinding,
        ),
    ).close()

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(2u, 2u, 1u),
                format = GPUTextureFormat.Depth24Plus,
                usage = GPUTextureUsage.StorageBinding or GPUTextureUsage.TextureBinding,
            ),
        ).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(2u, 2u, 1u),
                format = GPUTextureFormat.Stencil8,
                usage = GPUTextureUsage.StorageBinding or GPUTextureUsage.TextureBinding,
            ),
        ).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }
}
