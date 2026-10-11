package org.graphiks.webgpu.suite.acid.buffers

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A `mapAsync` request cancelled after it has really started must release the buffer: the next
 * mapping succeeds immediately, accepts a write, and the written pattern reads back.
 *
 * The first request is started with `CoroutineStart.UNDISPATCHED` so it reaches the backend
 * before the cancellation; the buffer is then `Pending`. Cancelling the request must unmap the
 * buffer synchronously, otherwise the immediate second `mapAsync` is rejected by WebGPU with
 * "Buffer already has an outstanding map pending".
 */
@AcidTest(
    id = AcidCaseId.BuffersMapCancelRemap,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.GPUBuffer_unmap,
        ApiSymbols.GPUBuffer_mapState,
        ApiSymbols.GPUBufferUsage_MapWrite,
        ApiSymbols.GPUBufferUsage_CopySrc,
    ],
)
suspend fun mapCancelRemap(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc),
    ).use { buffer ->
        coroutineScope {
            // Start the request undispatched so it reaches the backend before the cancellation:
            // the buffer is Pending when the coroutine is cancelled.
            val cancelled = async(start = CoroutineStart.UNDISPATCHED) {
                buffer.mapAsync(GPUMapMode.Write)
            }
            assertEquals(GPUBufferMapState.Pending, buffer.mapState, "The mapping request must have started")
            cancelled.cancelAndJoin()
            assertTrue(cancelled.isCancelled, "The mapping request must be cancelled")
            assertEquals(
                GPUBufferMapState.Unmapped,
                buffer.mapState,
                "Cancelling a started mapping must release the buffer immediately",
            )
        }

        // The immediate remap must not be rejected by the backend: the buffer is free.
        buffer.mapAsync(GPUMapMode.Write).getOrThrow()
        try {
            assertEquals(GPUBufferMapState.Mapped, buffer.mapState)
            buffer.getMappedRange().setUInts(0uL, uintArrayOf(5u, 6u, 7u, 8u))
        } finally {
            buffer.unmap()
        }
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)

        val bytes = readBufferBytes(device, buffer, 16uL)
        assertEquals(5, bytes[0].toInt() and 255)
        assertEquals(6, bytes[4].toInt() and 255)
        assertEquals(7, bytes[8].toInt() and 255)
        assertEquals(8, bytes[12].toInt() and 255)
    }
}
