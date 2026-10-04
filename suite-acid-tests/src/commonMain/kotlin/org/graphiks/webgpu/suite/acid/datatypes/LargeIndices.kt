package org.graphiks.webgpu.suite.acid.datatypes

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.Origin3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val LARGE_INDEX_SHADER = """
struct IndexOut {
    @builtin(position) position: vec4f,
    @location(0) @interpolate(flat) instance: u32,
};

@vertex fn indexMain(
    @builtin(vertex_index) vertexIndex: u32,
    @builtin(instance_index) instanceIndex: u32,
) -> IndexOut {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    var output: IndexOut;
    output.position = vec4f(points[vertexIndex], 0.5, 1.0);
    output.instance = instanceIndex;
    return output;
}

@fragment fn idsMain(@location(0) @interpolate(flat) instance: u32) -> @location(0) vec4u {
    return vec4u(instance, 0u, 0u, 0u);
}

@vertex fn offsetMain(@builtin(vertex_index) vertexIndex: u32) -> @builtin(position) vec4f {
    // The draw passes firstVertex 2^30, so the geometry only exists when vertex_index carries it.
    let local = vertexIndex - 0x40000000u;
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[local], 0.5, 1.0);
}

@fragment fn redMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

/**
 * The index aliases carry values across their whole 32-bit range. A draw with
 * `firstInstance` 2^31 delivers that exact value through the flat `instance_index` fragment input,
 * read back as the unmodified word 0x80000000. A draw with `firstVertex` 2^30 only renders its
 * triangle when `vertex_index` carries the offset, so the rendered pixel is itself the proof.
 * The coordinate alias is refused past its limit: a texture whose width is 2^31 fails validation
 * because no adapter's `maxTextureDimension2D` reaches that far.
 */
@AcidTest(
    id = AcidCaseId.DataTypesLargeIndices,
    family = AcidFamily.DataTypesDescriptorsFlagsSwizzle,
    contract = [
        ApiSymbols.GPURenderPassEncoder_draw,
        ApiSymbols.GPUTextureFormat_RGBA32Uint,
        ApiSymbols.GPUTextureUsage_RenderAttachment,
        ApiSymbols.GPUTextureUsage_CopySrc,
        ApiSymbols.GPUCommandEncoder_copyTextureToBuffer,
        ApiSymbols.GPUTexelCopyBufferInfo,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUBufferUsage_MapRead,
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun largeIndices(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        LARGE_INDEX_SHADER,
        colorTargets = listOf(ColorTargetState(GPUTextureFormat.RGBA32Uint)),
        vertexEntryPoint = "indexMain",
        fragmentEntryPoint = "idsMain",
    ).use { idsPipeline ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(3u, 3u, 1u),
                format = GPUTextureFormat.RGBA32Uint,
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
                    pass.setPipeline(idsPipeline)
                    // The full-range firstInstance reaches every fragment untouched through the
                    // flat input; firstVertex stays 0 so the geometry covers the target.
                    pass.draw(3u, 1u, 0u, 0x80000000u)
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
                        TexelCopyTextureInfo(texture = target, origin = Origin3D(1u, 1u, 0u)),
                        TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 1u),
                        Extent3D(1u, 1u, 1u),
                    )
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }

                staging.mapAsync(org.graphiks.webgpu.GPUMapMode.Read).getOrThrow()
                try {
                    val words = ArrayBuffer.of(staging.getMappedRange().toByteArray()).toUIntArray()
                    assertTrue(
                        words[0] == 0x80000000u,
                        "instance_index must deliver the firstInstance 0x80000000 untouched, observed ${words[0]}",
                    )
                } finally {
                    staging.unmap()
                }
            }
        }
    }

    createRenderPipeline(
        device,
        LARGE_INDEX_SHADER,
        vertexEntryPoint = "offsetMain",
        fragmentEntryPoint = "redMain",
    ).use { offsetPipeline ->
        createColorTarget(device, 8, 8).use { target ->
            target.createView().use { view ->
                device.createCommandEncoder().use { encoder ->
                    val pass = encoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachments = listOf(
                                RenderPassColorAttachment(
                                    view = view,
                                    loadOp = GPULoadOp.Clear,
                                    storeOp = GPUStoreOp.Store,
                                    clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                ),
                            ),
                        ),
                    )
                    pass.setPipeline(offsetPipeline)
                    // vertex_index must carry the 2^30 firstVertex for the geometry to exist.
                    pass.draw(3u, 1u, 0x40000000u, 0u)
                    pass.end()
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
            }

            val pixels = readRgba8(device, target, 8, 8)
            assertPixel(pixels, 8, 4, 4, 255, 0, 0, 255)
            assertPixel(pixels, 8, 0, 0, 255, 0, 0, 255)
        }
    }

    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(0x80000000u, 1u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.TextureBinding,
            ),
        ).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }
}
