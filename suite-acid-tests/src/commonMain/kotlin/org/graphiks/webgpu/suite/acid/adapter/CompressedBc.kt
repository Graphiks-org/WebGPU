package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertTrue

private const val BC1_SAMPLE_SHADER = """
@group(0) @binding(0) var image: texture_2d<f32>;
@group(0) @binding(1) var samp: sampler;

@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f {
    return textureSample(image, samp, vec2f(0.375));
}
"""

/**
 * The optional `TextureCompressionBC` feature is exercised end to end: the granted device reports
 * it, a 4x4 BC1 texture accepts its single block by copy — red endpoints over green, so the
 * opaque palette's first entry is pure red — and a quad that samples the texture in the fragment
 * stage renders pure red, so the decoded texel is observed, not just the creation. The decode is
 * asserted through fragment sampling; a compute-side load of a compressed texel is not asserted.
 *
 * Requires the optional `TextureCompressionBC` feature; the runner reports it as `unsupported`
 * otherwise.
 */
@AcidTest(
    id = AcidCaseId.FeaturesCompressedBc,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    requiredFeatures = [GPUFeatureName.TextureCompressionBC],
    contract = [
        ApiSymbols.GPUDevice_features,
        ApiSymbols.GPUFeatureName_TextureCompressionBC,
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureFormat_BC1RGBAUnorm,
        ApiSymbols.GPUTextureUsage_CopyDst,
        ApiSymbols.GPUTextureUsage_TextureBinding,
        ApiSymbols.GPUQueue_writeBuffer,
        ApiSymbols.GPUCommandEncoder_copyBufferToTexture,
        ApiSymbols.GPUTexelCopyBufferInfo,
        ApiSymbols.GPUTextureSampleType_Float,
        ApiSymbols.GPUTextureViewDimension_TwoD,
    ],
)
suspend fun compressedBc(device: GPUDevice) = withValidationScope(device) {
    assertTrue(
        GPUFeatureName.TextureCompressionBC in device.features,
        "A device granted for this case must report TextureCompressionBC",
    )

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.BC1RGBAUnorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        // One BC1 block: red over green endpoints, so the opaque palette's first entry is pure
        // red, and every 2-bit index selects it: all 16 texels decode to (255, 0, 0, 255).
        val block = ByteArray(256)
        block[0] = 0x00.toByte()
        block[1] = 0xF8.toByte()
        block[2] = 0xE0.toByte()
        block[3] = 0x07.toByte()

        device.createBuffer(
            BufferDescriptor(256uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
        ).use { upload ->
            device.queue.writeBuffer(upload, 0uL, ArrayBuffer.of(block))

            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToTexture(
                    TexelCopyBufferInfo(upload, bytesPerRow = 256u, rowsPerImage = 1u),
                    TexelCopyTextureInfo(texture = texture),
                    Extent3D(4u, 4u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        device.createSampler(
            SamplerDescriptor(magFilter = GPUFilterMode.Nearest, minFilter = GPUFilterMode.Nearest),
        ).use { sampler ->
            createRenderPipeline(device, BC1_SAMPLE_SHADER).use { pipeline ->
                pipeline.getBindGroupLayout(0u).use { layout ->
                    texture.createView().use { view ->
                        device.createBindGroup(
                            BindGroupDescriptor(
                                layout = layout,
                                entries = listOf(BindGroupEntry(0u, view), BindGroupEntry(1u, sampler)),
                            ),
                        ).use { group ->
                            createColorTarget(device, 2, 2).use { target ->
                                target.createView().use { targetView ->
                                    device.createCommandEncoder().use { encoder ->
                                        val pass = encoder.beginRenderPass(
                                            RenderPassDescriptor(
                                                colorAttachments = listOf(
                                                    RenderPassColorAttachment(
                                                        view = targetView,
                                                        loadOp = GPULoadOp.Clear,
                                                        storeOp = GPUStoreOp.Store,
                                                        clearValue = Color(0.0, 0.0, 1.0, 1.0),
                                                    ),
                                                ),
                                            ),
                                        )
                                        pass.setPipeline(pipeline)
                                        pass.setBindGroup(0u, group)
                                        pass.draw(3u)
                                        pass.end()
                                        encoder.finish().use { device.queue.submit(listOf(it)) }
                                    }
                                }

                                val pixels = readRgba8(device, target, 2, 2)
                                for (pixel in 0 until 4) {
                                    val x = pixel % 2
                                    val y = pixel / 2
                                    val offset = (y * 2 + x) * 4
                                    assertTrue(
                                        (pixels[offset].toInt() and 255) == 255 &&
                                            (pixels[offset + 1].toInt() and 255) == 0 &&
                                            (pixels[offset + 2].toInt() and 255) == 0 &&
                                            (pixels[offset + 3].toInt() and 255) == 255,
                                        "The quad sampling the BC1 texture must render pure red at ($x, $y), " +
                                            "observed r=${pixels[offset].toInt() and 255} g=${pixels[offset + 1].toInt() and 255} " +
                                            "b=${pixels[offset + 2].toInt() and 255} a=${pixels[offset + 3].toInt() and 255}",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
