package org.graphiks.webgpu.toolkit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.GPUQueue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/**
 * Lifecycle state of a [ToolkitSession], published through [ToolkitSession.state] as the session
 * progresses: `Ready` once the device is live, then `ShuttingDown` while handles are released,
 * ending in `Closed` on success. `ShutdownTimedOut` marks a bounded shutdown that did not finish
 * in time (its cleanup keeps running and a retried shutdown can still reach `Closed`);
 * `Lost` and `Failed` record a lost device or a failed block while work was admitted.
 */
sealed interface ToolkitSessionState {
    /**
     * The session holds a live device and admits work.
     */
    data object Ready : ToolkitSessionState

    /**
     * Shutdown has started and its cleanup is still bounded by the configured timeout.
     */
    data object ShuttingDown : ToolkitSessionState

    /**
     * Shutdown exceeded the configured timeout; handles may still be closing in the background.
     */
    data object ShutdownTimedOut : ToolkitSessionState

    /**
     * The device was lost while work was admitted; admitted work was cancelled.
     */
    data object Lost : ToolkitSessionState

    /**
     * The admitted block failed, or startup could not complete; admitted work was cancelled.
     */
    data object Failed : ToolkitSessionState

    /**
     * Every handle is released; the session accepts no further work.
     */
    data object Closed : ToolkitSessionState
}

/**
 * One admitted WebGPU session: a borrowed [device] and [queue] that callers never close, the
 * hot [state] flow of its lifecycle, loss observation, scoped work admission through [run] and
 * an idempotent, retryable [shutdown].
 */
interface ToolkitSession {
    /**
     * The session's device; borrowed from the session, never closed by callers.
     */
    val device: GPUDevice

    /**
     * The primary queue of [device]; borrowed from the session, never closed by callers.
     */
    val queue: GPUQueue

    /**
     * Hot flow of the latest [ToolkitSessionState].
     */
    val state: StateFlow<ToolkitSessionState>

    /**
     * Suspends until the device is lost and returns why, or `null` when the session ended
     * without an observed loss.
     */
    suspend fun awaitLost(): GPUDeviceLostInfo?

    /**
     * Runs [block] admitted as this session's work: device loss racing the block cancels it and
     * rethrows as [DeviceLostException], a failing block publishes `Failed`, and normal
     * completion returns the block's result.
     */
    suspend fun <R> run(block: suspend (ToolkitSession) -> R): R

    /**
     * Releases every session handle within the configured shutdown timeout: the device is
     * closed, its loss notification awaited, and the instance released. Idempotent: calls after
     * a finished shutdown are no-ops. A shutdown that exceeds the timeout publishes
     * `ShutdownTimedOut` while cleanup keeps retrying in the background, and a later call
     * retries the bounded release.
     */
    suspend fun shutdown()
}

/**
 * Cancels admitted session work after device loss; carries the loss the device reported.
 */
class DeviceLostException(
    /**
     * The loss reported by the device.
     */
    val info: GPUDeviceLostInfo,
) : CancellationException("WebGPU device was lost: ${info.message}")

/**
 * Opens a session with [extension] under the default [ToolkitConfig], runs [block] with it and
 * shuts the session down on every exit path, including cancellation of the caller.
 */
suspend fun <R> withSession(
    extension: WebGpuToolkitExtension,
    hints: AdapterHints = AdapterHints(),
    block: suspend (ToolkitSession) -> R,
): R = withSession(extension, ToolkitConfig(), hints, block)

/**
 * Opens a session with [extension] under [config], runs [block] with it and shuts the session
 * down on every exit path, including cancellation of the caller.
 *
 * Startup is bounded by [ToolkitConfig.startupTimeout] and maps every failure to
 * [ToolkitSessionState.Failed] before rethrowing; `Manual` progression is rejected fast with an
 * [UnsupportedOperationException] because v1 supports only `Pumped` and `EventLoop`.
 */
suspend fun <R> withSession(
    extension: WebGpuToolkitExtension,
    config: ToolkitConfig,
    hints: AdapterHints = AdapterHints(),
    block: suspend (ToolkitSession) -> R,
): R {
    val session = DefaultToolkitSession(extension, config, hints, coroutineContext)
    try {
        session.start()
        return session.run(block)
    } finally {
        withContext(NonCancellable) { session.shutdown() }
    }
}

/**
 * The only [ToolkitSession] implementation: opens the extension's instance, requests the
 * adapter and device, drives the pump loop for `Pumped` progression, admits blocks against the
 * device-loss race, and releases handles through a timeout-bounded, retryable teardown.
 */
