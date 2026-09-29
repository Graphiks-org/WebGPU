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

private const val CLAMP_SAMPLE_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;
@group(0) @binding(2) var samp: sampler;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureSampleLevel(tex, samp, vec2f(-0.25, 0.5), 0.0);
    out[1] = textureSampleLevel(tex, samp, vec2f(1.25, 0.5), 0.0);
    out[2] = textureSampleLevel(tex, samp, vec2f(2.25, 0.5), 0.0);
}
"""

/**
 * `ClampToEdge` pins coordinates outside 0..1 to the edge texel: u = -0.25 is the red edge,
 * u = 1.25 the blue edge and u = 2.25 stays pinned to the same blue edge. `MirrorRepeat` would
 * instead wrap u = 2.25 back to the red texel, so the two modes separate here.
 */
@AcidTest(
    id = AcidCaseId.TexturesSamplingClamp,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_addressModeU,
        ApiSymbols.GPUAddressMode_ClampToEdge,
        ApiSymbols.GPUTexture_createView,
    ],
)
suspend fun clampSampling(device: GPUDevice) = withValidationScope(device) {
    createRedBlueTexture(device).use { texture ->
        device.createSampler(
            SamplerDescriptor(
                addressModeU = GPUAddressMode.ClampToEdge,
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest,
                mipmapFilter = GPUMipmapFilterMode.Nearest,
            ),
        ).use { sampler ->
            texture.createView().use { view ->
                val floats = ArrayBuffer.of(
                    computeTextureRead(
                        device = device,
                        shaderCode = CLAMP_SAMPLE_SHADER,
                        textureView = view,
                        textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                        outputBytes = 48uL,
                        sampler = sampler,
                    ),
                ).toFloatArray()

                assertColor(floats, 0, 1f, 0f, 0f, 1f, "u = -0.25 clamps to the red edge")
                assertColor(floats, 1, 0f, 0f, 1f, 1f, "u = 1.25 clamps to the blue edge")
                assertColor(floats, 2, 0f, 0f, 1f, 1f, "u = 2.25 stays clamped to the blue edge")
            }
        }
    }
}
