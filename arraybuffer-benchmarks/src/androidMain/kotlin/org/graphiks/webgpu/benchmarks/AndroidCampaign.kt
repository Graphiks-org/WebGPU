package org.graphiks.webgpu.benchmarks

import android.os.Build

/**
 * Android entry point for the instrumentation host. The module is not published, so this stays out
 * of the library's public surface even though it is public for the Java test runner.
 */
object AndroidCampaign {

    /**
     * Runs the ArrayBuffer business matrix on device so the Java instrumentation runner can
     * validate the direct-buffer behavior without re-implementing the Kotlin unsigned API. Throws
     * an [AssertionError] listing every failed case.
     */
    @JvmStatic
    fun runSafetyChecks() = ArrayBufferAndroidBusinessCases.verifyAll()


    @JvmStatic
    fun run(profile: String, runIndex: Int): String = run(profile, runIndex, null, false)

    @JvmStatic
    fun run(profile: String, runIndex: Int, calibration: String?, writers: Boolean): String {
        val profileConfig = ProfileConfig.of(profile)
        val environment = Environment(
            os = "android-${Build.VERSION.SDK_INT}",
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            runtime = "android-${Build.VERSION.RELEASE}",
            kotlinVersion = KotlinVersion.CURRENT.toString(),
            cpu = Build.HARDWARE ?: "unknown",
        )
        val report = runCampaign(
            profile = profileConfig,
            target = "android",
            runIndex = runIndex,
            environment = environment,
            suiteCommit = "unknown",
            libraryCommit = "unknown",
            dirty = false,
            harnessHash = "unknown",
            libraryHash = "unknown",
            operationsOverride = parseCalibrationSpec(calibration),
            protocol = if (writers) WRITERS_PROTOCOL else PROTOCOL,
            inventory = if (writers) writerScenarios() else scenarios(),
        )
        return encodeReport(report)
    }
}
