@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.browser.CaseResult
import org.graphiks.webgpu.suite.demos.particles.ParticleScene

/** The two demo verification scenarios. The ids are the ones the site and the runner expect. */
internal const val ComputeRenderReadbackId = "particles.compute-render-readback"
internal const val BoundsPauseResetId = "particles.bounds-pause-reset"

/** The report the browser publishes in `globalThis.graphiksDemoReport`, separate from the acid-test report. */
@Serializable
internal data class DemoReport(
    val schemaVersion: Int = 1,
    val buildCommit: String,
    val buildVersion: String,
    val cases: List<CaseResult>,
    val fatalError: String? = null,
)

/**
 * Runs the two demo verification scenarios on [device] and returns one [CaseResult] each.
 *
 * The checks read back real GPU results: the computed particle values and the rendered pixels. They
 * are deliberately independent of the acid-test catalogue and never enter the conformance inventory.
 */
internal suspend fun particleGpuResults(device: GPUDevice): List<CaseResult> {
    val scenarios: List<Pair<String, suspend (GPUDevice) -> Unit>> = listOf(
        ComputeRenderReadbackId to ::checkComputeRenderReadback,
        BoundsPauseResetId to ::checkBoundsPauseReset,
    )
    return scenarios.map { (id, scenario) ->
        try {
            scenario(device)
            CaseResult(id, "passed")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            CaseResult(id, "failed", failure.stackTraceToString())
        }
    }
}

/**
 * Runs both demo scenarios and fails on the first problem, for consumers that only need a pass/fail.
 */
suspend fun checkParticleGpu(device: GPUDevice) {
    particleGpuResults(device).firstOrNull { it.status != "passed" }?.let { result ->
        error("${result.id}: ${result.diagnostic}")
    }
}

/**
 * Encodes one compute-then-render frame from a single particle, then reads back both the computed
 * position and the rendered pixels from the same submission. The particle starts at the origin with
 * velocity `(1, 0)` and a `0.02` delta, so after the frame it must sit at `x=0.02` and its disk must
 * be visible near the canvas center.
 */
