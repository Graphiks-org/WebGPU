package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureAspect
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDepthStencilAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertTrue

private const val PACKED_UINT_SHADER = """
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4u { return vec4u(1023u, 512u, 300u, 3u); }
"""

/**
 * Two formats outside the exercised set render and read back. A Depth16Unorm target cleared to
 * 0.25 copies, through its depth aspect, the 16-bit unsigned-normalized quantum of that depth
 * (16383.75 rounds to 16384, so one quantum of tolerance is allowed). An RGB10A2Uint target
 * rendered with a `vec4u(1023, 512, 300, 3)` fragment copies the exact 10/10/10/2 word 0xD2C803FF
 * — components beyond 255 and a non-zero alpha, which no 8-bit-per-channel interpretation can
 * produce.
 */
@AcidTest(
    id = AcidCaseId.FormatsDepthAndPacked,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUTextureFormat_Depth16Unorm,
        ApiSymbols.GPUTextureFormat_RGB10A2Uint,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_CopySrc,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTexelCopyTextureInfo_aspect,
        ApiSymbols.GPUTextureAspect_DepthOnly,
        ApiSymbols.GPUTexelCopyBufferInfo,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUBufferUsage_MapRead,
    ],
)
suspend fun depthAndPacked(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.Depth16Unorm,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        ),
    ).use { depth16 ->
        depth16.createView().use { view ->
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = emptyList(),
                        depthStencilAttachment = RenderPassDepthStencilAttachment(
                            view = view,
                            depthClearValue = 0.25f,
                            depthLoadOp = GPULoadOp.Clear,
                            depthStoreOp = GPUStoreOp.Store,
                        ),
                    ),
                )
                pass.end()
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        }

        device.createBuffer(
            BufferDescriptor(
                size = 1024uL,
                usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead,
                mappedAtCreation = true,
            ),
        ).use { staging ->
            staging.getMappedRange().setBytes(0uL, ByteArray(1024) { 0xA5.toByte() })
            staging.unmap()

            device.createCommandEncoder().use { encoder ->
                encoder.copyTextureToBuffer(
                    TexelCopyTextureInfo(texture = depth16, aspect = GPUTextureAspect.DepthOnly),
                    TexelCopyBufferInfo(staging, offset = 256uL, bytesPerRow = 256u, rowsPerImage = 2u),
                    Extent3D(2u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }

            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                val bytes = staging.getMappedRange().toByteArray()
                // Row 0: texels at 256-259, row 1: texels at 512-515, each an unorm16 depth.
                for (row in 0 until 2) {
                    for (texel in 0 until 2) {
                        val offset = 256 + row * 256 + texel * 2
                        val quantum =
                            (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)
                        assertTrue(
                            quantum == 16383 || quantum == 16384,
                            "Depth16Unorm texel ($texel, $row): expected the 0.25 depth quantum 16384 (±1) but observed $quantum",
                        )
                    }
                }
            } finally {
                staging.unmap()
            }
        }
    }

    createRenderPipeline(
        device,
        PACKED_UINT_SHADER,
        colorTargets = listOf(ColorTargetState(GPUTextureFormat.RGB10A2Uint)),
    ).use { pipeline ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(4u, 4u, 1u),
                format = GPUTextureFormat.RGB10A2Uint,
                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            ),
        ).use { target ->
            target.createView().use { view ->
                device.createCommandEncoder().use { encoder ->
                    val pass = encoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachments = listOf(
                                RenderPassColorAttachment(
                                    view = view,
                                    loadOp = GPULoadOp.Clear,
                                    storeOp = GPUStoreOp.Store,
                                    clearValue = Color(0.0, 0.0, 0.0, 0.0),
                                ),
                            ),
                        ),
                    )
                    pass.setPipeline(pipeline)
                    pass.draw(3u)
                    pass.end()
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            }

            device.createBuffer(
                BufferDescriptor(
                    size = 1024uL,
                    usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead,
                    mappedAtCreation = true,
                ),
            ).use { staging ->
                staging.getMappedRange().setBytes(0uL, ByteArray(1024) { 0xA5.toByte() })
                staging.unmap()

                device.createCommandEncoder().use { encoder ->
                    encoder.copyTextureToBuffer(
                        TexelCopyTextureInfo(texture = target),
                        TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u),
                        Extent3D(4u, 4u, 1u),
                    )
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }

                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                try {
                    val bytes = staging.getMappedRange().toByteArray()
                    // The 10/10/10/2 word of (1023, 512, 300, 3): an 8-bit interpretation cannot
                    // represent a component beyond 255, so the packed layout is observed.
                    val expectedWord = 1023u or (512u shl 10) or (300u shl 20) or (3u shl 30)
                    for (texel in 0 until 16) {
                        val base = (texel / 4) * 256 + (texel % 4) * 4
                        val word = (bytes[base].toUInt() and 255u) or
                            ((bytes[base + 1].toUInt() and 255u) shl 8) or
                            ((bytes[base + 2].toUInt() and 255u) shl 16) or
                            ((bytes[base + 3].toUInt() and 255u) shl 24)
                        assertTrue(
                            word == expectedWord,
                            "RGB10A2Uint texel $texel: expected the packed word 0x" +
                                expectedWord.toString(16).uppercase() + " but observed 0x" +
                                word.toString(16).uppercase(),
                        )
                    }
                } finally {
                    staging.unmap()
                }
            }
        }
    }
}
