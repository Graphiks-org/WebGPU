package org.graphiks.webgpu.benchmarks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WriterPrototypeTest {

    @Test
    fun rgbaLayoutTouchesOnlyPixels() {
        assertEquals(0L, rgbaTouchedBytes(0, 4, 16))
        assertEquals(0L, rgbaTouchedBytes(4, 0, 16))
        assertEquals(4L, rgbaTouchedBytes(1, 1, 4))
        assertEquals(64L, rgbaTouchedBytes(4, 4, 16))
        assertEquals(88L, rgbaTouchedBytes(4, 4, 24))
        assertFailsWith<IllegalArgumentException> { rgbaTouchedBytes(4, 4, 8) }
        assertFailsWith<IllegalArgumentException> { rgbaTouchedBytes(-1, 4, 16) }
    }

    @Test
    fun vertexLayoutIsThirtyTwoBytesPerVertex() {
        assertEquals(0L, vertexTouchedBytes(0))
        assertEquals(32L, vertexTouchedBytes(1))
        assertEquals(128L, vertexTouchedBytes(4))
        assertFailsWith<IllegalArgumentException> { vertexTouchedBytes(-1) }
    }

    @Test
    fun rgbaPrototypeWritesOnlyTheTouchedBytes() {
        withMemory(128) { memory ->
            fillByte(memory, 0x5a)
            fillRgbaPrototype(memory, base = 8, width = 4, height = 4, rowStride = 24, seed = 1)
            val bytes = memory.buffer.toByteArray()
            for (i in 0 until 8) assertEquals(0x5a.toByte(), bytes[i], "prefix byte $i")
            for (i in 96 until 128) assertEquals(0x5a.toByte(), bytes[i], "suffix byte $i")
            // Row padding between rows stays untouched.
            for (row in 0 until 3) {
                for (offset in 16 until 24) {
                    assertEquals(0x5a.toByte(), bytes[8 + row * 24 + offset], "padding row $row")
                }
            }
            for (y in 0 until 4) {
                for (x in 0 until 4) {
                    val at = 8 + y * 24 + x * 4
                    assertEquals(((x + 1) and 255).toByte(), bytes[at])
                    assertEquals(((y + 1) and 255).toByte(), bytes[at + 1])
                    assertEquals(((x xor y) and 255).toByte(), bytes[at + 2])
                    assertEquals(0xFF.toByte(), bytes[at + 3])
                }
            }
        }
    }

    @Test
    fun vertexPrototypeWritesOnlyTheTouchedBytes() {
        withMemory(256) { memory ->
            fillByte(memory, 0x33)
            fillVerticesPrototype(memory, base = 16, count = 4, seed = 9)
            val bytes = memory.buffer.toByteArray()
            for (i in 0 until 16) assertEquals(0x33.toByte(), bytes[i], "prefix byte $i")
            for (i in 144 until 256) assertEquals(0x33.toByte(), bytes[i], "suffix byte $i")
            for (vertex in 0 until 4) {
                for (component in 0 until VERTEX_FLOATS) {
                    assertEquals(
                        vertexFloat(vertex, component, 9),
                        memory.buffer.getFloat((16 + vertex * VERTEX_BYTES + component * 4).toULong()),
                    )
                }
            }
        }
    }

    @Test
    fun rgbaPrototypeRejectsAnOutOfRangeLayout() {
        withMemory(64) { memory ->
            assertFailsWith<IndexOutOfBoundsException> {
                fillRgbaPrototype(memory, base = 8, width = 4, height = 4, rowStride = 16, seed = 1)
            }
        }
    }

    @Test
    fun vertexPrototypeRejectsAMisalignedBaseAndOverflow() {
        withMemory(64) { memory ->
            assertFailsWith<IllegalArgumentException> {
                fillVerticesPrototype(memory, base = 2, count = 1, seed = 1)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                fillVerticesPrototype(memory, base = 0, count = 3, seed = 1)
            }
        }
    }

    @Test
    fun rgbaPrototypeMatchesTheCheckedKernel() {
        val scenario = Scenario("image.rgba8.test-16.Checked", IMAGE_RGBA8, 64, Variant.Checked, width = 4, height = 4)
        withMemory(64) { checked ->
            withMemory(64) { prototype ->
                val input = prepare(scenario, checked, 7)
                execute(scenario, checked, input, 7)
                fillRgbaPrototype(prototype, base = 0, width = 4, height = 4, rowStride = 16, seed = 7)
                assertEquals(checked.buffer.toByteArray().toList(), prototype.buffer.toByteArray().toList())
            }
        }
    }

    @Test
    fun vertexPrototypeMatchesTheCheckedKernel() {
        val scenario = Scenario("vertices.p3n3uv2.test-4.Checked", VERTICES_P3N3UV2, 128, Variant.Checked, count = 4)
        withMemory(128) { checked ->
            withMemory(128) { prototype ->
                val input = prepare(scenario, checked, 5)
                execute(scenario, checked, input, 5)
                fillVerticesPrototype(prototype, base = 0, count = 4, seed = 5)
                for (index in 0 until 4 * VERTEX_FLOATS) {
                    assertEquals(
                        checked.buffer.getFloat((index * 4).toULong()),
                        prototype.buffer.getFloat((index * 4).toULong()),
                        "float $index",
                    )
                }
            }
        }
    }

    private fun fillByte(memory: BenchmarkMemory, value: Byte) {
        val bytes = ByteArray(memory.buffer.size.toInt()) { value }
        memory.buffer.setBytes(0uL, bytes)
    }

    private fun withMemory(bytes: Int, block: (BenchmarkMemory) -> Unit) {
        val memory = BenchmarkMemory(bytes)
        try {
            block(memory)
        } finally {
            memory.close()
        }
    }
}
