package org.graphiks.webgpu.benchmarks

import kotlin.time.TimeSource

internal data class ProfileConfig(
    val id: String,
    val warmups: Int,
    val minWarmupMs: Long,
    val samples: Int,
    val launches: Int,
) {
    companion object {
        fun of(id: String): ProfileConfig = when (id) {
            "ci" -> ProfileConfig(id = "ci", warmups = 3, minWarmupMs = 0, samples = 5, launches = 1)
            "standard" -> ProfileConfig(id = "standard", warmups = 5, minWarmupMs = 2000, samples = 30, launches = 5)
            else -> throw IllegalArgumentException("unknown profile: $id")
        }
    }
}

private const val MIN_SAMPLE_MS = 10.0
private const val MAX_REPETITIONS = 65_536
private const val MAX_WARMUP_ROUNDS = 100_000

internal data class Measurement(
    val operationsPerSample: Int,
    val warmups: Int,
    val samplesMs: List<Double>,
    val seeds: List<Int>,
    val checksum: Int,
    val verifySeed: Int,
)

private fun elapsedMs(mark: TimeSource.Monotonic.ValueTimeMark): Double =
    mark.elapsedNow().inWholeMicroseconds / 1000.0

private fun runRepeats(
    scenario: Scenario,
    memory: BenchmarkMemory,
    input: PreparedInput,
    seed: Int,
    operations: Int,
): Double {
    var aggregate = 0
    val mark = TimeSource.Monotonic.markNow()
    for (repetition in 0 until operations) {
        aggregate = aggregate * 31 xor execute(scenario, memory, input, seed + repetition)
    }
    // Keep the aggregate observable even if a future JIT would otherwise drop it.
    memory.consume(aggregate)
    return elapsedMs(mark)
}

private fun calibrate(
    scenario: Scenario,
    memory: BenchmarkMemory,
    input: PreparedInput,
    baseSeed: Int,
): Int {
    var operations = 1
    var elapsed = runRepeats(scenario, memory, input, baseSeed, operations)
    while (elapsed < MIN_SAMPLE_MS && operations < MAX_REPETITIONS) {
        operations *= 2
        elapsed = runRepeats(scenario, memory, input, baseSeed, operations)
    }
    return operations
}

internal fun measure(
    scenario: Scenario,
    memory: BenchmarkMemory,
    input: PreparedInput,
    profile: ProfileConfig,
    baseSeed: Int,
    overrideOperations: Int? = null,
): Measurement {
    // A before/after comparison reuses the baseline's repetition count instead of recalibrating.
    val operations = overrideOperations?.takeIf { it > 0 } ?: calibrate(scenario, memory, input, baseSeed)

    val warmupMark = TimeSource.Monotonic.markNow()
    var warmups = 0
    while (warmups < profile.warmups || (profile.minWarmupMs > 0 && elapsedMs(warmupMark) < profile.minWarmupMs)) {
        runRepeats(scenario, memory, input, baseSeed, operations)
        warmups++
        if (warmups >= MAX_WARMUP_ROUNDS) break
    }

    val samples = ArrayList<Double>(profile.samples)
    val seeds = ArrayList<Int>(profile.samples)
    var checksum = 0
    for (sample in 0 until profile.samples) {
        val seed = baseSeed + sample
        seeds += seed
        var aggregate = 0
        val mark = TimeSource.Monotonic.markNow()
        for (repetition in 0 until operations) {
            aggregate = aggregate * 31 xor execute(scenario, memory, input, seed + repetition)
        }
        memory.consume(aggregate)
        samples += elapsedMs(mark)
        checksum = aggregate
    }

    // `prepare` owns the source for reads, bulk copies and BulkPrepared; those must be verified
    // against the preparation seed. Everything else writes with the last executed seed.
    val verifySeed = when {
        scenario.workload == SCALAR_READ_I32 -> baseSeed
        scenario.workload == BULK_BYTES || scenario.workload == BULK_FLOATS -> baseSeed
        scenario.variant == Variant.BulkPrepared -> baseSeed
        else -> baseSeed + (profile.samples - 1) + (operations - 1)
    }
    return Measurement(operations, warmups, samples, seeds, checksum, verifySeed)
}

/** Scenarios in execution order; within each layout group the variant order flips on odd launches. */
internal fun orderedScenarios(runIndex: Int): List<Scenario> {
    val groups = scenarios().groupBy { listOf(it.workload, it.bytes, it.width, it.height, it.count) }
    val result = ArrayList<Scenario>()
    for (group in groups.values) {
        result += if (runIndex % 2 == 0) group else group.reversed()
    }
    return result
}

internal fun runCampaign(
    profile: ProfileConfig,
    target: String,
    runIndex: Int,
    environment: Environment,
    suiteCommit: String,
    libraryCommit: String,
    dirty: Boolean,
    harnessHash: String,
    libraryHash: String,
    operationsOverride: Map<String, Int>? = null,
): CampaignReport {
    val order = orderedScenarios(runIndex)
    val reports = ArrayList<ScenarioReport>(order.size)
    var fatalError: String? = null

    for (scenario in order) {
        val memory = BenchmarkMemory(scenario.bytes)
        try {
            val seed = 1 + (scenario.id.hashCode() and 0x0FFF)
            val input = prepare(scenario, memory, seed)
            val measurement = measure(scenario, memory, input, profile, seed, operationsOverride?.get(scenario.id))
            verify(scenario, memory, measurement.verifySeed)
            reports += ScenarioReport(
                scenarioId = scenario.id,
                workload = scenario.workload,
                variant = scenario.variant.name,
                bytesPerOperation = scenario.bytes,
                width = scenario.width,
                height = scenario.height,
                count = scenario.count,
                operationsPerSample = measurement.operationsPerSample,
                warmups = measurement.warmups,
                status = "completed",
                outputVerified = true,
                samplesMs = measurement.samplesMs,
                seeds = measurement.seeds,
                checksum = measurement.checksum,
            )
        } catch (failure: Throwable) {
            fatalError = fatalError ?: failure.message
            reports += ScenarioReport(
                scenarioId = scenario.id,
                workload = scenario.workload,
                variant = scenario.variant.name,
                bytesPerOperation = scenario.bytes,
                width = scenario.width,
                height = scenario.height,
                count = scenario.count,
                operationsPerSample = 0,
                warmups = 0,
                status = "failed",
                outputVerified = false,
                diagnostic = failure.message ?: failure.toString(),
            )
        } finally {
            memory.close()
        }
    }

    return CampaignReport(
        target = target,
        buildMode = environment.compileMode,
        suiteCommit = suiteCommit,
        libraryCommit = libraryCommit,
        dirty = dirty,
        harnessHash = harnessHash,
        libraryHash = libraryHash,
        environmentId = environmentId(environment),
        profile = profile.id,
        runIndex = runIndex,
        environment = environment,
        variantOrder = order.map { it.id },
        scenarios = reports,
        fatalError = fatalError,
    )
}
