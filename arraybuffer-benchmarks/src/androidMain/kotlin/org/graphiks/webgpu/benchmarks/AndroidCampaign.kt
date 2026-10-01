package org.graphiks.webgpu.benchmarks

import android.os.Build

/**
 * Android entry point for the instrumentation host. The module is not published, so this stays out
 * of the library's public surface even though it is public for the Java test runner.
 */
object AndroidCampaign {
    @JvmStatic
    fun run(profile: String, runIndex: Int): String = run(profile, runIndex, null)

    @JvmStatic
    fun run(profile: String, runIndex: Int, calibration: String?): String {
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
        )
        return encodeReport(report)
    }
}
