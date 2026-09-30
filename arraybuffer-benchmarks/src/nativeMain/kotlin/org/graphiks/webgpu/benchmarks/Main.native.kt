@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package org.graphiks.webgpu.benchmarks

import kotlin.native.Platform
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

private data class Options(val profile: String, val output: String?, val runIndex: Int)

private fun parseOptions(args: Array<String>): Options {
    var profile = "ci"
    var output: String? = null
    var runIndex = 0
    for (argument in args) {
        when {
            argument.startsWith("--profile=") -> profile = argument.substringAfter('=')
            argument.startsWith("--output=") -> output = argument.substringAfter('=')
            argument.startsWith("--run-index=") -> runIndex = argument.substringAfter('=').toInt()
        }
    }
    return Options(profile, output, runIndex)
}

fun main(args: Array<String>) {
    val options = parseOptions(args)
    val profile = ProfileConfig.of(options.profile)
    val environment = Environment(
        os = Platform.osFamily.name,
        architecture = Platform.cpuArchitecture.name,
        runtime = "native",
        kotlinVersion = KotlinVersion.CURRENT.toString(),
    )
    val report = runCampaign(
        profile = profile,
        target = "native",
        runIndex = options.runIndex,
        environment = environment,
        suiteCommit = "unknown",
        libraryCommit = "unknown",
        dirty = false,
        harnessHash = "unknown",
        libraryHash = "unknown",
    )
    val json = encodeReport(report)
    val output = options.output
    if (output == null) {
        println(json)
    } else {
        val file = fopen(output, "w") ?: error("cannot open $output for writing")
        try {
            fputs(json, file)
        } finally {
            fclose(file)
        }
    }
}
