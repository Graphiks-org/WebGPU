package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUValidationError
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
import kotlin.test.assertIs

/**
 * A render pass timestamp descriptor is validated against its query set: indices 0 and 1 of a
 * count-2 set are accepted and the pass draws, while an end index of 2 — outside the set — is a
 * validation error. Dropping the descriptor removes the error, so the case fails rather than passing
 * vacuously.
 *
 * Requires the optional `TimestampQuery` feature; the runner reports it as `unsupported` otherwise.
 */
@AcidTest(
    id = AcidCaseId.ErrorsRenderTimestampIndices,
    family = AcidFamily.ErrorsAsync,
    requiredFeatures = [GPUFeatureName.TimestampQuery],
    contract = [
        ApiSymbols.GPUDevice_createQuerySet,
        ApiSymbols.GPUQueryType_Timestamp,
        ApiSymbols.GPURenderPassDescriptor_timestampWrites,
        ApiSymbols.GPURenderPassTimestampWrites_beginningOfPassWriteIndex,
        ApiSymbols.GPURenderPassTimestampWrites_endOfPassWriteIndex,
    ],
)
suspend fun renderTimestampIndices(device: GPUDevice) = withValidationScope(device) {
    device.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Timestamp, count = 2u)).use { querySet ->
        createRenderPipeline(device, FULLSCREEN_TRIANGLE_WGSL).use { pipeline ->
            // Control: the valid descriptor is accepted and the pass draws red.
            createColorTarget(device, 16, 16).use { target ->
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
                                timestampWrites = RenderPassTimestampWrites(
                                    querySet = querySet,
                                    beginningOfPassWriteIndex = 0u,
                                    endOfPassWriteIndex = 1u,
                                ),
                            ),
                        )
                        pass.setPipeline(pipeline)
                        pass.draw(3u)
                        pass.end()
                        encoder.finish().use { device.queue.submit(listOf(it)) }
                    }
                }
                val pixels = readRgba8(device, target, 16, 16)
                assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
            }

            // Probe: an out-of-range end index is a validation error. The invalid encoder is
            // finalized inside the scope (without submitting) so the error is observed.
            createColorTarget(device, 16, 16).use { probeTarget ->
                probeTarget.createView().use { probeView ->
                    device.pushErrorScope(GPUErrorFilter.Validation)
                    try {
                        device.createCommandEncoder().use { encoder ->
                            val pass = encoder.beginRenderPass(
                                RenderPassDescriptor(
                                    colorAttachments = listOf(
                                        RenderPassColorAttachment(
                                            view = probeView,
                                            loadOp = GPULoadOp.Clear,
                                            storeOp = GPUStoreOp.Store,
                                            clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                        ),
                                    ),
                                    timestampWrites = RenderPassTimestampWrites(
                                        querySet = querySet,
                                        beginningOfPassWriteIndex = 0u,
                                        endOfPassWriteIndex = 2u,
                                    ),
                                ),
                            )
                            pass.setPipeline(pipeline)
                            pass.draw(3u)
                            pass.end()
                            encoder.finish().close()
                        }
                    } finally {
                        assertIs<GPUValidationError>(
                            device.popErrorScope().getOrThrow(),
                            "A timestamp write index outside the query set must be a validation error",
                        )
                    }
                }
            }
        }
    }
}
