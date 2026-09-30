package org.graphiks.webgpu.benchmarks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtocolTest {

    @Test
    fun reportInventoryHasUniqueIds() {
        val ids = scenarios().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(52, ids.size)
    }

    @Test
    fun scenarioIdsFollowTheDeclaredFormat() {
        for (scenario in scenarios()) {
            assertEquals(scenarioId(scenario.workload, scenario.bytes, scenario.variant), scenario.id)
        }
    }

    @Test
    fun scalarAndBulkSizesAreElementAligned() {
        scenarios()
            .filter { it.workload.startsWith("scalar") || it.workload == BULK_FLOATS }
            .forEach { assertEquals(0, it.bytes % Int.SIZE_BYTES, it.id) }
    }

    @Test
    fun scatterCountsArePowersOfTwo() {
        scenarios().filter { it.workload == SCATTER_WRITE_I32 }.forEach {
            val count = it.bytes / Int.SIZE_BYTES
            assertTrue(count > 0 && count and (count - 1) == 0, "${it.id} count=$count")
        }
    }

    @Test
    fun layoutSizesMatchTheDeclaredDimensions() {
        scenarios().filter { it.workload == IMAGE_RGBA8 }.forEach {
            assertEquals(it.width * it.height * 4, it.bytes, it.id)
        }
        scenarios().filter { it.workload == VERTICES_P3N3UV2 }.forEach {
            assertEquals(it.count * VERTEX_BYTES, it.bytes, it.id)
        }
    }

    @Test
    fun writerVariantsAreAbsentFromTheBaseInventory() {
        val writers = scenarios().filter { it.variant == Variant.RgbaWriter || it.variant == Variant.VertexWriter }
        assertTrue(writers.isEmpty(), "writers belong to the companion protocol: $writers")
    }
}
