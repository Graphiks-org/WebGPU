@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.abs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.browser.CaseResult
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionBrush
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDiffusionScene
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDisplay
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionParameters
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset
import org.graphiks.webgpu.suite.demos.reactiondiffusion.initialReactionState

internal val ReactionCheckIds = listOf(
    "reaction-diffusion.compute-render-readback",
    "reaction-diffusion.pause-step-reset",
    "reaction-diffusion.brush-boundaries",
)

/** Real GPU readbacks, separate from acid-test coverage and performance measurements. */
internal suspend fun reactionDiffusionGpuResults(device: GPUDevice): List<CaseResult> {
    val scenarios: List<suspend (ReactionFixture) -> Unit> = listOf(
        ::checkReactionComputeRender, ::checkReactionPauseReset, ::checkReactionBrush,
    )
    return ReactionCheckIds.zip(scenarios).map { (id, scenario) ->
        try {
            withReactionFixture(device, scenario)
            CaseResult(id, "passed")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            CaseResult(id, "failed", failure.stackTraceToString())
        }
    }
}

private suspend fun checkReactionComputeRender(f: ReactionFixture) {
    val expected = referenceReactionStep(initialReactionState(), ReactionPreset.Coral.parameters)
    val actual = f.frame(1)
    assertReactionState(expected, actual)
    val pixels = f.pixels()
    fun assertGray(x: Int, y: Int, value: Int) {
        val i = (y * 256 + x) * 4
        for (c in 0..2) assertTrue(abs(pixels[i + c].toInt() - value) <= 2, "Pixel ($x,$y), channel $c")
        assertEquals(255, pixels[i + 3].toInt())
    }
    assertGray(0, 0, 0)
    assertGray(64, 64, (expected[(64 * 256 + 64) * 4 + 1] * 255).toInt())
    // Reject malformed input before encoding any pass or writing uniforms.
    f.device.createCommandEncoder().use { encoder ->
        assertFailsWith<IllegalArgumentException> {
            f.scene.encodeFrame(encoder, f.view, 256, 256, 17, ReactionPreset.Coral.parameters)
        }
        assertFailsWith<IllegalArgumentException> {
            f.scene.encodeFrame(encoder, f.view, 0, 256, 0, ReactionPreset.Coral.parameters)
        }
        assertFailsWith<IllegalArgumentException> {
            f.scene.encodeFrame(encoder, f.view, 256, 256, 1, ReactionParameters(Float.NaN, 0.06f))
        }
    }
    assertFailsWith<IllegalArgumentException> { f.scene.reset(FloatArray(4)) }
    assertReactionState(actual, f.frame(0))
}

private suspend fun checkReactionPauseReset(f: ReactionFixture) {
    val initial = initialReactionState()
    assertContentEquals(initial, f.frame(0))
    var expected = initial
    for (steps in listOf(1, 2, 16, 1)) {
        repeat(steps) { expected = referenceReactionStep(expected, ReactionPreset.Coral.parameters) }
        val actual = f.frame(steps)
        assertReactionState(expected, actual)
        assertReactionState(actual, f.frame(0))
    }
    f.scene.reset()
    assertContentEquals(initial, f.frame(0))
    f.scene.close()
    f.scene.close()
    f.device.createCommandEncoder().use { encoder ->
        assertFailsWith<IllegalStateException> { f.scene.encodeStateCopy(encoder, f.staging) }
    }
    // The scene must never close the device it borrows.
    f.device.queue.onSubmittedWorkDone().getOrThrow()
}

private suspend fun checkReactionBrush(f: ReactionFixture) {
    for (brush in listOf(ReactionBrush(128, 128), ReactionBrush(0, 0))) {
        f.scene.reset()
        val expected = referenceReactionBrush(initialReactionState(), brush)
        assertContentEquals(expected, f.frame(0, brush))
        assertContentEquals(expected, f.frame(0))
        val evolved = referenceReactionStep(expected, ReactionPreset.Coral.parameters)
        assertReactionState(evolved, f.frame(1))
    }
}

private fun assertReactionState(expected: FloatArray, actual: FloatArray) {
    assertEquals(expected.size, actual.size)
    for (i in actual.indices) {
        assertTrue(actual[i].isFinite() && actual[i] in 0f..1f, "Non-finite or unbounded value at $i")
        assertTrue(abs(actual[i] - expected[i]) <= 0.0001f, "Float $i: expected ${expected[i]}, got ${actual[i]}")
    }
}

private class ReactionFixture(
    val device: GPUDevice,
    val scene: ReactionDiffusionScene,
    val target: GPUTexture,
    val view: GPUTextureView,
    val staging: GPUBuffer,
) {
    suspend fun frame(steps: Int, brush: ReactionBrush? = null): FloatArray {
        device.createCommandEncoder().use { encoder ->
            scene.encodeFrame(encoder, view, 256, 256, steps, ReactionPreset.Coral.parameters,
                display = ReactionDisplay.B, brush = brush)
            scene.encodeStateCopy(encoder, staging)
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
        device.queue.onSubmittedWorkDone().getOrThrow()
        staging.mapAsync(GPUMapMode.Read).getOrThrow()
        return try { staging.getMappedRange().toFloatArray() } finally { staging.unmap() }
    }

    suspend fun pixels(): UByteArray = device.createBuffer(
        BufferDescriptor(size = 262144uL, usage = GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
    ).use { buffer ->
        device.createCommandEncoder().use { encoder ->
            encoder.copyTextureToBuffer(TexelCopyTextureInfo(target),
                TexelCopyBufferInfo(buffer, bytesPerRow = 1024u, rowsPerImage = 256u), Extent3D(256u, 256u))
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
        buffer.mapAsync(GPUMapMode.Read).getOrThrow()
        try { buffer.getMappedRange().toUByteArray() } finally { buffer.unmap() }
    }
}

private suspend fun withReactionFixture(device: GPUDevice, block: suspend (ReactionFixture) -> Unit) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    var bodyFailure: Throwable? = null
    try {
        device.createTexture(TextureDescriptor(size = Extent3D(256u, 256u), format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc)).use { target ->
            target.createView().use { view ->
                device.createBuffer(BufferDescriptor(size = 1048576uL,
                    usage = GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                    ReactionDiffusionScene.create(device, GPUTextureFormat.RGBA8Unorm).use { scene ->
                        block(ReactionFixture(device, scene, target, view, staging))
                    }
                }
            }
        }
    } catch (failure: Throwable) {
        bodyFailure = failure
        throw failure
    } finally {
        try {
            val error = device.popErrorScope().getOrThrow()
            assertNull(error, "Unexpected validation error: ${error?.message}")
        } catch (failure: Throwable) {
            val original = bodyFailure
            if (original == null) throw failure
            original.addSuppressed(failure)
        }
    }
}
