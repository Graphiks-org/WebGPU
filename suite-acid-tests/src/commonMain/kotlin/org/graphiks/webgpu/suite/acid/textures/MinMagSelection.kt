package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUAddressMode
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
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

private const val MIN_MAG_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var samp: sampler;

@vertex fn vertexMain(@builtin(vertex_index) index: u32) -> @builtin(position) vec4f {
    var positions = array<vec2f, 3>(vec2f(-1.0, -1.0), vec2f(3.0, -1.0), vec2f(-1.0, 3.0));
    return vec4f(positions[index], 0.5, 1.0);
}

@fragment fn fragmentLod0() -> @location(0) vec4f { return textureSampleLevel(tex, samp, vec2f(0.4375, 0.125), 0.0); }
@fragment fn fragmentLod1() -> @location(0) vec4f { return textureSampleLevel(tex, samp, vec2f(0.4375, 0.125), 1.0); }
"""

/** Mip 0: columns x∈{0,1} red and x∈{2,3} blue, all rows. Mip 1: x=0 red, x=1 blue. */
private fun minMagTextureData(mip: Int): ByteArray = when (mip) {
    0 -> {
        val row = SOLID_RED + SOLID_RED + SOLID_BLUE + SOLID_BLUE
        row + row + row + row
    }
    else -> (SOLID_RED + SOLID_BLUE) + (SOLID_RED + SOLID_BLUE)
}

/**
 * `magFilter` and `minFilter` are honoured independently: with opposite filters on one sampler,
 * sampling at level 0 (magnification) follows `magFilter` and level 1 (minification) follows
 * `minFilter`. At u = 7/16 the filter weights are exactly representable, so the linear results are
 * computed, not approximated: mip 0 blends 0.75 red with 0.25 blue and mip 1 blends 0.625 red with
 * 0.375 blue. Swapping the filters swaps which sample is solid red and which is blended, so a
 * sampler that ignores either filter fails.
 */
@AcidTest(
    id = AcidCaseId.SamplingMinMagSelection,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_magFilter,
        ApiSymbols.GPUSamplerDescriptor_minFilter,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPURenderCommandsMixin_draw,
    ],
)
suspend fun minMagSelection(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
            mipLevelCount = 2u,
        ),
    ).use { texture ->
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = texture),
            ArrayBuffer.of(minMagTextureData(0)),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 16u, rowsPerImage = 4u),
            Extent3D(4u, 4u, 1u),
        )
        device.queue.writeTexture(
            TexelCopyTextureInfo(texture = texture, mipLevel = 1u),
            ArrayBuffer.of(minMagTextureData(1)),
            TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 8u, rowsPerImage = 2u),
            Extent3D(2u, 2u, 1u),
        )

        // (label, sampler, (red, blue) expected at LOD 0 and LOD 1)
        val configs = listOf(
            Triple(
                "magNearest-minLinear",
                SamplerDescriptor(
                    magFilter = GPUFilterMode.Nearest,
                    minFilter = GPUFilterMode.Linear,
                    mipmapFilter = GPUMipmapFilterMode.Nearest,
                    addressModeU = GPUAddressMode.ClampToEdge,
                    addressModeV = GPUAddressMode.ClampToEdge,
                    maxAnisotropy = 1u,
                ),
                listOf(255 to 0, 159 to 96),
            ),
            Triple(
                "magLinear-minNearest",
                SamplerDescriptor(
                    magFilter = GPUFilterMode.Linear,
                    minFilter = GPUFilterMode.Nearest,
                    mipmapFilter = GPUMipmapFilterMode.Nearest,
                    addressModeU = GPUAddressMode.ClampToEdge,
                    addressModeV = GPUAddressMode.ClampToEdge,
                    maxAnisotropy = 1u,
                ),
                listOf(191 to 64, 255 to 0),
            ),
        )

        createRenderPipeline(device, MIN_MAG_SHADER, fragmentEntryPoint = "fragmentLod0").use { lod0Pipeline ->
            createRenderPipeline(device, MIN_MAG_SHADER, fragmentEntryPoint = "fragmentLod1").use { lod1Pipeline ->
                val pipelines = listOf(0 to lod0Pipeline, 1 to lod1Pipeline)
                texture.createView().use { view ->
                    for ((samplerLabel, descriptor, expected) in configs) {
                        device.createSampler(descriptor).use { sampler ->
                            for ((lod, pipeline) in pipelines) {
                                pipeline.getBindGroupLayout(0u).use { layout ->
                                    device.createBindGroup(
                                        BindGroupDescriptor(
                                            layout = layout,
                                            entries = listOf(BindGroupEntry(0u, view), BindGroupEntry(1u, sampler)),
                                        ),
                                    ).use { group ->
                                        val (red, blue) = expected[lod]
                                        val pixels = renderMinMagSample(device, pipeline, group)
                                        assertPixel(pixels, 16, 8, 8, red, 0, blue, 255, tolerance = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Renders one fullscreen triangle of [pipeline] with [group] bound and reads the target back. */
private suspend fun renderMinMagSample(
    device: GPUDevice,
    pipeline: org.graphiks.webgpu.GPURenderPipeline,
    group: org.graphiks.webgpu.GPUBindGroup,
): ByteArray =
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
                pass.setPipeline(pipeline)
                pass.setBindGroup(0u, group)
                pass.draw(3u)
                pass.end()
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }
        readRgba8(device, target, 16, 16)
    }
