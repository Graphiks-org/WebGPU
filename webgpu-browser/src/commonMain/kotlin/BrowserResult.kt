package org.graphiks.webgpu.browser

internal suspend fun <T> browserResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: kotlinx.coroutines.CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
