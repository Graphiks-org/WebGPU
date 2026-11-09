import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.ToolkitConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.ToolkitProgression
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/**
 * [SurfaceTarget] fake with a scriptable state flow and a queue of [retain] results: send a lease
 * into [retainResults] to script a stale lease, otherwise [retain] hands out a fresh lease bound
 * to the currently published generation.
 */
class FakeSurfaceTarget(
    initial: TargetState,
) : SurfaceTarget {
    /** Scriptable state; publish new generations, extents or availability through it. */
    val mutableState = MutableStateFlow(initial)

    override val state: StateFlow<TargetState> = mutableState

    /** Leases [retain] hands out, in order; when empty, a fresh lease for the current generation. */
    val retainResults = Channel<TargetLease>(Channel.UNLIMITED)

    /** Leases handed out by [retain], in order. */
    val recordedRetains = mutableListOf<TargetLease>()

    /** Leases handed back through [retire], in order. */
    val recordedRetires = mutableListOf<TargetLease>()

    override suspend fun retain(): TargetLease {
        val lease = retainResults.tryReceive().getOrNull() ?: FakeLease(mutableState.value.generation)
        recordedRetains += lease
        return lease
    }

    override suspend fun retire(lease: TargetLease) {
        recordedRetires += lease
    }
}

/**
 * [TargetLease] fake carrying only its generation.
 */
data class FakeLease(
    override val generation: Long,
) : TargetLease

/**
 * [GPUTextureView] fake; no member carries behavior.
 */
class FakeTextureView : GPUTextureView {
    override var label: String = ""

    override fun close() = Unit
}

/**
 * [ToolkitSurfaceHandle] wrapper around a [FakeSurfaceHandle] that counts [close] calls and can
 * be scripted to fail its first [configure]; every other member forwards to the wrapped fake,
 * whose frame source and recordings stay scriptable through the forwarders.
 */
class RecordingSurfaceHandle(
    /** Wrapped fake handle; script frames through [frames] and read recordings through it. */
    private val fake: FakeSurfaceHandle,
    /** When not `null`, the first [configure] throws this error instead of recording. */
    failFirstConfigure: IllegalStateException? = null,
) : ToolkitSurfaceHandle by fake {
    /** Times [close] was called. */
    var closeCalls: Int = 0
        private set

    private var configureFailure: IllegalStateException? = failFirstConfigure

    /** Scriptable frame source of the wrapped fake. */
    val frames
        get() = fake.frames

    /** Configurations the wrapped fake recorded, in order. */
    val recordedConfigures
        get() = fake.recordedConfigures

    /** Frames the wrapped fake presented, in order. */
    val recordedPresents
        get() = fake.recordedPresents

    /** Frames the wrapped fake released, in order. */
    val recordedReleases
        get() = fake.recordedReleases

    override fun configure(configuration: PresentationConfiguration) {
        val failure = configureFailure
        if (failure != null) {
            configureFailure = null
            throw failure
        }
        fake.configure(configuration)
    }

    override fun close() {
        closeCalls++
        fake.close()
    }
}

/**
 * [WebGpuToolkitExtension] wrapping a [FakeExtension] whose opened instances record every
 * [ToolkitInstance.createSurface] call, so tests can reach the handle a surface was bound to,
 * the leases it was built with, and its close count.
 */
class RecordingFakeExtension : WebGpuToolkitExtension {
    private val delegate = FakeExtension()

    /** Progression reported by instances this extension opens; forwards to the wrapped fake. */
    var progression: ToolkitProgression
        get() = delegate.progression
        set(value) {
            delegate.progression = value
        }

    /**
     * When not `null`, the handle of the next [ToolkitInstance.createSurface] call fails its
     * first [configure] with this error; resets after that creation.
     */
    var failFirstConfigure: IllegalStateException? = null

    /** Handles created by [ToolkitInstance.createSurface], in order. */
    val createdHandles = mutableListOf<RecordingSurfaceHandle>()

    /** Leases passed to [ToolkitInstance.createSurface], in order. */
    val createSurfaceLeases = mutableListOf<TargetLease>()

    override val id: String
        get() = delegate.id

    override fun open(config: ToolkitConfig): ToolkitInstance {
        val wrapped = delegate.open(config)
        return object : ToolkitInstance by wrapped {
            override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle {
                createSurfaceLeases += lease
                val handle = RecordingSurfaceHandle(
                    fake = wrapped.createSurface(adapter, lease) as FakeSurfaceHandle,
                    failFirstConfigure = failFirstConfigure,
                )
                failFirstConfigure = null
                createdHandles += handle
                return handle
            }
        }
    }
}
