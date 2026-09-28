package org.graphiks.webgpu.suite.acid.renderpasses

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.MultisampleState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * A sample-count-4 attachment renders the red triangle and resolves into a sample-count-1 target:
 * the resolved interior pixels are red, and only the resolved texture is copied out.
 */
@AcidTest(
    id = AcidCaseId.MsaaResolve,
    family = AcidFamily.RenderPassesAttachments,
    contract = [
        ApiSymbols.GPUTextureDescriptor_sampleCount,
        ApiSymbols.GPUMultisampleState,
        ApiSymbols.GPURenderPassColorAttachment_resolveTarget,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun multisampleResolve(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        FULLSCREEN_TRIANGLE_WGSL,
        multisample = MultisampleState(count = 4u),
    ).use { pipeline ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(16u, 16u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.RenderAttachment,
                sampleCount = 4u,
            ),
        ).use { msaa ->
            createColorTarget(device, 16, 16).use { resolved ->
                msaa.createView().use { msaaView ->
                    resolved.createView().use { resolveView ->
                        device.createCommandEncoder().use { encoder ->
                            val pass = encoder.beginRenderPass(
                                RenderPassDescriptor(
                                    colorAttachments = listOf(
                                        RenderPassColorAttachment(
                                            view = msaaView,
                                            loadOp = GPULoadOp.Clear,
                                            storeOp = GPUStoreOp.Store,
                                            clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                            resolveTarget = resolveView,
                                        ),
                                    ),
                                ),
                            )
                            pass.setPipeline(pipeline)
                            pass.draw(3u)
                            pass.end()
                            encoder.finish().use { device.queue.submit(listOf(it)) }
                        }
                    }
                }

                val pixels = readRgba8(device, resolved, 16, 16)
                assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                assertPixel(pixels, 16, 2, 2, 255, 0, 0, 255)
            }
        }
    }
}
