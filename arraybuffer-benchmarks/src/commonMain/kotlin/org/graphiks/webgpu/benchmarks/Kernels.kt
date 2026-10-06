package org.graphiks.webgpu.benchmarks

/**
 * Data prepared outside the timed window for a scenario. Reads fill the destination in [prepare];
 * `BulkPrepared` builds the source array in [prepare], while `PrepareAndBulk` builds it inside
 * [execute].
 */
internal data class PreparedInput(
    val bytes: ByteArray? = null,
    val floats: FloatArray? = null,
)

internal fun prepare(scenario: Scenario, memory: BenchmarkMemory, seed: Int): PreparedInput =
    when (scenario.workload) {
        SCALAR_READ_I32 -> {
            val buffer = memory.buffer
            val count = scenario.bytes / Int.SIZE_BYTES
            for (i in 0 until count) {
                buffer.setInt((i * Int.SIZE_BYTES).toULong(), word(i, seed))
            }
            PreparedInput()
        }

        BULK_BYTES -> PreparedInput(bytes = ByteArray(scenario.bytes) { word(it, seed).toByte() })

        BULK_FLOATS -> PreparedInput(
            floats = FloatArray(scenario.bytes / Float.SIZE_BYTES) { scalarFloat(it, seed) },
        )

        IMAGE_RGBA8 -> if (scenario.variant == Variant.BulkPrepared) {
            PreparedInput(bytes = rgbaBytes(scenario.width, scenario.height, seed))
        } else {
            PreparedInput()
        }

        VERTICES_P3N3UV2 -> if (scenario.variant == Variant.BulkPrepared) {
            PreparedInput(floats = vertexFloats(scenario.count, seed))
        } else {
            PreparedInput()
        }

        else -> PreparedInput()
    }

/**
 * Runs the scenario kernel once and returns an observable value used to keep the work alive.
 * Reads return a checksum of every value read; writes return an opaque observation of the
 * destination. Complete validation lives in [verify] and stays outside the timed window.
 */
internal fun execute(scenario: Scenario, memory: BenchmarkMemory, input: PreparedInput, seed: Int): Int =
    when (scenario.workload) {
        SCALAR_WRITE_I32 -> {
            writeScalarInt(scenario, memory, seed)
            memory.consume(seed)
        }

        SCALAR_READ_I32 -> readScalarInt(scenario, memory)

        SCALAR_WRITE_F32 -> {
            writeScalarFloat(scenario, memory, seed)
            memory.consume(seed)
        }

        SCATTER_WRITE_I32 -> {
            writeScatter(scenario, memory, seed)
            memory.consume(seed)
        }

        BULK_BYTES -> {
            val source = input.bytes ?: error("bulk.bytes input must be prepared")
            copyBytes(scenario, memory, source)
            memory.consume(seed)
        }

        BULK_FLOATS -> {
            val source = input.floats ?: error("bulk.floats input must be prepared")
            copyFloats(scenario, memory, source)
            memory.consume(seed)
        }

        IMAGE_RGBA8 -> {
            produceImage(scenario, memory, input, seed)
            memory.consume(seed)
        }

        VERTICES_P3N3UV2 -> {
            produceVertices(scenario, memory, input, seed)
            memory.consume(seed)
        }

        else -> error("workload ${scenario.workload} is not part of the base inventory")
    }

private fun writeScalarInt(scenario: Scenario, memory: BenchmarkMemory, seed: Int) {
    val count = scenario.bytes / Int.SIZE_BYTES
    if (scenario.variant == Variant.Reference) {
        for (i in 0 until count) memory.setIntReference(i * Int.SIZE_BYTES, word(i, seed))
    } else {
        val buffer = memory.buffer
        for (i in 0 until count) buffer.setInt((i * Int.SIZE_BYTES).toULong(), word(i, seed))
    }
}

