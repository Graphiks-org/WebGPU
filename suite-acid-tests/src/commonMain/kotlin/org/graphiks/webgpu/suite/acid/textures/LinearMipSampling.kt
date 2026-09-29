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

private const val LINEAR_MIP_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;
@group(0) @binding(2) var samp: sampler;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureSampleLevel(tex, samp, vec2f(0.5, 0.5), 0.5);
}
"""

/**
 * `mipmapFilter: linear` blends adjacent mip levels: at LOD 0.5 between a red mip 0 and a blue
 * mip 1 the result is (0.5, 0, 0.5, 1), within one byte.
 */
@AcidTest(
    id = AcidCaseId.TexturesSamplingLinearMip,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_mipmapFilter,
        ApiSymbols.GPUMipmapFilterMode_Linear,
        ApiSymbols.GPUTextureDescriptor_mipLevelCount,
    ],
)
suspend fun linearMipSampling(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(4u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
            mipLevelCount = 2u,
        ),
    ).use { texture ->
        writeSolid(device, texture, 0u, 4, 4, SOLID_RED)
        writeSolid(device, texture, 1u, 2, 2, SOLID_BLUE)

        device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest,
                mipmapFilter = GPUMipmapFilterMode.Linear,
            ),
        ).use { sampler ->
            texture.createView().use { view ->
                val floats = ArrayBuffer.of(
                    computeTextureRead(
                        device = device,
                        shaderCode = LINEAR_MIP_SHADER,
                        textureView = view,
                        textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                        outputBytes = 16uL,
                        sampler = sampler,
                    ),
                ).toFloatArray()

                assertFilteredRedBlue(
                    floats,
                    0,
                    0.5f,
                    0.5f,
                    "LOD 0.5 blends the red mip 0 and the blue mip 1",
                    tolerance = 1f / 255f,
                    // Chromium 140 / SwiftShader returns alpha 0.9999847412109375 for this
                    // opaque pair. Keep the existing alpha rounding allowance local to A;
                    // green remains checked against zero with the tighter default tolerance.
                    alphaTolerance = 1e-3f,
                )
            }
        }
    }
}
