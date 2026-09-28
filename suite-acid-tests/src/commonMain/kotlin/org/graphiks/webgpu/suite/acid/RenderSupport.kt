package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUColorTargetState
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDepthStencilState
import org.graphiks.webgpu.GPUMultisampleState
import org.graphiks.webgpu.GPUPrimitiveState
import org.graphiks.webgpu.GPURenderPassEncoder
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUVertexBufferLayout
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.MultisampleState
import org.graphiks.webgpu.descriptors.PrimitiveState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.VertexState

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

/**
 * Builds a render pipeline from one shader module with the conventional `vertexMain`/`fragmentMain`
 * entry points. The case still declares its own vertex layouts, colour targets and primitive state.
 */
internal fun createRenderPipeline(
    device: GPUDevice,
    code: String,
    vertexLayouts: List<GPUVertexBufferLayout> = emptyList(),
    colorTargets: List<GPUColorTargetState> = listOf(ColorTargetState(GPUTextureFormat.RGBA8Unorm)),
    primitive: GPUPrimitiveState = PrimitiveState(),
    multisample: GPUMultisampleState = MultisampleState(),
    depthStencil: GPUDepthStencilState? = null,
    vertexEntryPoint: String = "vertexMain",
    fragmentEntryPoint: String = "fragmentMain",
): GPURenderPipeline {
    device.createShaderModule(ShaderModuleDescriptor(code = code)).use { shader ->
        return device.createRenderPipeline(
            RenderPipelineDescriptor(
                vertex = VertexState(module = shader, buffers = vertexLayouts, entryPoint = vertexEntryPoint),
                primitive = primitive,
                depthStencil = depthStencil,
                multisample = multisample,
                fragment = FragmentState(module = shader, targets = colorTargets, entryPoint = fragmentEntryPoint),
            ),
        )
    }
}

/**
 * Runs one render pass on [target] with a colour clear, lets [draw] configure the pipeline and the
 * draw calls, ends the pass and reads the target back as tightly packed RGBA8.
 */
internal suspend fun renderAndRead(
    device: GPUDevice,
    target: GPUTexture,
    width: Int,
    height: Int,
    clear: Color,
    draw: (GPURenderPassEncoder) -> Unit,
): ByteArray {
    target.createView().use { view ->
        device.createCommandEncoder().use { encoder ->
            val pass = encoder.beginRenderPass(
                RenderPassDescriptor(
                    colorAttachments = listOf(
                        RenderPassColorAttachment(
                            view = view,
                            loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                            storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                            clearValue = clear,
                        ),
                    ),
                ),
            )
            draw(pass)
            pass.end()
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
    }
    return readRgba8(device, target, width, height)
}
