package org.graphiks.webgpu.suite.browser

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.suite.acid.foundationCases
import org.graphiks.webgpu.suite.browser.demos.elementById
import org.graphiks.webgpu.suite.browser.demos.queryParameter
import org.graphiks.webgpu.suite.browser.demos.selectedLocale
import org.graphiks.webgpu.suite.browser.demos.showParticlesPage

/**
 * The page has two explicit routes: without a `demo` parameter it runs the foundation validation
 * suite exactly as before and publishes `graphiksSuiteReport`; `?demo=particles` runs the interactive
 * particle demo instead. An unknown demo name fails visibly rather than silently running nothing.
 */
fun main() {
    MainScope().launch {
        when (val demo = queryParameter("demo")) {
            null -> runValidation()
            "particles" -> if (queryParameter("verify") == "1") {
                verifyParticles()
            } else {
                showParticlesPage(selectedLocale())
            }
            else -> showUnknownRoute(demo)
        }
    }
}

private suspend fun runValidation() {
    val report = try {
        runFoundations()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        BrowserReport(
            cases = foundationCases().map {
                CaseResult(it.id.id, "not-run", "Runner could not complete")
            },
            fatalError = failure.stackTraceToString(),
        )
    }
    publishReport(Json.encodeToString(report))
}

private fun showUnknownRoute(demo: String) {
    elementById("validation")?.hidden = true
    elementById("demo-root")?.apply {
        hidden = false
        textContent = "Unknown demo '$demo'. Available demos: particles."
    }
}

private fun verifyParticles() {
    // The GPU verification route is wired in the next step.
    showUnknownRoute("particles (verification is not available in this build)")
}

private fun publishReport(value: String): Unit = js("""{
    globalThis.graphiksSuiteReport = value;
    document.getElementById('result').textContent = value;
}""")
