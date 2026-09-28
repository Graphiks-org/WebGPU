package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val LOD_CLAMP_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;
@group(0) @binding(2) var samp: sampler;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureSampleLevel(tex, samp, vec2f(0.5, 0.5), 0.0);
    out[1] = textureSampleLevel(tex, samp, vec2f(0.5, 0.5), 2.0);
}
"""

/**
 * `lodMinClamp` and `lodMaxClamp` bound the requested LOD: pinned to level 1 over a red, green, blue
 * mip chain, both an LOD 0 and an LOD 2 request sample the green mip.
 */
@AcidTest(
    id = AcidCaseId.TexturesSamplingLodClamp,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_lodMinClamp,
        ApiSymbols.GPUSamplerDescriptor_lodMaxClamp,
        ApiSymbols.GPUTextureDescriptor_mipLevelCount,
    ],
)
suspend fun lodClampSampling(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
            mipLevelCount = 3u,
        ),
    ).use { texture ->
        writeSolid(device, texture, 0u, 4, 4, SOLID_RED)
        writeSolid(device, texture, 1u, 2, 2, SOLID_GREEN)
        writeSolid(device, texture, 2u, 1, 1, SOLID_BLUE)

        device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest,
                mipmapFilter = GPUMipmapFilterMode.Nearest,
                lodMinClamp = 1f,
                lodMaxClamp = 1f,
            ),
        ).use { sampler ->
            texture.createView().use { view ->
                val floats = ArrayBuffer.of(
                    computeTextureRead(
                        device = device,
                        shaderCode = LOD_CLAMP_SHADER,
                        textureView = view,
                        textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                        outputBytes = 32uL,
                        sampler = sampler,
                    ),
                ).toFloatArray()

                assertColor(floats, 0, 0f, 1f, 0f, 1f, "LOD 0 is clamped up to the green mip")
                assertColor(floats, 1, 0f, 1f, 0f, 1f, "LOD 2 is clamped down to the green mip")
            }
        }
    }
}
