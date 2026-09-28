package org.graphiks.webgpu.suite.acid.renderbundles

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.VertexAttribute
import org.graphiks.webgpu.descriptors.VertexBufferLayout
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

internal const val BUNDLE_SQUARE_SHADER = """
@vertex fn vertexMain(@location(0) position: vec2f) -> @builtin(position) vec4f {
    return vec4f(position, 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

internal val BUNDLE_SQUARE_VERTICES = floatArrayOf(
    -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
    -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
)

internal fun bundleVertexLayout() = listOf(
    VertexBufferLayout(8uL, listOf(VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 0u))),
)

/**
 * A render bundle records its own pipeline and vertex buffer and draws a red square; executing it
 * over a black clear leaves the square red and the background black.
 */
@AcidTest(
    id = AcidCaseId.BundlesDraw,
    family = AcidFamily.RenderBundles,
    contract = [
        ApiSymbols.GPUDevice_createRenderBundleEncoder,
        ApiSymbols.GPURenderBundleEncoder_finish,
        ApiSymbols.GPURenderPassEncoder_executeBundles,
        ApiSymbols.GPURenderBundleEncoderDescriptor,
    ],
)
suspend fun bundleDraw(device: GPUDevice) = withValidationScope(device) {
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

            createColorTarget(device, 16, 16).use { target ->
                target.createView().use { view ->
                    device.createCommandEncoder().use { enc ->
                        val pass = enc.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = view,
                                        loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                        storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                        clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                    ),
                                ),
                            ),
                        )
                        pass.executeBundles(listOf(bundle))
                        pass.end()
                        enc.finish().use { device.queue.submit(listOf(it)) }
                    }
                }

                val pixels = readRgba8(device, target, 16, 16)
                assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                assertPixel(pixels, 16, 1, 1, 0, 0, 0, 255)
            }
        }
    }
}
