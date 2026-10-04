package org.graphiks.webgpu.suite.acid.renderbundles

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs

/**
 * A render bundle recorded against RGBA8Unorm is not executable everywhere: executing it in a pass
 * whose colour attachment is RGBA16Float fails validation, and executing it in a pass with no
 * colour attachment fails too. Both errors are captured by an error scope around the submission.
 */
@AcidTest(
    id = AcidCaseId.BundlesNegativeValidation,
    family = AcidFamily.RenderBundles,
    contract = [
        ApiSymbols.GPUDevice_createRenderBundleEncoder,
        ApiSymbols.GPURenderBundleEncoderDescriptor,
        ApiSymbols.GPURenderBundleEncoder_finish,
        ApiSymbols.GPURenderPassEncoder_executeBundles,
        ApiSymbols.GPUTextureFormat_RGBA16Float,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun bundlesNegativeValidation(device: GPUDevice) {
    val bundle = device.createRenderBundleEncoder(
        RenderBundleEncoderDescriptor(colorFormats = listOf(GPUTextureFormat.RGBA8Unorm)),
    ).use { encoder ->
        encoder.finish()
    }

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA16Float,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        ),
    ).use { target ->
        target.createView().use { view ->
            // A pass whose single attachment does not match the bundle's declared format.
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                device.createCommandEncoder().use { encoder ->
                    val pass = encoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachments = listOf(
                                RenderPassColorAttachment(
                                    view = view,
                                    loadOp = GPULoadOp.Clear,
                                    storeOp = GPUStoreOp.Store,
                                    clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                ),
                            ),
                        ),
                    )
                    pass.executeBundles(listOf(bundle))
                    pass.end()
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            } finally {
                val mismatch = device.popErrorScope().getOrThrow()
                assertIs<GPUValidationError>(mismatch)
            }

            // A pass with no colour attachment while the bundle declares one.
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                device.createCommandEncoder().use { encoder ->
                    val pass = encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = emptyList()))
                    pass.executeBundles(listOf(bundle))
                    pass.end()
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            } finally {
                val missing = device.popErrorScope().getOrThrow()
                assertIs<GPUValidationError>(missing)
            }
        }
    }
}
