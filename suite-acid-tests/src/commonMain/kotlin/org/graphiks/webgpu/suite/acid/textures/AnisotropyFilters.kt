package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertIs

/**
 * A sampler with `maxAnisotropy` 16 and all three filters linear is created without a validation
 * error, and a default-filter sampler with `maxAnisotropy` 1 stays valid. A sampler that asks for
 * anisotropy fails validation with any single filter left `nearest` — each of the three is checked
 * alone, so a validation that forgot only one of them still fails the case.
 */
@AcidTest(
    id = AcidCaseId.SamplingAnisotropyFilters,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_maxAnisotropy,
        ApiSymbols.GPUSamplerDescriptor_magFilter,
        ApiSymbols.GPUSamplerDescriptor_minFilter,
        ApiSymbols.GPUSamplerDescriptor_mipmapFilter,
        ApiSymbols.GPUFilterMode_Linear,
        ApiSymbols.GPUFilterMode_Nearest,
        ApiSymbols.GPUMipmapFilterMode,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun anisotropyFilters(device: GPUDevice) = withValidationScope(device) {
    device.createSampler(
        SamplerDescriptor(
            maxAnisotropy = 16u,
            magFilter = GPUFilterMode.Linear,
            minFilter = GPUFilterMode.Linear,
            mipmapFilter = GPUMipmapFilterMode.Linear,
        ),
    ).close()

    device.createSampler(SamplerDescriptor(maxAnisotropy = 1u)).close()

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        // Anisotropy above 1 requires every filter to be linear; each filter is checked alone so
        // a validation that forgot only one of the three still fails the case.
        device.createSampler(
            SamplerDescriptor(
                maxAnisotropy = 16u,
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Linear,
                mipmapFilter = GPUMipmapFilterMode.Linear,
            ),
        ).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createSampler(
            SamplerDescriptor(
                maxAnisotropy = 16u,
                magFilter = GPUFilterMode.Linear,
                minFilter = GPUFilterMode.Nearest,
                mipmapFilter = GPUMipmapFilterMode.Linear,
            ),
        ).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createSampler(
            SamplerDescriptor(
                maxAnisotropy = 16u,
                magFilter = GPUFilterMode.Linear,
                minFilter = GPUFilterMode.Linear,
                mipmapFilter = GPUMipmapFilterMode.Nearest,
            ),
        ).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }
}
