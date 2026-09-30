package org.graphiks.webgpu.benchmarks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val PROTOCOL = "arraybuffer-cpu-v1"
internal const val SCHEMA_VERSION = 1

@Serializable
internal data class Environment(
    val os: String,
    val architecture: String,
    val runtime: String,
    val kotlinVersion: String,
    val cpu: String = "unknown",
    val compileMode: String = "release",
    val browser: String = "n/a",
    val deviceKind: String = "physical",
    val thermal: String = "unknown",
)

@Serializable
internal data class ScenarioReport(
    val scenarioId: String,
    val workload: String,
    val variant: String,
    val bytesPerOperation: Int,
    val width: Int = 0,
    val height: Int = 0,
    val count: Int = 0,
    val operationsPerSample: Int,
    val warmups: Int,
    val status: String,
    val outputVerified: Boolean,
    val samplesMs: List<Double> = emptyList(),
    val seeds: List<Int> = emptyList(),
    val checksum: Int? = null,
    val allocatedBytesPerOperation: Long? = null,
    val allocationMeasurement: String = "unavailable",
    val diagnostic: String? = null,
)

@Serializable
internal data class CampaignReport(
    val schemaVersion: Int = SCHEMA_VERSION,
    val protocol: String = PROTOCOL,
    val target: String,
    val buildMode: String,
    val suiteCommit: String,
    val libraryCommit: String,
    val dirty: Boolean,
    val harnessHash: String,
    val libraryHash: String,
    val environmentId: String,
    val profile: String,
    val runIndex: Int,
    val environment: Environment,
    val variantOrder: List<String> = emptyList(),
    val scenarios: List<ScenarioReport>,
    val fatalError: String? = null,
)

internal val reportJson: Json = Json {
    prettyPrint = false
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = true
}

internal fun encodeReport(report: CampaignReport): String = reportJson.encodeToString(CampaignReport.serializer(), report)

internal fun environmentId(environment: Environment): String {
    fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    return listOf(environment.os, environment.architecture, environment.runtime)
        .joinToString("-") { normalize(it) }
        .ifEmpty { "unknown" }
}
