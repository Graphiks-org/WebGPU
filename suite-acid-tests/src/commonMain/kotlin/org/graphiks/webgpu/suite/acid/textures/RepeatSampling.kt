package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUAddressMode
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

private const val REPEAT_SAMPLE_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;
@group(0) @binding(2) var samp: sampler;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureSampleLevel(tex, samp, vec2f(1.25, 0.5), 0.0);
    out[1] = textureSampleLevel(tex, samp, vec2f(-0.25, 0.5), 0.0);
}
"""

/**
 * `Repeat` wraps coordinates by their fractional part: u = 1.25 is the red texel at 0.25 and
 * u = -0.25 the blue texel at 0.75.
 */
@AcidTest(
    id = AcidCaseId.TexturesSamplingRepeat,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_addressModeU,
        ApiSymbols.GPUAddressMode_Repeat,
        ApiSymbols.GPUTexture_createView,
    ],
)
suspend fun repeatSampling(device: GPUDevice) = withValidationScope(device) {
    createRedBlueTexture(device).use { texture ->
        device.createSampler(
            SamplerDescriptor(
                addressModeU = GPUAddressMode.Repeat,
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest,
                mipmapFilter = GPUMipmapFilterMode.Nearest,
            ),
        ).use { sampler ->
            texture.createView().use { view ->
                val floats = ArrayBuffer.of(
                    computeTextureRead(
                        device = device,
                        shaderCode = REPEAT_SAMPLE_SHADER,
                        textureView = view,
                        textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                        outputBytes = 32uL,
                        sampler = sampler,
                    ),
                ).toFloatArray()

                assertColor(floats, 0, 1f, 0f, 0f, 1f, "u = 1.25 wraps to 0.25, the red texel")
                assertColor(floats, 1, 0f, 0f, 1f, 1f, "u = -0.25 wraps to 0.75, the blue texel")
            }
        }
    }
}
