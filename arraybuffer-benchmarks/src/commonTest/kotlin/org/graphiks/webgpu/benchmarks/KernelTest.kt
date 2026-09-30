package org.graphiks.webgpu.benchmarks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KernelTest {

    @Test
    fun scatterIsAPermutation() {
        val n = 16384
        assertEquals((0 until n).toSet(), (0 until n).map { scatteredIndex(it, n) }.toSet())
    }

    @Test
    fun patternsReturnTheSpecifiedValues() {
        assertEquals(0 xor (0x13579bdf + 7), word(0, 7))
        assertEquals(1000 xor (0x13579bdf + 7), word(1000, 7))
        assertEquals(17f, scalarFloat(10, 7))
        assertEquals(0f, scalarFloat(0, 0))
        assertEquals(4f, vertexFloat(0, 4, 0))
        assertEquals(11f, vertexFloat(1, 8, -5))
    }

    @Test
    fun prepareAndBulkDefersTheSourceArray() {
        val scenario = scenarios().first {
            it.workload == IMAGE_RGBA8 && it.variant == Variant.PrepareAndBulk
        }
        withMemory(scenario.bytes) { memory ->
            val input = prepare(scenario, memory, 3)
            assertNull(input.bytes, "PrepareAndBulk must allocate its source inside execute")
            assertNull(input.floats)
        }
    }

    @Test
    fun bulkPreparedBuildsTheSourceArrayDuringPreparation() {
        val scenario = scenarios().first {
            it.workload == VERTICES_P3N3UV2 && it.variant == Variant.BulkPrepared && it.count == 1024
        }
        withMemory(scenario.bytes) { memory ->
            val input = prepare(scenario, memory, 11)
            assertNotNull(input.floats, "BulkPrepared must prepare the source array")
            assertEquals(scenario.count * VERTEX_FLOATS, input.floats.size)
        }
    }

    @Test
    fun smallImageVariantsProduceTheExpectedBytes() {
        for (variant in listOf(Variant.Checked, Variant.BulkPrepared, Variant.PrepareAndBulk)) {
            val scenario = Scenario(
                id = "image.rgba8.test-16.$variant",
                workload = IMAGE_RGBA8,
                bytes = 4 * 4 * 4,
                variant = variant,
                width = 4,
                height = 4,
            )
            roundTrip(scenario, seed = 5)
        }
    }

    @Test
    fun smallVertexVariantsProduceTheExpectedFloats() {
        for (variant in listOf(Variant.Checked, Variant.BulkPrepared, Variant.PrepareAndBulk)) {
            val scenario = Scenario(
                id = "vertices.p3n3uv2.test-4.$variant",
                workload = VERTICES_P3N3UV2,
                bytes = 4 * VERTEX_BYTES,
                variant = variant,
                count = 4,
            )
            roundTrip(scenario, seed = 9)
        }
    }

    @Test
    fun scalarAndBulkVariantsRoundTrip() {
        val cases = listOf(
            Scenario("scalar.write.i32.t.16.Checked", SCALAR_WRITE_I32, 64, Variant.Checked),
            Scenario("scalar.write.i32.t.16.Reference", SCALAR_WRITE_I32, 64, Variant.Reference),
            Scenario("scalar.read.i32.t.16.Checked", SCALAR_READ_I32, 64, Variant.Checked),
            Scenario("scalar.read.i32.t.16.Reference", SCALAR_READ_I32, 64, Variant.Reference),
            Scenario("scalar.write.f32.t.16.Checked", SCALAR_WRITE_F32, 64, Variant.Checked),
            Scenario("scalar.write.f32.t.16.Reference", SCALAR_WRITE_F32, 64, Variant.Reference),
            Scenario("scatter.write.i32.t.16.Checked", SCATTER_WRITE_I32, 64, Variant.Checked),
            Scenario("scatter.write.i32.t.16.Reference", SCATTER_WRITE_I32, 64, Variant.Reference),
            Scenario("bulk.bytes.t.64.Checked", BULK_BYTES, 64, Variant.Checked),
            Scenario("bulk.bytes.t.64.Reference", BULK_BYTES, 64, Variant.Reference),
            Scenario("bulk.floats.t.64.Checked", BULK_FLOATS, 64, Variant.Checked),
            Scenario("bulk.floats.t.64.Reference", BULK_FLOATS, 64, Variant.Reference),
        )
        for (scenario in cases) roundTrip(scenario, seed = 13)
    }

    @Test
    fun readsReturnAChecksumOfEveryValue() {
        val scenario = Scenario("scalar.read.i32.t.16.Checked", SCALAR_READ_I32, 64, Variant.Checked)
        val reference = Scenario("scalar.read.i32.t.16.Reference", SCALAR_READ_I32, 64, Variant.Reference)
        withMemory(64) { memory ->
            prepare(scenario, memory, 4)
            val checked = execute(scenario, memory, PreparedInput(), 4)
            var expected = 0
            for (i in 0 until 16) expected = (expected * 31) xor word(i, 4)
            assertEquals(expected, checked)
            assertEquals(checked, execute(reference, memory, PreparedInput(), 4))
        }
    }

    private fun roundTrip(scenario: Scenario, seed: Int) {
        withMemory(scenario.bytes) { memory ->
            val input = prepare(scenario, memory, seed)
            execute(scenario, memory, input, seed)
            verify(scenario, memory, seed)
        }
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
