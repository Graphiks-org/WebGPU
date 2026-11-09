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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lifecycle state of a [Session], published through [Session.state] as the session
 * progresses: `Ready` once the device is live, then `ShuttingDown` while handles are released,
 * ending in `Closed` on success. `ShutdownTimedOut` marks a bounded shutdown that did not finish
 * in time (a finite number of background retries follows, and a retried shutdown can still reach
 * `Closed`); `Lost` and `Failed` record a lost device or a failed block while work was admitted.
 */
sealed interface SessionState {
    /**
     * The session holds a live device and admits work.
     */
    data object Ready : SessionState

    /**
     * Shutdown has started and its cleanup is still bounded by the configured timeout.
     */
    data object ShuttingDown : SessionState

    /**
     * Shutdown exceeded the configured timeout; a finite number of background retries follows,
     * and a retried [Session.shutdown] can still finish the release.
     */
    data object ShutdownTimedOut : SessionState

    /**
     * The device was lost while work was admitted; admitted work was cancelled.
     */
    data object Lost : SessionState

    /**
     * The admitted block failed, or startup could not complete; admitted work was cancelled.
     */
    data object Failed : SessionState

    /**
     * Every handle is released; the session accepts no further work.
     */
    data object Closed : SessionState
}

/**
 * One admitted WebGPU session: a borrowed [device] and [queue] that callers never close, the
 * hot [state] flow of its lifecycle, loss observation, scoped work admission through [run] and
 * an idempotent, retryable [shutdown].
 */
interface Session {
    /**
     * The session's device; borrowed from the session, never closed by callers. Reading it after
     * the session reached `Closed` fails fast.
     */
    val device: GPUDevice

    /**
     * The primary queue of [device]; borrowed from the session, never closed by callers.
     * Reading it after the session reached `Closed` fails fast.
     */
    val queue: GPUQueue

    /**
     * Hot flow of the latest [SessionState].
     */
    val state: StateFlow<SessionState>

    /**
     * Suspends until the device is lost and returns why, or `null` when the session ended
     * without an observed loss.
     */
    suspend fun awaitLost(): GPUDeviceLostInfo?

    /**
     * Runs [block] admitted as this session's work while the session is `Ready`; admissions in
     * any other state are rejected. Device loss racing the block cancels it and rethrows as
     * [DeviceLostException], a failing block publishes `Failed` (only from `Ready`), and normal
     * completion returns the block's result. An already-admitted block keeps running through its
     * own [shutdown] call.
     */
    suspend fun <R> run(block: suspend (Session) -> R): R

