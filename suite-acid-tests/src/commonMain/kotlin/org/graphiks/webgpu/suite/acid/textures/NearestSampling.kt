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
import kotlin.test.assertEquals

private const val NEAREST_SAMPLE_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;
@group(0) @binding(2) var samp: sampler;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureSampleLevel(tex, samp, vec2f(0.25, 0.5), 0.0);
    out[1] = textureSampleLevel(tex, samp, vec2f(0.75, 0.5), 0.0);
    out[2] = textureSampleLevel(tex, samp, vec2f(0.375, 0.5), 0.0);
}
"""

/**
 * With nearest filtering, a texture coordinate inside a texel picks that texel: u = 0.25 is the red
 * texel and u = 0.75 the blue one, with no blending. The off-centre u = 0.375 stays entirely inside
 * the red texel, so a linear filter — which would blend in a quarter of the blue texel — fails it.
 */
@AcidTest(
    id = AcidCaseId.TexturesSamplingNearest,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUSamplerDescriptor_magFilter,
        ApiSymbols.GPUSamplerDescriptor_minFilter,
        ApiSymbols.GPUFilterMode_Nearest,
        ApiSymbols.GPUTexture_createView,
    ],
)
suspend fun nearestSampling(device: GPUDevice) = withValidationScope(device) {
    createRedBlueTexture(device).use { texture ->
        device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest,
                mipmapFilter = GPUMipmapFilterMode.Nearest,
            ),
        ).use { sampler ->
            texture.createView().use { view ->
                val floats = ArrayBuffer.of(
                    computeTextureRead(
                        device = device,
                        shaderCode = NEAREST_SAMPLE_SHADER,
                        textureView = view,
                        textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                        outputBytes = 48uL,
                        sampler = sampler,
                    ),
                ).toFloatArray()

                assertColor(floats, 0, 1f, 0f, 0f, 1f, "u = 0.25 picks the red texel")
                assertColor(floats, 1, 0f, 0f, 1f, 1f, "u = 0.75 picks the blue texel")
                assertColor(floats, 2, 1f, 0f, 0f, 1f, "u = 0.375 is still entirely inside the red texel")
            }
        }
    }
}

/** Asserts the four channels of output [index] with a tight tolerance. */
internal fun assertColor(
    floats: FloatArray,
    index: Int,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    message: String,
    tolerance: Float = 1e-6f,
) {
    assertEquals(r, floats[index * 4], tolerance, "$message (R)")
    assertEquals(g, floats[index * 4 + 1], tolerance, "$message (G)")
    assertEquals(b, floats[index * 4 + 2], tolerance, "$message (B)")
    assertEquals(a, floats[index * 4 + 3], tolerance, "$message (A)")
}
