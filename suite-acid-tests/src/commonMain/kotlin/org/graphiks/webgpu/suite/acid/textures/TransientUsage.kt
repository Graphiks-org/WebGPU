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
 * The `TransientAttachment` usage accepts exactly one companion: an RGBA8Unorm texture created with
 * `TransientAttachment` and `RenderAttachment` is valid, and so is the `RenderAttachment` plus
 * `CopySrc` control, but adding `CopySrc` to the transient pair fails validation and the transient
 * usage alone fails too. The `CopySrc` negative and its valid control differ by exactly the
 * transient bit, so an implementation that dropped the bit would leave a valid texture and the
 * expected error would not arrive.
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

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        ),
    ).close()

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        // The transient bit forbids any companion beyond RenderAttachment; without the bit the
        // usages would be the valid control pair above, so the refusal proves the bit is honoured.
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(4u, 4u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.TransientAttachment or GPUTextureUsage.RenderAttachment or
                    GPUTextureUsage.CopySrc,
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
