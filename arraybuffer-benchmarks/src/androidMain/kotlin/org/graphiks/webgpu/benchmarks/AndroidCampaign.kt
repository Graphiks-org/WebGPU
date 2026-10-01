package org.graphiks.webgpu.benchmarks

import android.os.Build
import org.graphiks.webgpu.ArrayBuffer

/**
 * Android entry point for the instrumentation host. The module is not published, so this stays out
 * of the library's public surface even though it is public for the Java test runner.
 */
object AndroidCampaign {

    /**
     * Runs the Android `ByteBuffer` safety checks on device so the Java instrumentation runner can
     * assert the direct-buffer behavior without re-implementing the Kotlin unsigned API. Throws on
     * the first failed expectation.
     */
    @JvmStatic
    fun runSafetyChecks() {
        val buffer = ArrayBuffer.of(ByteArray(16))
        buffer.setInt(12uL, 7)
        check(buffer.getInt(12uL) == 7) { "valid access failed" }
        expect<IndexOutOfBoundsException> { buffer.setInt(13uL, 1) }
        expect<IndexOutOfBoundsException> { buffer.getInt(16uL) }
        expect<IndexOutOfBoundsException> { buffer.setInt(4_294_967_296uL, 1) }
        expect<IllegalArgumentException> { buffer.setInt(1uL, 1) }
        expect<IllegalArgumentException> { buffer.getInt(2uL) }

        val sentinel = ArrayBuffer.of(ByteArray(16) { 0x5a.toByte() })
        expect<IndexOutOfBoundsException> { sentinel.setInts(12uL, intArrayOf(1, 2)) }
        expect<IllegalArgumentException> { sentinel.setInts(1uL, intArrayOf(1)) }
        check(sentinel.getByte(0uL) == 0x5a.toByte()) { "rejected write mutated the prefix" }
        check(sentinel.getByte(15uL) == 0x5a.toByte()) { "rejected write mutated the suffix" }

        val empty = ArrayBuffer.allocate(0uL)
        check(empty.size == 0uL) { "zero allocation has a non-zero size" }
        empty.setInts(0uL, intArrayOf())
        check(empty.toIntArray().isEmpty()) { "empty conversion is not empty" }
        expect<IllegalArgumentException> { ArrayBuffer.allocate(3uL).toIntArray() }
        expect<IllegalArgumentException> { ArrayBuffer.allocate(ULong.MAX_VALUE) }
    }

    private inline fun <reified T : Throwable> expect(block: () -> Unit) {
        try {
            block()
        } catch (failure: Throwable) {
            if (failure is T) return
            throw AssertionError("expected ${T::class.simpleName} but got $failure")
        }
        throw AssertionError("expected ${T::class.simpleName} but nothing was thrown")
    }

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