private suspend fun checkComputeRenderReadback(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(256u, 256u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        ),
    ).use { texture ->
        device.createBuffer(
            BufferDescriptor(size = 16uL, usage = GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
        ).use { particleStaging ->
            device.createBuffer(
                BufferDescriptor(size = 262144uL, usage = GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
            ).use { pixelStaging ->
                ParticleScene.create(
                    device,
                    GPUTextureFormat.RGBA8Unorm,
                    floatArrayOf(0f, 0f, 1f, 0f),
                ).use { scene ->
                    val view = texture.createView()
                    try {
                        device.createCommandEncoder().use { encoder ->
                            scene.encodeFrame(encoder, view, 256, 256, 0.02f)
                            encoder.copyBufferToBuffer(scene.particleBuffer, 0uL, particleStaging, 0uL, 16uL)
                            encoder.copyTextureToBuffer(
                                source = TexelCopyTextureInfo(texture),
                                destination = TexelCopyBufferInfo(
                                    buffer = pixelStaging,
                                    bytesPerRow = 1024u,
                                    rowsPerImage = 256u,
                                ),
                                copySize = Extent3D(256u, 256u),
                            )
                            val commandBuffer = encoder.finish()
                            try {
                                device.queue.submit(listOf(commandBuffer))
                                device.queue.onSubmittedWorkDone().getOrThrow()
                            } finally {
                                commandBuffer.close()
                            }
                        }
                    } finally {
                        view.close()
                    }

                    particleStaging.mapAsync(GPUMapMode.Read).getOrThrow()
                    try {
                        val actual = particleStaging.getMappedRange().toFloatArray()
                        assertTrue(abs(actual[0] - 0.02f) < 0.00001f, "Computed x was ${actual[0]}")
                        assertTrue(abs(actual[1]) < 0.00001f, "Computed y was ${actual[1]}")
                        assertEquals(1f, actual[2], "Velocity x must be unchanged")
                        assertEquals(0f, actual[3], "Velocity y must be unchanged")
                    } finally {
                        particleStaging.unmap()
                    }

                    pixelStaging.mapAsync(GPUMapMode.Read).getOrThrow()
                    try {
                        val pixels = pixelStaging.getMappedRange().toUByteArray()
                        val center = 128 * 1024 + 130 * 4
                        assertTrue(pixels[center].toInt() > 40, "Red channel was ${pixels[center]}")
                        assertTrue(pixels[center + 1].toInt() > 150, "Green channel was ${pixels[center + 1]}")
                        assertTrue(pixels[center + 2].toInt() > 200, "Blue channel was ${pixels[center + 2]}")
                        assertEquals(255u.toUByte(), pixels[center + 3], "Alpha must be opaque")
                        assertEquals(0u.toUByte(), pixels[0], "The cleared background must stay black")
                        assertEquals(0u.toUByte(), pixels[1], "The cleared background must stay black")
                        assertEquals(0u.toUByte(), pixels[2], "The cleared background must stay black")
                    } finally {
                        pixelStaging.unmap()
                    }
                }
            }
        }
    }
}

/**
 * Exercises the partial last workgroup (65 particles, not a multiple of 64), the bounce bounds, a
 * paused frame and a reset. The buffer is read directly, so a wrong `arrayLength` guard or a missed
 * bounce shows up as data, not as a rendering artefact.
 */
private suspend fun checkBoundsPauseReset(device: GPUDevice) = withValidationScope(device) {
    val initial = FloatArray(65 * 4)
    for (index in 0 until 65) {
        initial[index * 4] = 0f
        initial[index * 4 + 1] = 0f
        initial[index * 4 + 2] = 0.5f
        initial[index * 4 + 3] = 0f
    }
    initial[0] = 0.94f
    initial[1] = 0f
    initial[2] = 1f
    initial[3] = 0f
    initial[64 * 4] = -0.94f
    initial[64 * 4 + 1] = 0f
    initial[64 * 4 + 2] = -1f
    initial[64 * 4 + 3] = 0f
    val original = initial.copyOf()

    device.createBuffer(
        BufferDescriptor(size = 65uL * 16uL, usage = GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
    ).use { staging ->
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(256u, 256u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            ),
        ).use { texture ->
            ParticleScene.create(device, GPUTextureFormat.RGBA8Unorm, initial).use { scene ->
                val view = texture.createView()
                try {
                    val afterMove = renderAndReadParticles(device, scene, view, staging, 65, 0.02f)
                    assertClose(0.95f, afterMove[0], "Bounced x of the first particle")
                    assertEquals(-1f, afterMove[2], "Bounced velocity x of the first particle")
                    assertClose(-0.95f, afterMove[64 * 4], "Bounced x of the last particle")
                    assertEquals(1f, afterMove[64 * 4 + 2], "Bounced velocity x of the last particle")
                    for (index in 1 until 64) {
                        assertClose(0.01f, afterMove[index * 4], "Moved x of particle $index")
                        assertEquals(0.5f, afterMove[index * 4 + 2], "Velocity x of particle $index")
                    }

                    val afterPause = renderAndReadParticles(device, scene, view, staging, 65, 0f)
                    assertTrue(
                        afterMove.contentEquals(afterPause),
                        "A zero-delta frame must not change the particle data",
                    )

                    scene.reset(original)
                    val afterReset = renderAndReadParticles(device, scene, view, staging, 65, 0f)
                    assertTrue(
                        original.contentEquals(afterReset),
                        "Reset must restore the initial particle data",
                    )
                } finally {
                    view.close()
                }
            }
        }
    }
}

/** Encodes one frame, copies the particle buffer into [staging], submits and reads the particles back. */
private suspend fun renderAndReadParticles(
    device: GPUDevice,
    scene: ParticleScene,
    target: GPUTextureView,
    staging: GPUBuffer,
    count: Int,
    deltaSeconds: Float,
): FloatArray {
    device.createCommandEncoder().use { encoder ->
        scene.encodeFrame(encoder, target, 256, 256, deltaSeconds)
        encoder.copyBufferToBuffer(scene.particleBuffer, 0uL, staging, 0uL, (count * 16).toULong())
        val commandBuffer = encoder.finish()
        try {
            device.queue.submit(listOf(commandBuffer))
            device.queue.onSubmittedWorkDone().getOrThrow()
        } finally {
            commandBuffer.close()
        }
    }
    staging.mapAsync(GPUMapMode.Read).getOrThrow()
    return try {
        staging.getMappedRange().toFloatArray()
    } finally {
        staging.unmap()
    }
}

private fun assertClose(expected: Float, actual: Float, message: String) {
    assertTrue(abs(expected - actual) < 1e-5f, "$message: expected $expected but was $actual")
}

/** Pushes a validation scope, runs [block] and fails when the scope reports an unexpected error. */
private suspend fun withValidationScope(device: GPUDevice, block: suspend () -> Unit) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    var bodyFailure: Throwable? = null
    try {
        block()
    } catch (failure: Throwable) {
        bodyFailure = failure
        throw failure
    } finally {
        try {
            val error = device.popErrorScope().getOrThrow()
            assertNull(error, "Unexpected validation error: ${error?.message}")
        } catch (scopeFailure: Throwable) {
            val original = bodyFailure
            if (original == null) throw scopeFailure
            original.addSuppressed(scopeFailure)
        }
    }
}
