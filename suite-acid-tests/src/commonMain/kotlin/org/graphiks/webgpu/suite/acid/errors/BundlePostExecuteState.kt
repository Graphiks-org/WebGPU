package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUValidationError
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
import org.graphiks.webgpu.suite.acid.renderbundles.BUNDLE_SQUARE_SHADER
import org.graphiks.webgpu.suite.acid.renderbundles.BUNDLE_SQUARE_VERTICES
import org.graphiks.webgpu.suite.acid.renderbundles.bundleVertexLayout
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertIs

private const val GREEN_FULLSCREEN_WGSL = """
@vertex fn vertexMain(@builtin(vertex_index) index: u32) -> @builtin(position) vec4f {
    var positions = array<vec2f, 3>(vec2f(-1.0, -1.0), vec2f(3.0, -1.0), vec2f(-1.0, 3.0));
    return vec4f(positions[index], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0, 1, 0, 1); }
"""

/**
 * Executing a render bundle resets the pass's pipeline and binding state: after `executeBundles`, a
 * `draw` without rebinding is a validation error even though a valid pipeline and vertex buffer were
 * bound before the bundle. The control proves the opposite path: rebinding a distinct pipeline after
 * the bundle draws, and only that rebound fullscreen draw can paint the target's corners.
 */
@AcidTest(
    id = AcidCaseId.ErrorsBundlePostExecuteState,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_createRenderBundleEncoder,
        ApiSymbols.GPURenderBundleEncoder_finish,
        ApiSymbols.GPURenderPassEncoder_executeBundles,
        ApiSymbols.GPURenderPassEncoder_setPipeline,
        ApiSymbols.GPURenderCommandsMixin_draw,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUValidationError,
    ],
)
suspend fun bundlePostExecuteState(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(48uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst),
    ).use { vertices ->
        device.queue.writeBuffer(vertices, 0uL, ArrayBuffer.of(BUNDLE_SQUARE_VERTICES))
        createRenderPipeline(device, BUNDLE_SQUARE_SHADER, vertexLayouts = bundleVertexLayout()).use { squarePipeline ->
            createRenderPipeline(device, GREEN_FULLSCREEN_WGSL).use { greenPipeline ->
                val bundle = device.createRenderBundleEncoder(
                    RenderBundleEncoderDescriptor(colorFormats = listOf(GPUTextureFormat.RGBA8Unorm)),
                ).use { encoder ->
                    encoder.setPipeline(squarePipeline)
                    encoder.setVertexBuffer(0u, vertices)
                    encoder.draw(6u)
                    encoder.finish()
                }

                // Probe: valid state is prebound, the bundle executes, then a draw without rebinding
                // must be a validation error. The encoder is finalized inside the scope (without
                // submitting) so the error is observed.
                createColorTarget(device, 16, 16).use { probeTarget ->
                    probeTarget.createView().use { probeView ->
                        device.pushErrorScope(GPUErrorFilter.Validation)
                        try {
                            device.createCommandEncoder().use { encoder ->
                                val pass = encoder.beginRenderPass(
                                    RenderPassDescriptor(
                                        colorAttachments = listOf(
                                            RenderPassColorAttachment(
                                                view = probeView,
                                                loadOp = GPULoadOp.Clear,
                                                storeOp = GPUStoreOp.Store,
                                                clearValue = Color(0.0, 0.0, 1.0, 1.0),
                                            ),
                                        ),
                                    ),
                                )
                                pass.setPipeline(squarePipeline)
                                pass.setVertexBuffer(0u, vertices)
                                pass.executeBundles(listOf(bundle))
                                pass.draw(3u)
                                pass.end()
                                encoder.finish().close()
                            }
                        } finally {
                            assertIs<GPUValidationError>(
                                device.popErrorScope().getOrThrow(),
                                "A draw after executeBundles without rebinding must be a validation error",
                            )
                        }
                    }
                }

                // Control: after the bundle, rebinding a distinct pipeline draws. The bundle paints
                // the red square (pixels 4..12); only the rebound green fullscreen draw can paint
                // the corners (1,1) and (14,14).
                createColorTarget(device, 16, 16).use { target ->
                    target.createView().use { view ->
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
                            pass.setPipeline(greenPipeline)
                            pass.draw(3u)
                            pass.end()
                            encoder.finish().use { device.queue.submit(listOf(it)) }
                        }
                    }
                    val pixels = readRgba8(device, target, 16, 16)
                    assertPixel(pixels, 16, 1, 1, 0, 255, 0, 255)
                    assertPixel(pixels, 16, 14, 14, 0, 255, 0, 255)
                }
            }
        }
    }
}
