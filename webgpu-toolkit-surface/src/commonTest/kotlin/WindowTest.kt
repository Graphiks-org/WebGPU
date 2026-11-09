import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.surface.Window
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowEvent
import org.graphiks.webgpu.toolkit.surface.WindowHost
import org.graphiks.webgpu.toolkit.surface.WindowRuntime
import org.graphiks.webgpu.toolkit.surface.withSurface
import org.graphiks.webgpu.toolkit.surface.withWindow

/**
 * Drawable target state for [FakeWindow]: available, 640 by 480, of [generation].
 */
private fun drawableState(generation: Long = 1L): TargetState =
    TargetState(generation = generation, extent = Extent2D(width = 640u, height = 480u), available = true)

/**
 * One feedable frame for a [FakeSurfaceHandle.frames] channel, of [extent].
 */
private fun acquiredFrame(extent: Extent2D = Extent2D(width = 640u, height = 480u)): AcquiredFrame =
    AcquiredFrame(view = FakeTextureView(), extent = extent)

/**
 * [Window] fake with a scriptable state flow, a [MutableSharedFlow] event source, retain/retire
 * recording and a [closed] flag. [resize] models the platform resizing the window: it publishes
 * a fresh generation with the new extent and emits [WindowEvent.Resized].
 */
class FakeWindow(
    /** Runtime exclusivity this fake window reports. */
    override val runtime: WindowRuntime = WindowRuntime.Owned,
    initial: TargetState,
) : Window {
    /** Scriptable state; [resize] publishes new generations through it. */
    val mutableState = MutableStateFlow(initial)

    override val state: StateFlow<TargetState> = mutableState

    /** Scriptable event source; [resize] emits through it. */
    val eventFlow = MutableSharedFlow<WindowEvent>(replay = 8)

    override val events: Flow<WindowEvent> = eventFlow

    /** Whether [close] ran. */
    var closed: Boolean = false
        private set

    /** Leases handed out by [retain], in order. */
    val recordedRetains = mutableListOf<TargetLease>()

    /** Leases handed back through [retire], in order. */
    val recordedRetires = mutableListOf<TargetLease>()

    override suspend fun retain(): TargetLease =
        FakeLease(mutableState.value.generation).also { recordedRetains += it }

    override suspend fun retire(lease: TargetLease) {
        recordedRetires += lease
    }

    override fun close() {
        closed = true
    }

    /**
     * Models the platform resizing this window: publishes a fresh generation with [extent] on
     * the state flow and emits [WindowEvent.Resized] on [eventFlow].
     */
    suspend fun resize(extent: Extent2D) {
        mutableState.value = TargetState(
            generation = mutableState.value.generation + 1,
            extent = extent,
            available = true,
        )
        eventFlow.emit(WindowEvent.Resized(extent))
    }
}

/**
 * [WindowHost] fake: records every [open] config and hands out fresh [FakeWindow]s carrying the
 * host's runtime.
 */
class FakeWindowHost(
    /** Runtime exclusivity reported by this host and every window it opens. */
    runtime: WindowRuntime = WindowRuntime.Owned,
) : WindowHost {
    override val runtime: WindowRuntime = runtime

    /** Configs passed to [open], one entry per open call. */
    val recordedConfigs = mutableListOf<WindowConfig>()

    /** Windows handed out by [open], in order. */
    val openedWindows = mutableListOf<FakeWindow>()

    override suspend fun open(config: WindowConfig): Window =
        FakeWindow(runtime = runtime, initial = drawableState()).also {
            recordedConfigs += config
            openedWindows += it
        }
}

class WindowTest : FreeSpec({
    "withWindow opens the host window and presents through it" {
        runTest {
            val fake = RecordingFakeExtension()
            val host = FakeWindowHost()
            val acquired = acquiredFrame()
            val result = withWindow(fake, host, WindowConfig(title = "cornell")) { session, window, surface ->
                window shouldBe host.openedWindows.single()
                val handle = fake.createdHandles.single()
                handle.frames.send(acquired)
                surface.withFrame { frame ->
                    frame.view shouldBe acquired.view
                    frame.extent shouldBe acquired.extent
                }
                surface.configuration.value!!.device shouldBe session.device
                "presented"
            }
            result shouldBe "presented"
            host.recordedConfigs shouldContainExactly listOf(WindowConfig(title = "cornell"))
            val handle = fake.createdHandles.single()
            handle.recordedConfigures.single().format shouldBe
                FakeSurfaceHandle.DEFAULT_CAPABILITIES.preferredFormat
            handle.recordedPresents shouldContainExactly listOf(acquired)
        }
    }

    "resize event updates the target state" {
        runTest {
            val fake = RecordingFakeExtension()
            val host = FakeWindowHost()
            withWindow(fake, host) { _, window, _ ->
                val fakeWindow = host.openedWindows.single()
                window shouldBe fakeWindow
                fakeWindow.resize(Extent2D(width = 800u, height = 600u))
                window.state.value shouldBe TargetState(
                    generation = 2L,
                    extent = Extent2D(width = 800u, height = 600u),
                    available = true,
                )
                fakeWindow.eventFlow.replayCache shouldContainExactly
                    listOf(WindowEvent.Resized(Extent2D(width = 800u, height = 600u)))
                // The window stays a usable presentation target: a fresh lease on the new
                // generation negotiates a surface whose withFrame completes with the new extent.
                withSurface(fake, window) { _, surface ->
                    val frame = acquiredFrame(Extent2D(width = 800u, height = 600u))
                    fake.createdHandles.last().frames.send(frame)
                    surface.withFrame { it.extent shouldBe Extent2D(width = 800u, height = 600u) }
                }
            }
        }
    }

    "owned window closes on exit, borrowed window survives" {
        runTest {
            val fake = RecordingFakeExtension()
            val owned = FakeWindowHost(WindowRuntime.Owned)
            val borrowed = FakeWindowHost(WindowRuntime.Borrowed)
            withWindow(fake, owned) { _, _, _ ->
                owned.openedWindows.single().closed shouldBe false
            }
            withWindow(fake, borrowed) { _, _, _ ->
                borrowed.openedWindows.single().closed shouldBe false
            }
            owned.openedWindows.single().closed shouldBe true
            borrowed.openedWindows.single().closed shouldBe false
        }
    }

    "owned window closes when the block fails" {
        runTest {
            val fake = RecordingFakeExtension()
            val host = FakeWindowHost()
            val rejected = shouldThrow<IllegalStateException> {
                withWindow(fake, host) { _, _, _ ->
                    error("render failed")
                }
            }
            rejected.message shouldBe "render failed"
            host.openedWindows.single().closed shouldBe true
        }
    }
})
