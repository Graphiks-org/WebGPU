import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUTextureView
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
 * [WebGpuToolkitExtension] wrapping a [FakeExtension] whose opened instances record every
 * [ToolkitInstance.createSurface] call, so tests can reach the [ToolkitSurfaceHandle] a surface
 * was bound to and the leases it was built with.
 */
class RecordingFakeExtension : WebGpuToolkitExtension {
    private val delegate = FakeExtension()

    /** Progression reported by instances this extension opens; forwards to the wrapped fake. */
    var progression: ToolkitProgression
        get() = delegate.progression
        set(value) {
            delegate.progression = value
        }

    /** Handles created by [ToolkitInstance.createSurface], in order. */
    val createdHandles = mutableListOf<FakeSurfaceHandle>()

    /** Leases passed to [ToolkitInstance.createSurface], in order. */
    val createSurfaceLeases = mutableListOf<TargetLease>()

    override val id: String
        get() = delegate.id

    override fun open(config: ToolkitConfig): ToolkitInstance {
        val wrapped = delegate.open(config)
        return object : ToolkitInstance by wrapped {
            override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle {
                createSurfaceLeases += lease
                val handle = wrapped.createSurface(adapter, lease) as FakeSurfaceHandle
                createdHandles += handle
                return handle
            }
        }
    }
}