private fun readScalarInt(scenario: Scenario, memory: BenchmarkMemory): Int {
    val count = scenario.bytes / Int.SIZE_BYTES
    var checksum = 0
    if (scenario.variant == Variant.Reference) {
        for (i in 0 until count) {
            checksum = (checksum * 31) xor memory.getIntReference(i * Int.SIZE_BYTES)
        }
    } else {
        val buffer = memory.buffer
        for (i in 0 until count) {
            checksum = (checksum * 31) xor buffer.getInt((i * Int.SIZE_BYTES).toULong())
        }
    }
    return checksum
}

private fun writeScalarFloat(scenario: Scenario, memory: BenchmarkMemory, seed: Int) {
    val count = scenario.bytes / Float.SIZE_BYTES
    if (scenario.variant == Variant.Reference) {
        for (i in 0 until count) memory.setFloatReference(i * Float.SIZE_BYTES, scalarFloat(i, seed))
    } else {
        val buffer = memory.buffer
        for (i in 0 until count) buffer.setFloat((i * Float.SIZE_BYTES).toULong(), scalarFloat(i, seed))
    }
}

private fun writeScatter(scenario: Scenario, memory: BenchmarkMemory, seed: Int) {
    val count = scenario.bytes / Int.SIZE_BYTES
    if (scenario.variant == Variant.Reference) {
        for (i in 0 until count) {
            memory.setIntReference(scatteredIndex(i, count) * Int.SIZE_BYTES, word(i, seed))
        }
    } else {
        val buffer = memory.buffer
        for (i in 0 until count) {
            buffer.setInt((scatteredIndex(i, count) * Int.SIZE_BYTES).toULong(), word(i, seed))
        }
    }
}

private fun copyBytes(scenario: Scenario, memory: BenchmarkMemory, source: ByteArray) {
    if (scenario.variant == Variant.Reference) {
        memory.setBytesReference(0, source)
    } else {
        memory.buffer.setBytes(0u, source)
    }
}

private fun copyFloats(scenario: Scenario, memory: BenchmarkMemory, source: FloatArray) {
    if (scenario.variant == Variant.Reference) {
        memory.setFloatsReference(0, source)
    } else {
        memory.buffer.setFloats(0u, source)
    }
}

private fun produceImage(scenario: Scenario, memory: BenchmarkMemory, input: PreparedInput, seed: Int) {
    val buffer = memory.buffer
    when (scenario.variant) {
        Variant.Checked -> {
            var offset = 0
            for (y in 0 until scenario.height) {
                for (x in 0 until scenario.width) {
                    buffer.setByte(offset.toULong(), ((x + seed) and 255).toByte())
                    buffer.setByte((offset + 1).toULong(), ((y + seed) and 255).toByte())
                    buffer.setByte((offset + 2).toULong(), ((x xor y) and 255).toByte())
                    buffer.setByte((offset + 3).toULong(), 0xFF.toByte())
                    offset += 4
                }
            }
        }

        Variant.BulkPrepared -> buffer.setBytes(0u, input.bytes ?: error("image input must be prepared"))
        Variant.PrepareAndBulk -> buffer.setBytes(0u, rgbaBytes(scenario.width, scenario.height, seed))
        Variant.RgbaWriter -> fillRgbaPrototype(
            memory = memory,
            base = 0,
            width = scenario.width,
            height = scenario.height,
            rowStride = scenario.width * 4,
            seed = seed,
        )

        else -> error("variant ${scenario.variant} is not part of the base inventory")
    }
}

private fun produceVertices(scenario: Scenario, memory: BenchmarkMemory, input: PreparedInput, seed: Int) {
    val buffer = memory.buffer
    when (scenario.variant) {
        Variant.Checked -> {
            for (vertex in 0 until scenario.count) {
                for (component in 0 until VERTEX_FLOATS) {
                    buffer.setFloat(
                        (vertex * VERTEX_BYTES + component * Float.SIZE_BYTES).toULong(),
                        vertexFloat(vertex, component, seed),
                    )
                }
            }
        }

        Variant.BulkPrepared -> buffer.setFloats(0u, input.floats ?: error("vertex input must be prepared"))
        Variant.PrepareAndBulk -> buffer.setFloats(0u, vertexFloats(scenario.count, seed))
        Variant.VertexWriter -> fillVerticesPrototype(memory, base = 0, count = scenario.count, seed = seed)
        else -> error("variant ${scenario.variant} is not part of the base inventory")
    }
}

