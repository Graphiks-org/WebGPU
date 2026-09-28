package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals

private const val ARRAY_LOAD_SHADER = """
@group(0) @binding(0) var tex: texture_2d_array<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<vec4f>;

@compute @workgroup_size(1)
fn main() {
    out[0] = textureLoad(tex, vec2i(0, 0), 0, 0);
    out[1] = textureLoad(tex, vec2i(0, 0), 1, 0);
}
"""

/**
 * A `baseArrayLayer` view exposes a sub-range of an array texture: with `baseArrayLayer = 1` the
 * view's local layer 0 is the green texture layer 1 and its local layer 1 is the blue layer 2.
 */
@AcidTest(
    id = AcidCaseId.TexturesViewBaseLayer,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor_baseArrayLayer,
        ApiSymbols.GPUTextureViewDescriptor_arrayLayerCount,
        ApiSymbols.GPUTextureViewDescriptor_dimension,
    ],
)
suspend fun arrayLayerView(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(1u, 1u, 3u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.CopyDst or GPUTextureUsage.TextureBinding,
        ),
    ).use { texture ->
        writeSolid(device, texture, 0u, 1, 1, byteArrayOf(255.toByte(), 0, 0, 255.toByte()), layer = 0u)
        writeSolid(device, texture, 0u, 1, 1, byteArrayOf(0, 255.toByte(), 0, 255.toByte()), layer = 1u)
        writeSolid(device, texture, 0u, 1, 1, byteArrayOf(0, 0, 255.toByte(), 255.toByte()), layer = 2u)

        texture.createView(
            TextureViewDescriptor(
                dimension = GPUTextureViewDimension.TwoDArray,
                baseArrayLayer = 1u,
                arrayLayerCount = 2u,
            ),
        ).use { view ->
            val floats = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = ARRAY_LOAD_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(
                        sampleType = GPUTextureSampleType.Float,
                        viewDimension = GPUTextureViewDimension.TwoDArray,
                    ),
                    outputBytes = 32uL,
                ),
            ).toFloatArray()

            assertEquals(0f, floats[0], 1e-6f, "Local layer 0 is the green layer")
            assertEquals(1f, floats[1], 1e-6f, "Local layer 0 is the green layer")
            assertEquals(0f, floats[4], 1e-6f, "Local layer 1 is the blue layer")
            assertEquals(1f, floats[6], 1e-6f, "Local layer 1 is the blue layer")
        }
    }
}
