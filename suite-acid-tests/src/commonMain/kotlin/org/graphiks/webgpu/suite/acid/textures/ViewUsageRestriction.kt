package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
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
import kotlin.test.assertIs

private const val LOAD_PIXEL_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(1)
fn main() {
    let c = textureLoad(tex, vec2i(0, 0), 0);
    out[0] = u32(round(c.r * 255.0));
    out[1] = u32(round(c.g * 255.0));
    out[2] = u32(round(c.b * 255.0));
    out[3] = u32(round(c.a * 255.0));
}
"""

/**
 * A view restricted to `TextureBinding` can be sampled: a `textureLoad` of a red clear reads red.
 * The same restricted view is then offered as a render attachment, which must fail validation. If
 * the view `usage` were ignored, the second expectation would not hold.
 */
@AcidTest(
    id = AcidCaseId.TexturesViewUsageRestriction,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor,
        ApiSymbols.GPUTextureViewDescriptor_usage,
        ApiSymbols.GPUTextureUsage,
        ApiSymbols.GPUTextureUsage_TextureBinding,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_CopySrc,
        ApiSymbols.GPUTextureSampleType_Float,
        ApiSymbols.GPUTextureViewDimension_TwoD,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
        ApiSymbols.GPUValidationError,
    ],
)
suspend fun viewUsageRestriction(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        ),
    ).use { texture ->
        texture.createView().use { renderView ->
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = listOf(
                            RenderPassColorAttachment(
                                view = renderView,
                                loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                clearValue = Color(1.0, 0.0, 0.0, 1.0),
                            ),
                        ),
                    ),
                )
                pass.end()
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        texture.createView(TextureViewDescriptor(usage = GPUTextureUsage.TextureBinding)).use { samplingView ->
            val words = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = LOAD_PIXEL_SHADER,
                    textureView = samplingView,
                    textureLayout = TextureBindingLayout(
                        sampleType = GPUTextureSampleType.Float,
                        viewDimension = GPUTextureViewDimension.TwoD,
                    ),
                    outputBytes = 16uL,
                ),
            ).toUIntArray()
            assertContentEquals(uintArrayOf(255u, 0u, 0u, 255u), words, "The restricted view must still sample the red clear")
        }

        texture.createView(TextureViewDescriptor(usage = GPUTextureUsage.TextureBinding)).use { restrictedView ->
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                device.createCommandEncoder().use { encoder ->
                    val pass = encoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachments = listOf(
                                RenderPassColorAttachment(
                                    view = restrictedView,
                                    loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                    storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                    clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                ),
                            ),
                        ),
                    )
                    pass.end()
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            } finally {
                assertIs<GPUValidationError>(
                    device.popErrorScope().getOrThrow(),
                    "A TextureBinding-only view must be rejected as a render attachment",
                )
            }
        }
    }
}
