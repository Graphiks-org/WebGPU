@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.ToolkitProgression
import org.graphiks.webgpu.toolkit.surface.withSurface

/**
 * Drawable target state for [FakeSurfaceTarget]: available, 640 by 480, of [generation].
 */
private fun drawableState(generation: Long = 1L): TargetState =
    TargetState(generation = generation, extent = Extent2D(width = 640u, height = 480u), available = true)

/**
 * One feedable frame for a [FakeSurfaceHandle.frames] channel.
 */
private fun acquiredFrame(): AcquiredFrame =
    AcquiredFrame(view = FakeTextureView(), extent = Extent2D(width = 640u, height = 480u))

class PresentationTest : FreeSpec({
    "withSurface negotiates configuration from capabilities" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val result = withSurface(fake, target) { session, surface ->
                surface.configuration.value!!.format shouldBe FakeSurfaceHandle.DEFAULT_CAPABILITIES.preferredFormat
                val handle = fake.createdHandles.single()
                val recorded = handle.recordedConfigures.single()
                recorded.format shouldBe FakeSurfaceHandle.DEFAULT_CAPABILITIES.preferredFormat
                recorded.device shouldBe session.device
                "negotiated"
            }
            result shouldBe "negotiated"
        }
    }

    "frame rejects double present" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val acquired = acquiredFrame()
            val rejected = shouldThrow<IllegalStateException> {
                withSurface(fake, target) { _, surface ->
                    fake.createdHandles.single().frames.send(acquired)
                    surface.withFrame { frame ->
                        frame.present()
                        frame.present()
                    }
                }
            }
            rejected.message shouldContain "already presented"
            val handle = fake.createdHandles.single()
            handle.recordedPresents shouldContainExactly listOf(acquired)
            handle.recordedReleases shouldContainExactly listOf(acquired)
        }
    }

    "zero extent suspends frame acquisition" {
        runTest {
            val fake = RecordingFakeExtension().apply { progression = ToolkitProgression.EventLoop }
            val target = FakeSurfaceTarget(
                TargetState(generation = 1, extent = Extent2D(width = 0u, height = 0u), available = true),
            )
            withSurface(fake, target) { _, surface ->
                fake.createdHandles.single().frames.send(acquiredFrame())
                var completed = false
                launch { surface.withFrame { }; completed = true }
                advanceUntilIdle()
                completed shouldBe false
                target.mutableState.value = drawableState()
                advanceUntilIdle()
                completed shouldBe true
            }
        }
    }

    "unavailable target suspends frame acquisition" {
        runTest {
            val fake = RecordingFakeExtension().apply { progression = ToolkitProgression.EventLoop }
            val target = FakeSurfaceTarget(
                TargetState(generation = 1, extent = Extent2D(width = 640u, height = 480u), available = false),
            )
            withSurface(fake, target) { _, surface ->
                fake.createdHandles.single().frames.send(acquiredFrame())
                var completed = false
                launch { surface.withFrame { }; completed = true }
                advanceUntilIdle()
                completed shouldBe false
                target.mutableState.value = drawableState()
                advanceUntilIdle()
                completed shouldBe true
            }
        }
    }

    "stale lease rolls back" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState(generation = 5))
            val stale = FakeLease(generation = 4)
            target.retainResults.send(stale)
            withSurface(fake, target) { session, surface ->
                surface.configuration.value!!.device shouldBe session.device
                target.recordedRetires shouldContainExactly listOf(stale)
                target.recordedRetains.size shouldBe 2
                target.recordedRetains.first() shouldBe stale
                fake.createSurfaceLeases.size shouldBe 1
                fake.createSurfaceLeases.single().generation shouldBe 5
            }
        }
    }

    "block return presents once" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val acquired = acquiredFrame()
            withSurface(fake, target) { _, surface ->
                val handle = fake.createdHandles.single()
                handle.frames.send(acquired)
                var seenView: GPUTextureView? = null
                var seenExtent: Extent2D? = null
                surface.withFrame { frame ->
                    seenView = frame.view
                    seenExtent = frame.extent
                }
                seenView shouldBe acquired.view
                seenExtent shouldBe acquired.extent
                handle.recordedPresents shouldContainExactly listOf(acquired)
                handle.recordedReleases shouldContainExactly listOf(acquired)
            }
        }
    }

    "cancellation abandons the open frame" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val acquired = acquiredFrame()
            withSurface(fake, target) { _, surface ->
                val handle = fake.createdHandles.single()
                handle.frames.send(acquired)
                lateinit var job: Job
                job = launch {
                    surface.withFrame {
                        job.cancel()
                        throw CancellationException("frame abandoned")
                    }
                }
                job.join()
                handle.recordedPresents shouldContainExactly emptyList<AcquiredFrame>()
                handle.recordedReleases shouldContainExactly listOf(acquired)
            }
        }
    }

    "configure failure closes the created handle" {
        runTest {
            val fake = RecordingFakeExtension()
            val failure = IllegalStateException("unsupported format and alpha mode combination")
            fake.failFirstConfigure = failure
            val target = FakeSurfaceTarget(drawableState())
            val rejected = shouldThrow<IllegalStateException> {
                withSurface(fake, target) { _, _ ->
                    error("block must not run when configure failed")
                }
            }
            rejected shouldBe failure
            val handle = fake.createdHandles.single()
            handle.closeCalls shouldBe 1
            handle.recordedConfigures shouldContainExactly emptyList<PresentationConfiguration>()
            target.recordedRetires shouldContainExactly target.recordedRetains
        }
    }
})
