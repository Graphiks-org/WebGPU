@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.benchmarks

private fun queryParameter(name: String): String? =
    js("new URL(globalThis.location.href).searchParams.get(name)")

private fun documentHidden(): Boolean = js("globalThis.document.visibilityState !== 'visible'")

private fun browserUserAgent(): String = js("globalThis.navigator.userAgent")

private fun publish(value: String) {
    js("globalThis.graphiksArrayBufferReport = value")
}

private fun errorReport(target: String, profile: String, runIndex: Int, message: String): String =
    encodeReport(
        CampaignReport(
            target = target,
            buildMode = "release",
            suiteCommit = "unknown",
            libraryCommit = "unknown",
            dirty = false,
            harnessHash = "unknown",
            libraryHash = "unknown",
            environmentId = "unknown",
            profile = profile,
            runIndex = runIndex,
            environment = Environment(
                os = "unknown",
                architecture = "unknown",
                runtime = "browser",
                kotlinVersion = KotlinVersion.CURRENT.toString(),
            ),
            scenarios = emptyList(),
            fatalError = message,
        ),
    )

fun main() {
    val profileId = queryParameter("profile") ?: "ci"
    val runIndex = queryParameter("runIndex")?.toIntOrNull() ?: 0
    val target = queryParameter("target") ?: "js"
    val commit = queryParameter("commit") ?: "unknown"
    val dirty = queryParameter("dirty") == "true"
    val harnessHash = queryParameter("harnessHash") ?: "unknown"
    val libraryHash = queryParameter("libraryHash") ?: "unknown"

    if (documentHidden()) {
        publish(errorReport(target, profileId, runIndex, "document is hidden; campaign interrupted"))
        return
    }

    try {
        val profile = ProfileConfig.of(profileId)
        val environment = Environment(
            os = queryParameter("os") ?: "unknown",
            architecture = queryParameter("arch") ?: "unknown",
            runtime = "browser",
            kotlinVersion = KotlinVersion.CURRENT.toString(),
            browser = browserUserAgent(),
        )
        val report = runCampaign(
            profile = profile,
            target = target,
            runIndex = runIndex,
            environment = environment,
            suiteCommit = commit,
            libraryCommit = commit,
            dirty = dirty,
            harnessHash = harnessHash,
            libraryHash = libraryHash,
        )
        publish(encodeReport(report))
    } catch (failure: Throwable) {
        publish(errorReport(target, profileId, runIndex, failure.message ?: failure.toString()))
    }
}
