package org.graphiks.webgpu.suite.acid.queries

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPassTimestampWrites
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

private const val SENTINEL_WORD = 0xFFFFFFFFu

/**
 * A render pass draws red while writing begin and end timestamps into query indices 1 and 2 of a
 * count-4 set. `resolveQuerySet` writes the two `u64` results at offset 256 of a `0xff`-prefilled
 * 512-byte buffer; the 16 written bytes replace their sentinels, every other range stays intact, and
 * the end timestamp is not before the begin one (equality is allowed). The rendered red is also
 * checked. This validates query use and resolution, not temporal precision.
 *
 * Requires the optional `TimestampQuery` feature; the runner reports it as `unsupported` otherwise.
 */
@AcidTest(
    id = AcidCaseId.QueriesRenderTimestampWrites,
    family = AcidFamily.QueriesTimestamps,
    requiredFeatures = [GPUFeatureName.TimestampQuery],
    contract = [
        ApiSymbols.GPUDevice_createQuerySet,
        ApiSymbols.GPUQueryType_Timestamp,
        ApiSymbols.GPURenderPassDescriptor_timestampWrites,
        ApiSymbols.GPURenderPassTimestampWrites,
        ApiSymbols.GPURenderPassTimestampWrites_beginningOfPassWriteIndex,
        ApiSymbols.GPURenderPassTimestampWrites_endOfPassWriteIndex,
        ApiSymbols.GPUCommandEncoder_resolveQuerySet,
    ],
)
suspend fun renderTimestamps(device: GPUDevice) = withValidationScope(device) {
    device.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Timestamp, count = 4u)).use { querySet ->
        device.createBuffer(
            BufferDescriptor(
                size = 512uL,
                usage = GPUBufferUsage.QueryResolve or GPUBufferUsage.CopySrc,
                mappedAtCreation = true,
            ),
        ).use { destination ->
            destination.getMappedRange().setBytes(0uL, ByteArray(512) { 0xFF.toByte() })
            destination.unmap()

            device.createBuffer(
                BufferDescriptor(512uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead),
            ).use { staging ->
                createRenderPipeline(device, FULLSCREEN_TRIANGLE_WGSL).use { pipeline ->
                    createColorTarget(device, 16, 16).use { target ->
                        target.createView().use { view ->
                            device.createCommandEncoder().use { encoder ->
                                val pass = encoder.beginRenderPass(
                                    RenderPassDescriptor(
                                        colorAttachments = listOf(
                                            RenderPassColorAttachment(
                                                view = view,
                                                loadOp = org.graphiks.webgpu.GPULoadOp.Clear,
                                                storeOp = org.graphiks.webgpu.GPUStoreOp.Store,
                                                clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                            ),
                                        ),
                                        timestampWrites = RenderPassTimestampWrites(
                                            querySet = querySet,
                                            beginningOfPassWriteIndex = 1u,
                                            endOfPassWriteIndex = 2u,
                                        ),
                                    ),
                                )
                                pass.setPipeline(pipeline)
                                pass.draw(3u)
                                pass.end()

                                encoder.resolveQuerySet(querySet, 1u, 2u, destination, 256uL)
                                encoder.copyBufferToBuffer(destination, 0uL, staging, 0uL, 512uL)
                                encoder.finish().use { device.queue.submit(listOf(it)) }
                            }
                        }

                        val pixels = readRgba8(device, target, 16, 16)
                        assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
                    }
                }

                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                val bytes = try {
                    staging.getMappedRange().toByteArray().copyOf(512)
                } finally {
                    staging.unmap()
                }

                assertContentEquals(
                    ByteArray(256) { 0xFF.toByte() },
                    bytes.copyOfRange(0, 256),
                    "Bytes before the resolve offset must keep the 0xff sentinel",
                )
                assertContentEquals(
                    ByteArray(240) { 0xFF.toByte() },
                    bytes.copyOfRange(272, 512),
                    "Bytes after the two resolved queries must keep the 0xff sentinel",
                )

                val words = ArrayBuffer.of(bytes).toUIntArray()
                val beginLow = words[64]
                val beginHigh = words[65]
                val endLow = words[66]
                val endHigh = words[67]
                assertTrue(
                    !(beginLow == SENTINEL_WORD && beginHigh == SENTINEL_WORD),
                    "The begin timestamp must replace its 0xff sentinel",
                )
                assertTrue(
                    !(endLow == SENTINEL_WORD && endHigh == SENTINEL_WORD),
                    "The end timestamp must replace its 0xff sentinel",
                )
                assertTrue(
                    endHigh > beginHigh || (endHigh == beginHigh && endLow >= beginLow),
                    "The end timestamp must not be before the begin timestamp",
                )
            }
        }
    }
}
