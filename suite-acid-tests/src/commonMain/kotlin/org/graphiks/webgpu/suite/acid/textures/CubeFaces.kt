package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUAddressMode
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals

private const val CUBE_SAMPLE_SHADER = """
@group(0) @binding(0) var tex: texture_cube<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;
@group(0) @binding(2) var samp: sampler;

@compute @workgroup_size(1)
fn main() {
    let directions = array<vec3f,6>(
        vec3f(1, 0, 0), vec3f(-1, 0, 0),
        vec3f(0, 1, 0), vec3f(0, -1, 0),
        vec3f(0, 0, 1), vec3f(0, 0, -1),
    );
    for (var i = 0u; i < 6u; i = i + 1u) {
        out[i] = textureSampleLevel(tex, samp, directions[i], 0.0);
    }
}
"""

/**
 * A cube view maps each axis direction to its own layer: sampling +X, -X, +Y, -Y, +Z and -Z with a
 * nearest sampler returns the red, green, blue, yellow, magenta and cyan layers in that order.
 */
@AcidTest(
    id = AcidCaseId.TexturesCubeFaces,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUDevice_createSampler,
        ApiSymbols.GPUTextureViewDescriptor_dimension,
        ApiSymbols.GPUTextureViewDescriptor_arrayLayerCount,
        ApiSymbols.GPUTextureSampleType_Float,
    ],
)
suspend fun cubeFaces(device: GPUDevice) = withValidationScope(device) {
    val colors = listOf(
        byteArrayOf(255.toByte(), 0, 0, 255.toByte()),
        byteArrayOf(0, 255.toByte(), 0, 255.toByte()),
        byteArrayOf(0, 0, 255.toByte(), 255.toByte()),
        byteArrayOf(255.toByte(), 255.toByte(), 0, 255.toByte()),
        byteArrayOf(255.toByte(), 0, 255.toByte(), 255.toByte()),
        byteArrayOf(0, 255.toByte(), 255.toByte(), 255.toByte()),
    )

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 6u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        colors.forEachIndexed { layer, color ->
            writeSolid(device, texture, 0u, 2, 2, color, layer = layer.toUInt())
        }

        device.createSampler(
            SamplerDescriptor(
                addressModeU = GPUAddressMode.ClampToEdge,
                addressModeV = GPUAddressMode.ClampToEdge,
                addressModeW = GPUAddressMode.ClampToEdge,
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest,
                mipmapFilter = GPUMipmapFilterMode.Nearest,
            ),
        ).use { sampler ->
            texture.createView(
                TextureViewDescriptor(dimension = GPUTextureViewDimension.Cube, arrayLayerCount = 6u),
            ).use { view ->
                val floats = ArrayBuffer.of(
                    computeTextureRead(
                        device = device,
                        shaderCode = CUBE_SAMPLE_SHADER,
                        textureView = view,
                        textureLayout = TextureBindingLayout(
                            sampleType = GPUTextureSampleType.Float,
                            viewDimension = GPUTextureViewDimension.Cube,
                        ),
                        outputBytes = 96uL,
                        sampler = sampler,
                    ),
                ).toFloatArray()

                val expected = listOf(
                    floatArrayOf(1f, 0f, 0f, 1f),
                    floatArrayOf(0f, 1f, 0f, 1f),
                    floatArrayOf(0f, 0f, 1f, 1f),
                    floatArrayOf(1f, 1f, 0f, 1f),
                    floatArrayOf(1f, 0f, 1f, 1f),
                    floatArrayOf(0f, 1f, 1f, 1f),
                )
                expected.forEachIndexed { face, color ->
                    for (channel in 0..3) {
                        assertEquals(
                            color[channel],
                            floats[face * 4 + channel],
                            1e-6f,
                            "Face $face channel $channel",
                        )
                    }
                }
            }
        }
    }
}
