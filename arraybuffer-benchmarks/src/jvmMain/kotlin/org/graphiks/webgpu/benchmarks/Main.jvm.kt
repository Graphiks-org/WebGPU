package org.graphiks.webgpu.benchmarks

import java.io.File
import java.security.MessageDigest

private data class Options(
    val profile: String,
    val output: String?,
    val runIndex: Int,
    val calibration: String?,
    val writers: Boolean,
)

private fun parseOptions(args: Array<String>): Options {
    var profile = "ci"
    var output: String? = null
    var runIndex = 0
    var calibration: String? = null
    var writers = false
    for (argument in args) {
        when {
            argument.startsWith("--profile=") -> profile = argument.substringAfter('=')
            argument.startsWith("--output=") -> output = argument.substringAfter('=')
            argument.startsWith("--run-index=") -> runIndex = argument.substringAfter('=').toInt()
            argument.startsWith("--calibration=") -> calibration = argument.substringAfter('=')
            argument == "--writers" -> writers = true
        }
    }
    return Options(profile, output, runIndex, calibration, writers)
}

private fun runGit(dir: File, vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args)
        .directory(dir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    process.waitFor()
    if (process.exitValue() == 0) output else null
} catch (_: Throwable) {
    null
}

private fun contentHash(root: File, relative: String): String {
    val directory = File(root, relative)
    if (!directory.isDirectory) return "unknown"
    val digest = MessageDigest.getInstance("SHA-256")
    directory.walkTopDown()
        .filter { it.isFile }
        .sortedBy { it.relativeTo(root).path }
        .forEach { file ->
            digest.update(file.relativeTo(root).path.toByteArray())
            digest.update(file.readBytes())
        }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun main(args: Array<String>) {
    val options = parseOptions(args)
    val profile = ProfileConfig.of(options.profile)
    val workingDir = File(".").canonicalFile
    val commit = runGit(workingDir, "rev-parse", "HEAD") ?: "unknown"
    val dirty = runGit(workingDir, "status", "--porcelain")?.isNotBlank() ?: false
    val harnessHash = contentHash(workingDir, "arraybuffer-benchmarks/src")
    val libraryHash = contentHash(workingDir, "webgpu-api/src")
    val environment = Environment(
        os = System.getProperty("os.name") ?: "unknown",
        architecture = System.getProperty("os.arch") ?: "unknown",
        runtime = "jvm-" + (System.getProperty("java.version") ?: "unknown"),
        kotlinVersion = KotlinVersion.CURRENT.toString(),
        cpu = System.getProperty("os.arch") ?: "unknown",
    )
    val report = runCampaign(
        profile = profile,
        target = "jvm",
        runIndex = options.runIndex,
        environment = environment,
        suiteCommit = commit,
        libraryCommit = commit,
        dirty = dirty,
        harnessHash = harnessHash,
        libraryHash = libraryHash,
        operationsOverride = options.calibration?.let { calibrationFromReport(File(it).readText()) },
        protocol = if (options.writers) WRITERS_PROTOCOL else PROTOCOL,
        inventory = if (options.writers) writerScenarios() else scenarios(),
    )
    val json = encodeReport(report)
    if (options.output == null) {
        println(json)
    } else {
        val output = File(options.output)
        output.parentFile?.mkdirs()
        output.writeText(json)
        println("wrote ${output.path}")
    }
}
