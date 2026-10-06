@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

private class FakeBuffer(val capacity: ULong = 16uL) : GPUBuffer {
    private val backing = ArrayBuffer.allocate(capacity)
    private var mapped = false
    var mapCalls = 0
    var unmapCalls = 0
    var failNextMap = false

    override var label: String = ""
    override val size: ULong get() = capacity
    override val usage: GPUBufferUsage get() = GPUBufferUsage.MapWrite
    override val mapState: GPUBufferMapState
        get() = if (mapped) GPUBufferMapState.Mapped else GPUBufferMapState.Unmapped

    override suspend fun mapAsync(mode: GPUMapMode, offset: ULong, size: ULong?): Result<Unit> {
        mapCalls++
        if (failNextMap) {
            failNextMap = false
            return Result.failure(IllegalStateException("mapping rejected"))
        }
        if (mapped) return Result.failure(IllegalStateException("already mapped"))
        mapped = true
        return Result.success(Unit)
    }

    override fun getMappedRange(offset: ULong, size: ULong?): ArrayBuffer = backing

    override fun unmap() {
        unmapCalls++
        mapped = false
    }

    override fun close() {}
}

class MappedRangeTest : FreeSpec({

    "withMappedRange maps, runs the block, unmaps and preserves the written content" {
        val buffer = FakeBuffer()

        buffer.withMappedRange(GPUMapMode.Write) { view ->
            view.setUInts(0uL, uintArrayOf(1u, 2u, 3u, 4u))
        }

        buffer.mapState shouldBe GPUBufferMapState.Unmapped
        buffer.unmapCalls shouldBe 1
        buffer.mapCalls shouldBe 1

        val readBack = buffer.withMappedRange(GPUMapMode.Read) { view -> view.toUIntArray().toList() }
        readBack shouldBe listOf(1u, 2u, 3u, 4u)
        buffer.unmapCalls shouldBe 2
    }

    "a throwing block still unmaps and the buffer can be remapped" {
        val buffer = FakeBuffer()

        shouldThrow<IllegalStateException> {
            buffer.withMappedRange(GPUMapMode.Write) { throw IllegalStateException("boom") }
        }

        buffer.mapState shouldBe GPUBufferMapState.Unmapped
        buffer.unmapCalls shouldBe 1

        buffer.withMappedRange(GPUMapMode.Write) { view ->
            view.setUInts(0uL, uintArrayOf(9u, 9u, 9u, 9u))
        }
        buffer.mapCalls shouldBe 2
        buffer.mapState shouldBe GPUBufferMapState.Unmapped
    }

    "a failed mapping request does not tear down a pre-existing mapping" {
        val buffer = FakeBuffer()
        buffer.mapAsync(GPUMapMode.Write).getOrThrow()

        buffer.failNextMap = true
        shouldThrow<IllegalStateException> {
            buffer.withMappedRange(GPUMapMode.Write) { }
        }

        buffer.mapState shouldBe GPUBufferMapState.Mapped
        buffer.unmapCalls shouldBe 0

        buffer.unmap()
        buffer.mapState shouldBe GPUBufferMapState.Unmapped
    }
})