internal class DefaultToolkitSession(
    private val extension: WebGpuToolkitExtension,
    private val config: ToolkitConfig,
    private val hints: AdapterHints,
    callerContext: CoroutineContext,
) : ToolkitSession {
    private val scope = CoroutineScope(
        SupervisorJob() + (callerContext[ContinuationInterceptor] ?: Dispatchers.Default),
    )
    private val _state = MutableStateFlow<ToolkitSessionState>(ToolkitSessionState.Ready)
    override val state: StateFlow<ToolkitSessionState> = _state
    private val lostInfo = CompletableDeferred<GPUDeviceLostInfo?>()
    private val releaseMutex = Mutex()
    private var handlesReleased = false

    private var instance: ToolkitInstance? = null
    private var startedDevice: GPUDevice? = null
    private var pumpJob: Job? = null
    private var retryJob: Job? = null

    override val device: GPUDevice
        get() = checkNotNull(startedDevice) { "session is not started" }

    override val queue: GPUQueue
        get() = device.queue

    override suspend fun awaitLost(): GPUDeviceLostInfo? = lostInfo.await()

    /**
     * Opens the instance and requests the adapter and device, bounded by the startup timeout;
     * every failure maps to `Failed` and still releases what was opened.
     */
    suspend fun start() {
        try {
            withTimeout(config.startupTimeout) { startup() }
        } catch (e: UnsupportedOperationException) {
            _state.value = ToolkitSessionState.Failed
            throw e
        } catch (e: TimeoutCancellationException) {
            _state.value = ToolkitSessionState.Failed
            failFastClose()
            throw IllegalStateException("toolkit session startup timed out after ${config.startupTimeout}", e)
        } catch (e: CancellationException) {
            _state.value = ToolkitSessionState.Failed
            failFastClose()
            throw e
        } catch (e: Throwable) {
            _state.value = ToolkitSessionState.Failed
            failFastClose()
            throw IllegalStateException("toolkit session startup failed", e)
        }
    }

    private suspend fun startup() {
        val opened = extension.open(config)
        instance = opened
        if (opened.progression == ToolkitProgression.Manual) {
            _state.value = ToolkitSessionState.Failed
            failFastClose()
            throw UnsupportedOperationException(
                "Manual progression is not supported in v1; use Pumped or EventLoop progression",
            )
        }
        val adapter = opened.requestAdapter(hints).getOrElse { error ->
            throw IllegalStateException("no adapter matched the toolkit session hints", error)
        }
        val started = adapter.requestDevice().getOrElse { error ->
            throw IllegalStateException("requesting a device for the toolkit session failed", error)
        }
        startedDevice = started

        // Losses caused by our own close carry reason Destroyed and are not observed as losses.
        scope.launch {
            val info = started.awaitLost().getOrNull() ?: return@launch
            if (info.reason == GPUDeviceLostReason.Destroyed) {
                lostInfo.complete(null)
            } else {
                lostInfo.complete(info)
            }
        }

        if (opened.progression == ToolkitProgression.Pumped) {
            pumpJob = scope.launch {
                while (isActive) {
                    opened.pumpEvents()
                    yield()
                }
            }
        }
    }

    override suspend fun <R> run(block: suspend (ToolkitSession) -> R): R = coroutineScope {
        val admitted = this
        val lossWatch = launch {
            device.awaitLost().onSuccess { info ->
                if (info.reason != GPUDeviceLostReason.Destroyed) {
                    _state.value = ToolkitSessionState.Lost
                    admitted.cancel(DeviceLostException(info))
                }
            }
        }
        try {
            block(this@DefaultToolkitSession)
        } catch (e: Throwable) {
            if (e !is CancellationException) _state.value = ToolkitSessionState.Failed
            throw e
        } finally {
            lossWatch.cancel()
        }
    }

    override suspend fun shutdown() {
        when (_state.value) {
            ToolkitSessionState.Closed, ToolkitSessionState.ShuttingDown -> return
            else -> {}
        }
        _state.value = ToolkitSessionState.ShuttingDown
        pumpJob?.cancelAndJoin()
        try {
            withTimeout(config.shutdownTimeout) { releaseHandles() }
            _state.value = ToolkitSessionState.Closed
            dispose()
        } catch (e: TimeoutCancellationException) {
            _state.value = ToolkitSessionState.ShutdownTimedOut
            retryReleaseInBackground()
        }
    }

    /**
     * Releases the device and the instance, at most once: closing the device triggers its loss
     * notification, which is awaited so the shutdown timeout bounds real asynchronous teardown
     * before the instance itself is released.
     */
    private suspend fun releaseHandles() {
        releaseMutex.withLock {
            if (handlesReleased) return
            val deviceToClose = startedDevice
            val opened = instance
            if (deviceToClose != null) {
                runCatching { deviceToClose.close() }
                // Suspend until teardown is notified; must NOT be wrapped in runCatching,
                // so shutdown-timeout cancellation interrupts the release here.
                deviceToClose.awaitLost()
            }
            runCatching { opened?.close() }
            handlesReleased = true
        }
    }

    /**
     * Best-effort bounded release used on startup-failure paths, where no handles were
     * published; if it still times out, the background retry takes over.
     */
    private suspend fun failFastClose() {
        withContext(NonCancellable) {
            try {
                withTimeout(config.shutdownTimeout) { releaseHandles() }
            } catch (e: TimeoutCancellationException) {
                retryReleaseInBackground()
            }
        }
    }

    /**
     * Keeps retrying the bounded release until it lands, then finishes the shutdown.
     */
    private fun retryReleaseInBackground() {
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            while (true) {
                try {
                    withTimeout(config.shutdownTimeout) { releaseHandles() }
                    break
                } catch (e: TimeoutCancellationException) {
                    // Keep retrying; the retried release is what a later shutdown() awaits.
                }
            }
            if (_state.compareAndSet(ToolkitSessionState.ShutdownTimedOut, ToolkitSessionState.Closed)) {
                dispose()
            }
        }
    }

    /**
     * Releases the session scope once every handle is closed.
     */
    private fun dispose() {
        lostInfo.complete(null)
        scope.cancel()
    }
}
