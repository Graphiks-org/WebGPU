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
import kotlin.test.assertContentEquals

private const val STENCIL_LOAD_SHADER = """
@group(0) @binding(0) var image: texture_2d<u32>;
@group(0) @binding(1) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureLoad(image, vec2i(0, 0), 0).x;
    out[1] = textureLoad(image, vec2i(1, 0), 0).x;
    out[2] = textureLoad(image, vec2i(0, 1), 0).x;
    out[3] = textureLoad(image, vec2i(1, 1), 0).x;
}
"""

/**
 * A Depth24PlusStencil8 texture cleared to stencil 7 is read through a `StencilOnly` view bound as
 * `texture_2d<u32>`; every texel loads the stencil value 7. The sibling `texture.depth-aspect-load`
 * proves the depth half of the combined format; this proves the stencil half through the other
 * view aspect.
 */
@AcidTest(
    id = AcidCaseId.TexturesStencilViewAspect,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureFormat_Depth24PlusStencil8,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_TextureBinding,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor_aspect,
        ApiSymbols.GPUTextureAspect_StencilOnly,
        ApiSymbols.GPUTextureSampleType_Uint,
        ApiSymbols.GPUTextureViewDimension_TwoD,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPURenderPassDepthStencilAttachment,
    ],
)
suspend fun stencilViewAspect(device: GPUDevice) = withValidationScope(device) {
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

        texture.createView(TextureViewDescriptor(aspect = GPUTextureAspect.StencilOnly)).use { stencilView ->
            val bytes = computeTextureRead(
                device = device,
                shaderCode = STENCIL_LOAD_SHADER,
                textureView = stencilView,
                textureLayout = TextureBindingLayout(
                    sampleType = GPUTextureSampleType.Uint,
                    viewDimension = GPUTextureViewDimension.TwoD,
                ),
                outputBytes = 16uL,
            )

            assertContentEquals(
                uintArrayOf(7u, 7u, 7u, 7u),
                ArrayBuffer.of(bytes).toUIntArray(),
                "Stencil-only view texels must load the cleared stencil value 7",
            )
        }
    }
}
