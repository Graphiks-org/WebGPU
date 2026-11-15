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
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.AlphaMode
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.PresentationPolicy
import org.graphiks.webgpu.toolkit.PresentMode
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.Progression
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
            val fake = RecordingFakeExtension().apply { progression = Progression.EventLoop }
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
            val fake = RecordingFakeExtension().apply { progression = Progression.EventLoop }
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

    "replaceTarget keeps the device and swaps the presented target" {
        runTest {
            val fake = RecordingFakeExtension()
            val first = FakeSurfaceTarget(drawableState(generation = 1L))
            val second = FakeSurfaceTarget(drawableState(generation = 7L))
            withSurface(fake, first) { _, surface ->
                val keptDevice = surface.configuration.value!!.device
                surface.replaceTarget(second)

                // The configuration is preserved when the replacement supports its format.
                surface.configuration.value!!.device shouldBe keptDevice
                surface.configuration.value!!.format shouldBe
                    FakeSurfaceHandle.DEFAULT_CAPABILITIES.preferredFormat

                // The old target was retired and its handle closed; the new lease is live.
                first.recordedRetires shouldContainExactly first.recordedRetains
                fake.createdHandles.first().closeCalls shouldBe 1
                second.recordedRetains.single().generation shouldBe 7L

                val frame = acquiredFrame()
                fake.createdHandles.last().frames.send(frame)
                surface.withFrame { it.present() }
                fake.createdHandles.last().recordedPresents shouldContainExactly listOf(frame)
            }
        }
    }

    "a failed replaceTarget keeps the previous target usable" {
        runTest {
            val fake = RecordingFakeExtension()
            val first = FakeSurfaceTarget(drawableState(generation = 1L))
            val replacement = FakeSurfaceTarget(drawableState(generation = 9L))
            withSurface(fake, first) { _, surface ->
                fake.failFirstConfigure = IllegalStateException("replacement rejected")
                shouldThrow<IllegalStateException> { surface.replaceTarget(replacement) }

                // The replacement was rolled back; the previous target still presents.
                replacement.recordedRetires shouldContainExactly replacement.recordedRetains
                val frame = acquiredFrame()
                fake.createdHandles.first().frames.send(frame)
                surface.withFrame { it.present() }
                fake.createdHandles.first().recordedPresents shouldContainExactly listOf(frame)
            }
        }
    }

    "detachTarget releases the target and ignores stale generations" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState(generation = 3L))
            withSurface(fake, target) { _, surface ->
                surface.detachTarget(2L) // stale: ignored, the surface keeps its target
                fake.createdHandles.single().closeCalls shouldBe 0

                surface.detachTarget(3L)
                target.recordedRetires shouldContainExactly target.recordedRetains
                fake.createdHandles.single().closeCalls shouldBe 1
                surface.configuration.value shouldBe null
            }
        }
    }

    "withSurface applies the presentation policy" {
        runTest {
            val fake = RecordingFakeExtension()
            fake.handleCapabilities = SurfaceCapabilities(
                preferredFormat = GPUTextureFormat.BGRA8Unorm,
                formats = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm),
                alphaModes = listOf(AlphaMode.Opaque, AlphaMode.Premultiplied),
                presentModes = listOf(PresentMode.Fifo, PresentMode.Mailbox),
            )
            val target = FakeSurfaceTarget(drawableState())
            val policy = PresentationPolicy(
                preferredFormats = listOf(GPUTextureFormat.RGBA8Unorm),
                preferredAlphaModes = listOf(AlphaMode.Premultiplied),
                preferredPresentModes = listOf(PresentMode.Mailbox),
            )
            withSurface(fake, target, policy = policy) { _, surface ->
                val configuration = surface.configuration.value!!
                configuration.format shouldBe GPUTextureFormat.RGBA8Unorm
                configuration.alphaMode shouldBe AlphaMode.Premultiplied
                configuration.presentMode shouldBe PresentMode.Mailbox
            }
        }
    }

    "a preferred value the target lacks falls through to the next entry" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val policy = PresentationPolicy(
                preferredPresentModes = listOf(PresentMode.Mailbox, PresentMode.Fifo),
            )
            withSurface(fake, target, policy = policy) { _, surface ->
                surface.configuration.value!!.presentMode shouldBe PresentMode.Fifo
            }
        }
    }

    "a required value the target does not advertise fails configuration" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val policy = PresentationPolicy(requiredPresentMode = PresentMode.Immediate)
            shouldThrow<IllegalArgumentException> {
                withSurface(fake, target, policy = policy) { _, _ -> }
            }
        }
    }

    "an empty policy falls back to the target's preferred values" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            withSurface(fake, target) { _, surface ->
                val configuration = surface.configuration.value!!
                configuration.format shouldBe FakeSurfaceHandle.DEFAULT_CAPABILITIES.preferredFormat
                configuration.alphaMode shouldBe FakeSurfaceHandle.DEFAULT_CAPABILITIES.alphaModes.first()
                configuration.presentMode shouldBe FakeSurfaceHandle.DEFAULT_CAPABILITIES.presentModes.first()
            }
        }
    }

    "acquireFrame keeps the frame open until releaseFrame" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val acquired = acquiredFrame()
            withSurface(fake, target) { _, surface ->
                val handle = fake.createdHandles.single()
                handle.frames.send(acquired)

                val frame = surface.acquireFrame()
                frame.extent shouldBe acquired.extent
                // Nothing is presented or released while the caller still holds the frame.
                handle.recordedPresents shouldContainExactly emptyList()
                handle.recordedReleases shouldContainExactly emptyList()

                frame.present()
                handle.recordedPresents shouldContainExactly listOf(acquired)

                surface.releaseFrame(frame)
                handle.recordedReleases shouldContainExactly listOf(acquired)
            }
        }
    }

    "releaseFrame is idempotent and a released frame cannot be presented" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val acquired = acquiredFrame()
            withSurface(fake, target) { _, surface ->
                val handle = fake.createdHandles.single()
                handle.frames.send(acquired)

                val frame = surface.acquireFrame()
                surface.releaseFrame(frame)
                surface.releaseFrame(frame)
                handle.recordedReleases shouldContainExactly listOf(acquired)

                val rejected = shouldThrow<IllegalStateException> { frame.present() }
                rejected.message shouldContain "already released"
                handle.recordedPresents shouldContainExactly emptyList()
            }
        }
    }

    "a frame released without presenting is not shown" {
        runTest {
            val fake = RecordingFakeExtension()
            val target = FakeSurfaceTarget(drawableState())
            val acquired = acquiredFrame()
            withSurface(fake, target) { _, surface ->
                val handle = fake.createdHandles.single()
                handle.frames.send(acquired)

                surface.releaseFrame(surface.acquireFrame())

                handle.recordedPresents shouldContainExactly emptyList()
                handle.recordedReleases shouldContainExactly listOf(acquired)
            }
        }
    }
})
