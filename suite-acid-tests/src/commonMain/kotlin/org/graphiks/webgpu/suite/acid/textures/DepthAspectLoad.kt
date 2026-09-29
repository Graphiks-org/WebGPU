package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureAspect
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.math.abs
import kotlin.test.assertTrue

private const val DEPTH_LOAD_SHADER = """
@group(0) @binding(0) var image: texture_depth_2d;
@group(0) @binding(1) var<storage, read_write> out: array<f32>;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureLoad(image, vec2i(0, 0), 0);
    out[1] = textureLoad(image, vec2i(1, 0), 0);
    out[2] = textureLoad(image, vec2i(0, 1), 0);
    out[3] = textureLoad(image, vec2i(1, 1), 0);
}
"""

/**
 * A Depth24PlusStencil8 texture cleared to depth 0.25 and stencil 7 is read through a `DepthOnly`
 * view bound as `texture_depth_2d`; every texel loads 0.25. The case reads the depth value, not the
 * opaque bytes of the combined format.
 */
@AcidTest(
    id = AcidCaseId.TexturesDepthAspectLoad,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureFormat_Depth24PlusStencil8,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_TextureBinding,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor_aspect,
        ApiSymbols.GPUTextureAspect_DepthOnly,
        ApiSymbols.GPUTextureSampleType_Depth,
        ApiSymbols.GPUTextureViewDimension_TwoD,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderPassDepthStencilAttachment,
    ],
)
suspend fun depthAspectLoad(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.Depth24PlusStencil8,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        texture.createView().use { clearView ->
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = emptyList(),
                        depthStencilAttachment = RenderPassDepthStencilAttachment(
                            view = clearView,
                            depthClearValue = 0.25f,
                            depthLoadOp = GPULoadOp.Clear,
                            depthStoreOp = GPUStoreOp.Store,
                            stencilClearValue = 7u,
                            stencilLoadOp = GPULoadOp.Clear,
                            stencilStoreOp = GPUStoreOp.Store,
                        ),
                    ),
                )
                pass.end()
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        texture.createView(TextureViewDescriptor(aspect = GPUTextureAspect.DepthOnly)).use { depthView ->
            val floats = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = DEPTH_LOAD_SHADER,
                    textureView = depthView,
                    textureLayout = TextureBindingLayout(
                        sampleType = GPUTextureSampleType.Depth,
                        viewDimension = GPUTextureViewDimension.TwoD,
                    ),
                    outputBytes = 16uL,
                ),
            ).toFloatArray()

            floats.forEachIndexed { index, value ->
                assertTrue(
                    abs(value - 0.25f) <= 1e-6f,
                    "Depth texel $index: expected 0.25 (±1e-6) but observed $value",
                )
            }
        }
    }
}
