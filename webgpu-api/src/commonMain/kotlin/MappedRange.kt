package org.graphiks.webgpu

/**
 * Maps this buffer, runs [block] with the mapped range and unmaps the buffer afterwards, even
 * when [block] throws.
 *
 * The helper starts a new mapping on a buffer that is currently unmapped; it does not take over
 * a mapping that is already in progress. Because the `try`/`finally` only starts after
 * `mapAsync` succeeded, a failed mapping request never unmaps a pre-existing mapping.
 *
 * The range passed to [block] is borrowed: it is invalidated by the `unmap()` this helper
 * performs on exit, and by destroying the buffer. [block] is not suspending and must neither
 * retain nor return the borrowed view; this restriction is not enforceable through the generic
 * type parameter.
 *
 * @param mode The mapping mode, [GPUMapMode.Read] or [GPUMapMode.Write].
 * @param offset The offset in bytes of the range to map.
 * @param size The size in bytes of the range to map, or `null` for the rest of the buffer.
 * @param block The non-suspending block to run with the borrowed mapped range.
 * @return The value returned by [block].
 */
suspend fun <T> GPUBuffer.withMappedRange(
    mode: GPUMapMode,
    offset: GPUSize64 = 0uL,
    size: GPUSize64? = null,
    block: (ArrayBuffer) -> T,
): T {
    mapAsync(mode, offset, size).getOrThrow()
    try {
        return block(getMappedRange(offset, size))
    } finally {
        unmap()
    }
}