    /**
     * Releases every session handle within the configured shutdown timeout: the device is
     * closed, its loss notification awaited, and the instance released. Idempotent: calls after
     * a finished shutdown are no-ops. A shutdown that exceeds the timeout publishes
     * `ShutdownTimedOut` while cleanup is retried a finite number of times in the background
     * with exponential backoff; once those retries are exhausted the session stays in
     * `ShutdownTimedOut` and a later call retries the bounded release.
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
 * Opens a session with [extension] under the default [SessionConfig], runs [block] with it and
 * shuts the session down on every exit path, including cancellation of the caller.
 */
suspend fun <R> withSession(
    extension: WebGpuToolkitExtension,
    hints: AdapterHints = AdapterHints(),
    block: suspend (Session) -> R,
): R = withSession(extension, SessionConfig(), hints, block)

/**
 * Opens a session with [extension] under [config], runs [block] with it and shuts the session
 * down on every exit path, including cancellation of the caller.
 *
 * Startup is bounded by [SessionConfig.startupTimeout] and maps every failure to
 * [SessionState.Failed] before rethrowing; `Manual` progression is rejected fast with an
 * [UnsupportedOperationException] because v1 supports only `Pumped` and `EventLoop`.
 */
suspend fun <R> withSession(
    extension: WebGpuToolkitExtension,
    config: SessionConfig,
    hints: AdapterHints = AdapterHints(),
    block: suspend (Session) -> R,
): R {
    val session = DefaultSession(extension, config, hints, coroutineContext)
    try {
        session.start()
        return session.run(block)
    } finally {
        withContext(NonCancellable) { session.shutdown() }
    }
}

/**
 * The only [Session] implementation: opens the extension's instance, requests the
 * adapter and device, drives the pump loop for `Pumped` progression, admits blocks against the
 * device-loss race, and releases handles through a timeout-bounded, retryable teardown.
 */
internal class DefaultSession(
    private val extension: WebGpuToolkitExtension,
    private val config: SessionConfig,
    private val hints: AdapterHints,
    callerContext: CoroutineContext,
) : Session {
    private val scope = CoroutineScope(
        SupervisorJob() + (callerContext[ContinuationInterceptor] ?: Dispatchers.Default),
    )
    private val _state = MutableStateFlow<SessionState>(SessionState.Ready)
    override val state: StateFlow<SessionState> = _state
    private val lostInfo = CompletableDeferred<GPUDeviceLostInfo?>()
    private val releaseMutex = Mutex()
    private var handlesReleased = false

    private var instance: ToolkitInstance? = null
    private var startedDevice: GPUDevice? = null
    private var pumpJob: Job? = null
    private var retryJob: Job? = null

    private companion object {
        /** Background release attempts before the loop gives up and leaves the session retryable. */
        private const val BACKGROUND_RETRY_ATTEMPTS = 5

        /** Wait before the first background retry; later retries double it, capped at the shutdown timeout. */
        private val BACKGROUND_RETRY_FIRST_DELAY = 100.milliseconds
    }

    override val device: GPUDevice
        get() {
            check(_state.value != SessionState.Closed) { "toolkit session is closed" }
            return checkNotNull(startedDevice) { "session is not started" }
        }

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
            _state.value = SessionState.Failed
            throw e
        } catch (e: TimeoutCancellationException) {
            _state.value = SessionState.Failed
            failFastClose()
            throw IllegalStateException("toolkit session startup timed out after ${config.startupTimeout}", e)
        } catch (e: CancellationException) {
            _state.value = SessionState.Failed
            failFastClose()
            throw e
        } catch (e: Throwable) {
            _state.value = SessionState.Failed
            failFastClose()
            throw IllegalStateException("toolkit session startup failed", e)
        }
    }

    private suspend fun startup() {
        val opened = extension.open(config)
        instance = opened
        if (opened.progression == Progression.Manual) {
            _state.value = SessionState.Failed
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

        if (opened.progression == Progression.Pumped) {
            pumpJob = scope.launch {
                while (isActive) {
                    opened.pumpEvents()
                    yield()
                }
            }
        }
    }

    override suspend fun <R> run(block: suspend (Session) -> R): R {
        check(_state.value == SessionState.Ready) {
            "toolkit session is ${_state.value} and admits no further work"
        }
        return coroutineScope {
            val admitted = this
            val lossWatch = launch {
                device.awaitLost().onSuccess { info ->
                    if (info.reason != GPUDeviceLostReason.Destroyed &&
                        _state.value == SessionState.Ready
                    ) {
                        _state.value = SessionState.Lost
                        admitted.cancel(DeviceLostException(info))
                    }
                }
            }
            try {
                block(this@DefaultSession)
            } catch (e: Throwable) {
                if (e !is CancellationException && _state.value == SessionState.Ready) {
                    _state.value = SessionState.Failed
                }
                throw e
            } finally {
                lossWatch.cancel()
            }
        }
    }

    override suspend fun shutdown() {
        when (_state.value) {
            SessionState.Closed, SessionState.ShuttingDown -> return
            else -> {}
        }
        _state.value = SessionState.ShuttingDown
        // Cancel eagerly, but join INSIDE the timed release: a pump blocked in a native
        // pumpEvents() never observes the cancellation, and an unbounded join would hang the
        // shutdown forever instead of publishing ShutdownTimedOut.
        val pump = pumpJob
        pump?.cancel()
        try {
            withTimeout(config.shutdownTimeout) {
                pump?.join()
                releaseHandles()
            }
            _state.value = SessionState.Closed
            dispose()
        } catch (e: TimeoutCancellationException) {
            _state.value = SessionState.ShutdownTimedOut
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
     * Retries the bounded release in the background a finite number of times with exponential
     * backoff: the first retry waits [BACKGROUND_RETRY_FIRST_DELAY] (100ms), every later retry
     * doubles that delay, capped at the configured shutdown timeout. Once the
     * [BACKGROUND_RETRY_ATTEMPTS] attempts are exhausted, the loop gives up: the session STAYS
     * in `ShutdownTimedOut` — a caller may still retry [shutdown] manually — and the referenced
     * handles are never freed by the exhausted loop.
     */
    private fun retryReleaseInBackground() {
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            var backoff = BACKGROUND_RETRY_FIRST_DELAY.coerceAtMost(config.shutdownTimeout)
            repeat(BACKGROUND_RETRY_ATTEMPTS) {
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(config.shutdownTimeout)
                try {
                    withTimeout(config.shutdownTimeout) { releaseHandles() }
                    if (_state.compareAndSet(SessionState.ShutdownTimedOut, SessionState.Closed)) {
                        dispose()
                    }
                    return@launch
                } catch (e: TimeoutCancellationException) {
                    // Retry until the attempts are exhausted, then leave the session retryable.
                }
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
