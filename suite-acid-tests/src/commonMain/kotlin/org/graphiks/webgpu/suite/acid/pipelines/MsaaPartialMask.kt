package org.graphiks.webgpu.suite.acid.pipelines

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
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val WHITE_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,1,1,1); }
"""

/**
 * A 4x MSAA white fullscreen draw with the full sample mask resolves to pure white, while the same
 * draw with the partial mask `0b0101` covers only samples 0 and 2 of every pixel: the resolved
 * colour is about half the draw over the black clear — strictly between the two, never 0 and
 * never 255. The sibling `msaa.sample-mask-zero` proves the empty mask; this proves a genuinely
 * partial one.
 */
@AcidTest(
    id = AcidCaseId.RenderMsaaPartialMask,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUMultisampleState,
        ApiSymbols.GPUMultisampleState_mask,
        ApiSymbols.GPUSampleMask,
        ApiSymbols.GPURenderPassColorAttachment_resolveTarget,
    ],
)
suspend fun msaaPartialMask(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        WHITE_SHADER,
        multisample = MultisampleState(count = 4u, mask = 0xFFFFFFFFu),
    ).use { fullPipeline ->
        createRenderPipeline(
            device,
            WHITE_SHADER,
            multisample = MultisampleState(count = 4u, mask = 0b0101u),
        ).use { partialPipeline ->
            fun msaaTarget() = device.createTexture(
                TextureDescriptor(
                    size = Extent3D(16u, 16u, 1u),
                    format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.RenderAttachment,
                    sampleCount = 4u,
                ),
            )

            msaaTarget().use { fullMsaa ->
                createColorTarget(device, 16, 16).use { fullResolved ->
                    fullMsaa.createView().use { msaaView ->
                        fullResolved.createView().use { resolveView ->
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
                                pass.setPipeline(fullPipeline)
                                pass.draw(3u)
                                pass.end()
                                encoder.finish().use { device.queue.submit(listOf(it)) }
                            }
                        }
                    }

                    val pixels = readRgba8(device, fullResolved, 16, 16)
                    assertPixel(pixels, 16, 8, 8, 255, 255, 255, 255)
                }
            }

            msaaTarget().use { partialMsaa ->
                createColorTarget(device, 16, 16).use { partialResolved ->
                    partialMsaa.createView().use { msaaView ->
                        partialResolved.createView().use { resolveView ->
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
                                pass.setPipeline(partialPipeline)
                                pass.draw(3u)
                                pass.end()
                                encoder.finish().use { device.queue.submit(listOf(it)) }
                            }
                        }
                    }

                    val pixels = readRgba8(device, partialResolved, 16, 16)
                    // Two of the four samples per pixel are white; the resolve is about half
                    // brightness, and the tolerance only absorbs the resolve's rounding.
                    assertPixel(pixels, 16, 8, 8, 128, 128, 128, 255, tolerance = 28)
                    assertPixel(pixels, 16, 2, 13, 128, 128, 128, 255, tolerance = 28)
                }
            }
        }
    }
}
