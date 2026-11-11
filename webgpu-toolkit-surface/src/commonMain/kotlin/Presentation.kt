package org.graphiks.webgpu.toolkit.surface

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.withSession
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * One frame acquired from a [ToolkitSurface]: the [view] to render the frame into, its physical
 * [extent], and [present] to show it. A frame is presented exactly once: a second [present]
 * fails, and a frame whose block returns normally is presented by the surface itself unless the
 * block already presented it.
 */
interface Frame {
    /**
     * View of the swap chain texture to render the frame into.
     */
    val view: GPUTextureView

    /**
     * Physical size of [view], matching the target extent at acquisition time.
     */
    val extent: Extent2D

    /**
     * Shows the frame on the target now; a second call fails with [IllegalStateException].
     * Presenting explicitly is optional: [ToolkitSurface.withFrame] presents a frame whose
     * block returned normally without presenting it.
     */
    fun present()
}

/**
 * A presentation surface bound to one retained target lease: it publishes the
 * [PresentationConfiguration] it was negotiated with and hands out frames through [withFrame],
 * which suspends while the target cannot be drawn to.
 */
interface ToolkitSurface : AutoCloseable {
    /**
     * Hot flow of the configuration the surface was bound with, or `null` while the surface is
     * being negotiated.
     */
    val configuration: StateFlow<PresentationConfiguration?>

    /**
     * Acquires the next frame and runs [block] with it. Acquisition suspends while the target is
     * unavailable or its extent is zero. On a normal block return the frame is presented unless
     * the block already presented it, then released; on any throw, including cancellation, the
     * frame is released without being presented. Using the surface after its target moved to a
     * new generation, or after [close], fails fast.
     */
    suspend fun <R> withFrame(block: (Frame) -> R): R
}

/**
 * Opens a session with [extension] through the toolkit core, drives the target-lease protocol
 * against [target] — retaining a lease, rolling back stale generations, requesting a
 * target-bound adapter and negotiating the surface configuration — then runs [block] with the
 * session and the resulting [ToolkitSurface]. The surface is released and the session shut down
 * on every exit path, including cancellation of the caller.
 *
 * The surface negotiates on its own [ToolkitInstance] opened from the same extension: the core
 * session keeps its instance private, so the surface protocol opens a dedicated second one.
 * This is an explicit, deliberate two-instance design — the web extension's instances are
 * stateless, so the second instance costs nothing there — and consolidating to one instance
 * per session is deferred to the wgpu4k iteration (spec D7).
 */
suspend fun <R> withSurface(
    extension: WebGpuToolkitExtension,
    target: SurfaceTarget,
    hints: AdapterHints = AdapterHints(),
    block: suspend (Session, ToolkitSurface) -> R,
): R = withSession(extension, hints) { session ->
    val surface = DefaultToolkitSurface.acquire(extension, session, target, hints, coroutineContext)
    try {
        block(session, surface)
    } finally {
        withContext(NonCancellable) { surface.release() }
    }
}

/**
 * The only [ToolkitSurface] implementation: it owns the surface-scoped [ToolkitInstance] the
 * target-bound adapter and surface came from, the retained [TargetLease], and the
 * [ToolkitSurfaceHandle] presenting to it.
 *
 * The core session keeps its own instance private, so the surface protocol opens a dedicated
 * one from the same extension and releases it with the surface.
 */
