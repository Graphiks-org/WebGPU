package org.graphiks.webgpu.benchmarks

/**
 * Benchmark variant. [RgbaWriter] and [VertexWriter] belong to the companion writers protocol and
 * are deliberately absent from the base inventory produced by [scenarios].
 */
internal enum class Variant {
    Checked,
    Reference,
    BulkPrepared,
    PrepareAndBulk,
    RgbaWriter,
    VertexWriter,
}

/**
 * One measured scenario. [bytes] is the destination size in bytes; [width]/[height] and [count]
 * describe the layout when the workload is an image or an interleaved vertex stream.
 */
internal data class Scenario(
    val id: String,
    val workload: String,
    val bytes: Int,
    val variant: Variant,
    val width: Int = 0,
    val height: Int = 0,
    val count: Int = 0,
)

internal const val SCALAR_WRITE_I32 = "scalar.write.i32"
internal const val SCALAR_READ_I32 = "scalar.read.i32"
internal const val SCALAR_WRITE_F32 = "scalar.write.f32"
internal const val SCATTER_WRITE_I32 = "scatter.write.i32"
internal const val BULK_BYTES = "bulk.bytes"
internal const val BULK_FLOATS = "bulk.floats"
internal const val IMAGE_RGBA8 = "image.rgba8"
internal const val VERTICES_P3N3UV2 = "vertices.p3n3uv2"

internal val SCALAR_SIZES = intArrayOf(256, 65_536, 8 * 1024 * 1024)
internal val SCATTER_SIZES = intArrayOf(65_536, 8 * 1024 * 1024)
internal val BULK_SIZES = intArrayOf(256, 65_536, 8 * 1024 * 1024)
internal val IMAGE_DIMENSIONS = listOf(256 to 256, 1024 to 1024, 4096 to 4096)
internal val VERTEX_COUNTS = intArrayOf(1024, 65_536, 1_048_576)

internal const val VERTEX_FLOATS = 8
internal const val VERTEX_BYTES = VERTEX_FLOATS * Float.SIZE_BYTES

private fun scalarVariant(): List<Variant> = listOf(Variant.Checked, Variant.Reference)

private fun bulkProduceVariant(): List<Variant> =
    listOf(Variant.Checked, Variant.BulkPrepared, Variant.PrepareAndBulk)

internal fun scenarioId(workload: String, bytes: Int, variant: Variant): String =
    "$workload.bytes-$bytes.${variant.name}"

/**
 * Companion inventory for the prevalidated writer prototypes. It re-measures its own controls
 * (`Checked`, `BulkPrepared`, `PrepareAndBulk`) alongside `RgbaWriter`/`VertexWriter`, because a
 * comparison with the base protocol is not automatic.
 */
internal fun writerScenarios(): List<Scenario> {
    val result = mutableListOf<Scenario>()
    for ((width, height) in IMAGE_DIMENSIONS) {
        val bytes = width * height * 4
        for (variant in listOf(Variant.Checked, Variant.BulkPrepared, Variant.PrepareAndBulk, Variant.RgbaWriter)) {
            result += Scenario(scenarioId(IMAGE_RGBA8, bytes, variant), IMAGE_RGBA8, bytes, variant, width = width, height = height)
        }
    }
    for (count in VERTEX_COUNTS) {
        val bytes = count * VERTEX_BYTES
        for (variant in listOf(Variant.Checked, Variant.BulkPrepared, Variant.PrepareAndBulk, Variant.VertexWriter)) {
            result += Scenario(scenarioId(VERTICES_P3N3UV2, bytes, variant), VERTICES_P3N3UV2, bytes, variant, count = count)
        }
    }
    return result
}

internal fun scenarios(): List<Scenario> {
    val result = mutableListOf<Scenario>()

    for (size in SCALAR_SIZES) {
        for (variant in scalarVariant()) {
            result += Scenario(scenarioId(SCALAR_WRITE_I32, size, variant), SCALAR_WRITE_I32, size, variant)
            result += Scenario(scenarioId(SCALAR_READ_I32, size, variant), SCALAR_READ_I32, size, variant)
            result += Scenario(scenarioId(SCALAR_WRITE_F32, size, variant), SCALAR_WRITE_F32, size, variant)
        }
    }

    for (size in SCATTER_SIZES) {
        for (variant in scalarVariant()) {
            result += Scenario(scenarioId(SCATTER_WRITE_I32, size, variant), SCATTER_WRITE_I32, size, variant)
        }
    }

    for (size in BULK_SIZES) {
        for (variant in scalarVariant()) {
            result += Scenario(scenarioId(BULK_BYTES, size, variant), BULK_BYTES, size, variant)
            result += Scenario(scenarioId(BULK_FLOATS, size, variant), BULK_FLOATS, size, variant)
        }
    }

    for ((width, height) in IMAGE_DIMENSIONS) {
        val bytes = width * height * 4
        for (variant in bulkProduceVariant()) {
            result += Scenario(
                id = scenarioId(IMAGE_RGBA8, bytes, variant),
                workload = IMAGE_RGBA8,
                bytes = bytes,
                variant = variant,
                width = width,
                height = height,
            )
        }
    }

    for (count in VERTEX_COUNTS) {
        val bytes = count * VERTEX_BYTES
        for (variant in bulkProduceVariant()) {
            result += Scenario(
                id = scenarioId(VERTICES_P3N3UV2, bytes, variant),
                workload = VERTICES_P3N3UV2,
                bytes = bytes,
                variant = variant,
                count = count,
            )
        }
    }

    return result
}
