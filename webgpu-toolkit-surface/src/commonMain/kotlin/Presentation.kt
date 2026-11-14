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
import org.graphiks.webgpu.GPUDevice
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

    /**
     * Swaps the presentation target for [target] between frames.
     *
     * The surface retains the new target and rolls a stale generation back exactly like
     * [withSurface] does. The device and its configuration are kept when [target] supports the
     * surface's current format; otherwise the surface renegotiates and [configuration] emits the
     * new value. A failed replacement leaves the previous target in place and rethrows, so the
     * surface stays usable. Must not run concurrently with [withFrame].
     */
    suspend fun replaceTarget(target: SurfaceTarget)

    /**
     * Detaches the target whose lease carries [generation], releasing it and leaving the surface
     * without a target: [withFrame] then suspends until a [replaceTarget] call installs one.
     * A generation that is no longer the surface's is ignored. Must not run concurrently with
     * [withFrame].
     */
    suspend fun detachTarget(generation: Long)
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
    private val instance: ToolkitInstance,
    private val device: GPUDevice,
    private var target: SurfaceTarget?,
    private var lease: TargetLease?,
    private var handle: ToolkitSurfaceHandle?,
    configuration: PresentationConfiguration,
    callerContext: CoroutineContext,
) : ToolkitSurface {
    private val scope = CoroutineScope(
        SupervisorJob() + (callerContext[ContinuationInterceptor] ?: Dispatchers.Default),
    )
    private val _configuration = MutableStateFlow<PresentationConfiguration?>(configuration)
    override val configuration: StateFlow<PresentationConfiguration?> = _configuration
    private val releaseMutex = Mutex()
    private val swapMutex = Mutex()
    private var released = false

    override suspend fun <R> withFrame(block: (Frame) -> R): R {
        check(!released) { "toolkit surface is closed" }
        // A detached surface has no target: wait for a replacement to install one.
        val current = swapMutex.withLock {
            check(!released) { "toolkit surface is closed" }
            val installed = target
            checkNotNull(installed) { "toolkit surface has no target; call replaceTarget first" }
            installed to checkNotNull(handle) { "toolkit surface has no handle" }
        }
        val (boundTarget, boundHandle) = current
        checkGeneration(boundTarget)
        // Wait until the target can be drawn to: available with a non-zero extent.
        val drawable = boundTarget.state.first { it.available && it.extent.width != 0u && it.extent.height != 0u }
        checkGeneration(boundTarget, drawable)
        val acquired = boundHandle.acquireFrame()
        val frame = DefaultFrame(acquired, boundHandle)
        try {
            val result = block(frame)
            if (!frame.consumed) frame.present()
            return result
        } finally {
            boundHandle.releaseFrame(acquired)
        }
    }

    override suspend fun replaceTarget(target: SurfaceTarget) {
        swapMutex.withLock {
            check(!released) { "toolkit surface is closed" }
            val previousTarget = this@DefaultToolkitSurface.target
            val previousLease = lease
            val previousHandle = handle

            // Retain the replacement, rolling stale generations back like withSurface does.
            var retained: TargetLease? = null
            var created: ToolkitSurfaceHandle? = null
            try {
                while (true) {
                    val candidate = target.retain()
                    retained = candidate
                    if (candidate.generation != target.state.value.generation) {
                        target.retire(candidate)
                        retained = null
                        continue
                    }
                    val adapter = instance.requestAdapter(forTarget = candidate).getOrElse { error ->
                        throw IllegalStateException("no adapter matched the replacement surface target", error)
                    }
                    created = instance.createSurface(adapter, candidate)
                    break
                }
                val replacement = checkNotNull(created)
                val currentFormat = _configuration.value?.format
                val compatible = replacement.capabilities.formats.contains(currentFormat)
                val next = if (compatible && currentFormat != null) {
                    _configuration.value!!
                } else {
                    PresentationConfiguration(
                        device = device,
                        format = replacement.capabilities.preferredFormat,
                    )
                }
                replacement.configure(next)

                // Commit: the replacement is live, release what it replaced.
                this@DefaultToolkitSurface.target = target
                lease = retained
                handle = replacement
                retained = null
                created = null
                _configuration.value = next
            } catch (error: Throwable) {
                // A failed replacement preserves the previous target and policy.
                withContext(NonCancellable) {
                    created?.let { runCatching { it.close() } }
                    retained?.let { runCatching { target.retire(it) } }
                }
                throw error
            }

            withContext(NonCancellable) {
                previousHandle?.let { runCatching { it.close() } }
                previousLease?.let { leaseTarget -> runCatching { previousTarget?.retire(leaseTarget) } }
            }
        }
    }

    override suspend fun detachTarget(generation: Long) {
        swapMutex.withLock {
            if (released) return
            val current = lease ?: return
            if (current.generation != generation) return // stale detach requests are ignored
            val detachedTarget = target
            val detachedHandle = handle
            target = null
            lease = null
            handle = null
            withContext(NonCancellable) {
                detachedHandle?.let { runCatching { it.close() } }
                detachedTarget?.let { runCatching { it.retire(current) } }
            }
            _configuration.value = null
        }
    }

    /**
     * Fails fast once the target moved past the generation this surface is leased against.
     */
    private fun checkGeneration(boundTarget: SurfaceTarget, state: TargetState = boundTarget.state.value) {
        val bound = lease?.generation ?: return
        check(state.generation == bound) {
            "surface target moved to generation ${state.generation} while the surface holds " +
                "generation $bound; replace the target with replaceTarget"
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
            runCatching { handle?.close() }
            runCatching { lease?.let { held -> target?.retire(held) } }
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
                    return DefaultToolkitSurface(
                        instance = instance,
                        device = session.device,
                        target = target,
                        lease = retained,
                        handle = created,
                        configuration = configuration,
                        callerContext = callerContext,
                    )
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
