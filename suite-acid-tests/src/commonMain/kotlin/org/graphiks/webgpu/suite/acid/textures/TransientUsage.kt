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
 * The `TransientAttachment` usage is only valid together with `RenderAttachment`: an RGBA8Unorm
 * texture created with both usages is valid, while the transient usage alone fails validation
 * because a transient attachment always implies the render-attachment usage.
 */
@AcidTest(
    id = AcidCaseId.TexturesTransientUsage,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureDescriptor_usage,
        ApiSymbols.GPUTextureUsage_TransientAttachment,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureFormat_RGBA8Unorm,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun transientUsage(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.TransientAttachment or GPUTextureUsage.RenderAttachment,
        ),
    ).close()

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(4u, 4u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.TransientAttachment,
            ),
        ).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }
}
