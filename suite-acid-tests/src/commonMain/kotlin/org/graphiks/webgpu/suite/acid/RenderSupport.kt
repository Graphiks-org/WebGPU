package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor

/**
 * A single triangle whose vertices are outside the viewport, so it covers the whole render target.
 *
 * Cases that need a different colour, depth or input keep their own shader next to the behaviour
 * they assert.
 */
internal const val FULLSCREEN_TRIANGLE_WGSL = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

/**
 * Creates an RGBA8 colour target that can be rendered to and copied out. The case owns the pass,
 * the draw and the readback; this only allocates the attachment.
 */
internal fun createColorTarget(device: GPUDevice, width: Int, height: Int): GPUTexture =
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(width.toUInt(), height.toUInt(), 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        ),
    )