internal class DefaultToolkitSurface(
    private val target: SurfaceTarget,
    private val lease: TargetLease,
    private val instance: ToolkitInstance,
    private val handle: ToolkitSurfaceHandle,
    configuration: PresentationConfiguration,
    callerContext: CoroutineContext,
) : ToolkitSurface {
    private val scope = CoroutineScope(
        SupervisorJob() + (callerContext[ContinuationInterceptor] ?: Dispatchers.Default),
    )
    private val _configuration = MutableStateFlow<PresentationConfiguration?>(configuration)
    override val configuration: StateFlow<PresentationConfiguration?> = _configuration
    private val releaseMutex = Mutex()
    private var released = false

    override suspend fun <R> withFrame(block: (Frame) -> R): R {
        check(!released) { "toolkit surface is closed" }
        checkGeneration(target.state.value)
        // Wait until the target can be drawn to: available with a non-zero extent.
        val drawable = target.state.first { it.available && it.extent.width != 0u && it.extent.height != 0u }
        checkGeneration(drawable)
        val acquired = handle.acquireFrame()
        val frame = DefaultFrame(acquired, handle)
        try {
            val result = block(frame)
            if (!frame.consumed) frame.present()
            return result
        } finally {
            handle.releaseFrame(acquired)
        }
    }

    /**
     * Fails fast once the target moved past the generation this surface is leased against; the
     * surface is dead from then on, as target replacement only arrives in v2.
     */
    private fun checkGeneration(state: TargetState) {
        check(state.generation == lease.generation) {
            "surface target moved to generation ${state.generation} while the surface holds " +
                "generation ${lease.generation}; the surface is lost"
        }
    }

    /**
     * Releases the surface: unconfigures the handle, retires the lease and closes the
     * surface-scoped instance. Idempotent; the first call wins.
     */
    suspend fun release() {
        releaseMutex.withLock {
            if (released) return
            released = true
        }
        withContext(NonCancellable) {
            runCatching { handle.close() }
            runCatching { target.retire(lease) }
            runCatching { instance.close() }
        }
        scope.cancel()
    }

    /**
     * [AutoCloseable] bridge for callers outside [withSurface]: schedules the suspending
     * [release] on the surface scope.
     */
    override fun close() {
        scope.launch { release() }
    }

    companion object {
        /**
         * Runs the target-lease protocol: retain a lease, roll leases of stale generations back
         * with [SurfaceTarget.retire] and retry, then request an adapter bound to the retained
         * target, create its surface and negotiate the configuration from the capabilities and
         * the session's device. Every failure closes any handle already created, retires the
         * lease held at that point and releases the surface-scoped instance.
         */
        suspend fun acquire(
            extension: WebGpuToolkitExtension,
            session: Session,
            target: SurfaceTarget,
            hints: AdapterHints,
            callerContext: CoroutineContext,
        ): DefaultToolkitSurface {
            val instance = extension.open()
            var lease: TargetLease? = null
            var handle: ToolkitSurfaceHandle? = null
            try {
                while (true) {
                    val retained = target.retain()
                    lease = retained
                    val state = target.state.value
                    if (retained.generation != state.generation) {
                        target.retire(retained)
                        lease = null
                        continue
                    }
                    val adapter = instance.requestAdapter(forTarget = retained, hints = hints).getOrElse { error ->
                        throw IllegalStateException("no adapter matched the retained surface target", error)
                    }
                    val created = instance.createSurface(adapter, retained)
                    handle = created
                    val configuration = PresentationConfiguration(
                        device = session.device,
                        format = created.capabilities.preferredFormat,
                    )
                    created.configure(configuration)
                    lease = null // ownership moves to the surface
                    handle = null
                    return DefaultToolkitSurface(target, retained, instance, created, configuration, callerContext)
                }
            } catch (error: Throwable) {
                withContext(NonCancellable) {
                    handle?.let { runCatching { it.close() } }
                    lease?.let { runCatching { target.retire(it) } }
                    runCatching { instance.close() }
                }
                throw error
            }
        }
    }
}

/**
 * The only [Frame] implementation: wraps one [AcquiredFrame] and enforces present-once through
 * [consumed].
 */
internal class DefaultFrame(
    private val acquired: AcquiredFrame,
    private val handle: ToolkitSurfaceHandle,
) : Frame {
    /**
     * Whether [present] already showed this frame; read by [DefaultToolkitSurface.withFrame] to
     * present a normally returned frame exactly once.
     */
    var consumed = false
        private set

    override val view: GPUTextureView
        get() = acquired.view

    override val extent: Extent2D
        get() = acquired.extent

    override fun present() {
        check(!consumed) { "frame was already presented" }
        consumed = true
        handle.present(acquired)
    }
}
