package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val LINEAR_SAMPLE_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;
@group(0) @binding(2) var samp: sampler;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureSampleLevel(tex, samp, vec2f(0.5, 0.5), 0.0);
}
"""

/**
 * Linear filtering at the boundary between two texels averages them: sampling u = 0.5 between red
 * and blue gives (0.5, 0, 0.5, 1), within one byte of quantisation.
 */
@AcidTest(
    id = AcidCaseId.TexturesSamplingLinear,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_magFilter,
        ApiSymbols.GPUSamplerDescriptor_minFilter,
        ApiSymbols.GPUFilterMode_Linear,
        ApiSymbols.GPUTexture_createView,
    ],
)
suspend fun linearSampling(device: GPUDevice) = withValidationScope(device) {
    createRedBlueTexture(device).use { texture ->
        device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Linear,
                minFilter = GPUFilterMode.Linear,
                mipmapFilter = GPUMipmapFilterMode.Nearest,
            ),
        ).use { sampler ->
            texture.createView().use { view ->
                val floats = ArrayBuffer.of(
                    computeTextureRead(
                        device = device,
                        shaderCode = LINEAR_SAMPLE_SHADER,
                        textureView = view,
                        textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                        outputBytes = 16uL,
                        sampler = sampler,
                    ),
                ).toFloatArray()

                assertColor(floats, 0, 0.5f, 0f, 0.5f, 1f, "u = 0.5 blends red and blue", tolerance = 1f / 255f)
            }
        }
    }
}