/**
 * Checks every element the scenario touched against the deterministic pattern. Runs outside the
 * timed window; it reads the measured buffer and never consults a reference buffer.
 */
internal fun verify(scenario: Scenario, memory: BenchmarkMemory, seed: Int) {
    val buffer = memory.buffer
    when (scenario.workload) {
        SCALAR_WRITE_I32, SCALAR_READ_I32 -> {
            val count = scenario.bytes / Int.SIZE_BYTES
            for (i in 0 until count) {
                check(buffer.getInt((i * Int.SIZE_BYTES).toULong()) == word(i, seed)) {
                    "${scenario.id}: int at $i differs"
                }
            }
        }

        SCALAR_WRITE_F32 -> {
            val count = scenario.bytes / Float.SIZE_BYTES
            for (i in 0 until count) {
                check(buffer.getFloat((i * Float.SIZE_BYTES).toULong()) == scalarFloat(i, seed)) {
                    "${scenario.id}: float at $i differs"
                }
            }
        }

        SCATTER_WRITE_I32 -> {
            val count = scenario.bytes / Int.SIZE_BYTES
            for (i in 0 until count) {
                check(buffer.getInt((scatteredIndex(i, count) * Int.SIZE_BYTES).toULong()) == word(i, seed)) {
                    "${scenario.id}: scattered int $i differs"
                }
            }
        }

        BULK_BYTES -> {
            for (i in 0 until scenario.bytes) {
                check(buffer.getByte(i.toULong()) == word(i, seed).toByte()) {
                    "${scenario.id}: byte $i differs"
                }
            }
        }

        BULK_FLOATS -> {
            val count = scenario.bytes / Float.SIZE_BYTES
            for (i in 0 until count) {
                check(buffer.getFloat((i * Float.SIZE_BYTES).toULong()) == scalarFloat(i, seed)) {
                    "${scenario.id}: float copy at $i differs"
                }
            }
        }

        IMAGE_RGBA8 -> {
            // Compare byte by byte: a whole-buffer copy would allocate a second image the size of
            // the destination and can OOM on devices with a small heap.
            var offset = 0
            for (y in 0 until scenario.height) {
                for (x in 0 until scenario.width) {
                    check(buffer.getByte(offset.toULong()) == ((x + seed) and 255).toByte()) {
                        "${scenario.id}: red at ($x,$y) differs"
                    }
                    check(buffer.getByte((offset + 1).toULong()) == ((y + seed) and 255).toByte()) {
                        "${scenario.id}: green at ($x,$y) differs"
                    }
                    check(buffer.getByte((offset + 2).toULong()) == ((x xor y) and 255).toByte()) {
                        "${scenario.id}: blue at ($x,$y) differs"
                    }
                    check(buffer.getByte((offset + 3).toULong()) == 0xFF.toByte()) {
                        "${scenario.id}: alpha at ($x,$y) differs"
                    }
                    offset += 4
                }
            }
        }

        VERTICES_P3N3UV2 -> {
            for (vertex in 0 until scenario.count) {
                for (component in 0 until VERTEX_FLOATS) {
                    check(
                        buffer.getFloat((vertex * VERTEX_BYTES + component * Float.SIZE_BYTES).toULong()) ==
                            vertexFloat(vertex, component, seed),
                    ) { "${scenario.id}: vertex $vertex.$component differs" }
                }
            }
        }

        else -> error("workload ${scenario.workload} is not part of the base inventory")
    }
}
