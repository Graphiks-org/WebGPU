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
import kotlin.test.assertContentEquals

private const val DIMENSIONS_SHADER = """
@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(1)
fn main() {
    let level0 = textureDimensions(tex, 0);
    let level1 = textureDimensions(tex, 1);
    out[0] = level0.x;
    out[1] = level0.y;
    out[2] = level1.x;
    out[3] = level1.y;
    out[4] = textureNumLevels(tex);
}
"""

/**
 * `textureDimensions` and `textureNumLevels` are relative to the view: a view over levels 1..2 of an
 * 8×4 texture reports 4×2 at view level 0, 2×1 at view level 1 and two levels in total.
 */
@AcidTest(
    id = AcidCaseId.TexturesViewDimensions,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor_baseMipLevel,
        ApiSymbols.GPUTextureViewDescriptor_mipLevelCount,
        ApiSymbols.GPUTexture_width,
        ApiSymbols.GPUTexture_height,
    ],
)
suspend fun viewDimensions(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(8u, 4u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.TextureBinding,
            mipLevelCount = 3u,
        ),
    ).use { texture ->
        texture.createView(
            TextureViewDescriptor(baseMipLevel = 1u, mipLevelCount = 2u, dimension = GPUTextureViewDimension.TwoD),
        ).use { view ->
            val words = ArrayBuffer.of(
                computeTextureRead(
                    device = device,
                    shaderCode = DIMENSIONS_SHADER,
                    textureView = view,
                    textureLayout = TextureBindingLayout(sampleType = GPUTextureSampleType.Float),
                    outputBytes = 20uL,
                ),
            ).toUIntArray()

            assertContentEquals(uintArrayOf(4u, 2u, 2u, 1u, 2u), words)
        }
    }
}
