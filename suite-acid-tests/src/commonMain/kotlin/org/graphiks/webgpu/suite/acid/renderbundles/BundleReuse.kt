package org.graphiks.webgpu.suite.acid.renderbundles

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * The same recorded bundle runs unchanged in two passes over two differently cleared targets: each
 * keeps its own background (blue and green) with the red square in the middle.
 */
@AcidTest(
    id = AcidCaseId.BundlesReuse,
    family = AcidFamily.RenderBundles,
    contract = [
        ApiSymbols.GPUDevice_createRenderBundleEncoder,
        ApiSymbols.GPURenderPassEncoder_executeBundles,
        ApiSymbols.GPURenderBundleEncoder_finish,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun bundleReuse(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst)).use { vertices ->
        device.queue.writeBuffer(vertices, 0uL, ArrayBuffer.of(BUNDLE_SQUARE_VERTICES))
        createRenderPipeline(device, BUNDLE_SQUARE_SHADER, vertexLayouts = bundleVertexLayout()).use { pipeline ->
            val bundle = device.createRenderBundleEncoder(
                RenderBundleEncoderDescriptor(colorFormats = listOf(GPUTextureFormat.RGBA8Unorm)),
            ).use { encoder ->
                encoder.setPipeline(pipeline)
                encoder.setVertexBuffer(0u, vertices)
                encoder.draw(6u)
                encoder.finish()
            }

            createColorTarget(device, 16, 16).use { blueTarget ->
                createColorTarget(device, 16, 16).use { greenTarget ->
                    blueTarget.createView().use { blueView ->
                        greenTarget.createView().use { greenView ->
                            device.createCommandEncoder().use { enc ->
                                val bluePass = enc.beginRenderPass(
                                    RenderPassDescriptor(
                                        colorAttachments = listOf(
                                            RenderPassColorAttachment(
                                                view = blueView,
                                                loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                                storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                                clearValue = Color(0.0, 0.0, 1.0, 1.0),
                                            ),
                                        ),
                                    ),
                                )
                                bluePass.executeBundles(listOf(bundle))
                                bluePass.end()

                                val greenPass = enc.beginRenderPass(
                                    RenderPassDescriptor(
                                        colorAttachments = listOf(
                                            RenderPassColorAttachment(
                                                view = greenView,
                                                loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                                storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                                clearValue = Color(0.0, 1.0, 0.0, 1.0),
                                            ),
                                        ),
                                    ),
                                )
                                greenPass.executeBundles(listOf(bundle))
                                greenPass.end()
                                enc.finish().use { device.queue.submit(listOf(it)) }
                            }
                        }
                    }

                    val bluePixels = readRgba8(device, blueTarget, 16, 16)
                    val greenPixels = readRgba8(device, greenTarget, 16, 16)
                    // Both triangles of the square must be painted in each pass.
                    assertPixel(bluePixels, 16, 6, 6, 255, 0, 0, 255)
                    assertPixel(bluePixels, 16, 10, 10, 255, 0, 0, 255)
                    assertPixel(bluePixels, 16, 1, 1, 0, 0, 255, 255)
                    assertPixel(greenPixels, 16, 6, 6, 255, 0, 0, 255)
                    assertPixel(greenPixels, 16, 10, 10, 255, 0, 0, 255)
                    assertPixel(greenPixels, 16, 1, 1, 0, 255, 0, 255)
                }
            }
        }
    }
}
