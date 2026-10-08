@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import js.coroutines.resumeWithError
import js.promise.Promise
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Awaits the promise and stays cancellable: cancelling the calling coroutine resumes this
 * suspension with a [kotlinx.coroutines.CancellationException] instead of waiting for the
 * promise to settle. `js.promise.await` uses a non-cancellable suspension, which would leak
 * the coroutine on cancellation.
 */
internal suspend fun <T : JsAny?> Promise<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value ->
                continuation.resume(value)
                null
            },
            onRejected = { error ->
                continuation.resumeWithError(error)
                null
            },
        )
    }

/**
 * Awaits the promise like [await], but releases the resolved value when the delivery is cancelled:
 * the value was produced by the backend, yet the calling coroutine was cancelled before it could
 * take ownership of it. [releaseUndelivered] runs exactly once for such an undelivered value and
 * never for a value that was actually delivered to the caller.
 */
internal suspend fun <T : JsAny?> Promise<T>.await(releaseUndelivered: (T) -> Unit): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value ->
                continuation.resume(value) { _, undelivered, _ ->
                    releaseUndelivered(undelivered)
                }
                null
            },
            onRejected = { error ->
                continuation.resumeWithError(error)
                null
            },
        )
    }
