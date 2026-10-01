@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.UnsafeNumber::class,
)

package org.graphiks.webgpu.benchmarks

import kotlin.native.Platform
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind

private data class Options(val profile: String, val output: String?, val runIndex: Int, val calibration: String?)

private fun parseOptions(args: Array<String>): Options {
    var profile = "ci"
    var output: String? = null
    var runIndex = 0
    var calibration: String? = null
    for (argument in args) {
        when {
            argument.startsWith("--profile=") -> profile = argument.substringAfter('=')
            argument.startsWith("--output=") -> output = argument.substringAfter('=')
            argument.startsWith("--run-index=") -> runIndex = argument.substringAfter('=').toInt()
            argument.startsWith("--calibration=") -> calibration = argument.substringAfter('=')
        }
    }
    return Options(profile, output, runIndex, calibration)
}

private fun readTextFile(path: String): String? {
    val file = fopen(path, "rb") ?: return null
    try {
        fseek(file, 0, SEEK_END)
        val length = ftell(file).toInt()
        rewind(file)
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        bytes.usePinned { pinned -> fread(pinned.addressOf(0), 1.convert(), length.convert(), file) }
        return bytes.decodeToString()
    } finally {
        fclose(file)
    }
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
        operationsOverride = options.calibration?.let { path -> readTextFile(path)?.let(::calibrationFromReport) },
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
