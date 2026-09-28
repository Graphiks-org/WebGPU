package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val HALF_GRAY_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(0.5,0.5,0.5,0.5); }
"""

/**
 * An `rgba8unorm-srgb` attachment encodes linear fragments on write: a linear 0.5 reads back as
 * about 188, and the linear 0.5 alpha reads back as about 128.
 */
@AcidTest(
    id = AcidCaseId.RenderSrgbEncode,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUTextureFormat_RGBA8UnormSrgb,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUColorTargetState_format,
    ],
)
suspend fun srgbEncode(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        HALF_GRAY_SHADER,
        colorTargets = listOf(ColorTargetState(GPUTextureFormat.RGBA8UnormSrgb)),
    ).use { pipeline ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(16u, 16u, 1u),
                format = GPUTextureFormat.RGBA8UnormSrgb,
                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            ),
        ).use { target ->
            val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                pass.setPipeline(pipeline)
                pass.draw(3u)
            }
            assertPixel(pixels, 16, 8, 8, 188, 188, 188, 128, tolerance = 1)
        }
    }
}
