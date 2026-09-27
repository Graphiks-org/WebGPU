package org.graphiks.webgpu.suite.browser

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.suite.acid.foundationCases

fun main() {
    MainScope().launch {
        val report = try {
            runFoundations()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            BrowserReport(
                cases = foundationCases().map {
                    CaseResult(it.id, "not-run", "Runner could not complete")
                },
                fatalError = failure.stackTraceToString(),
            )
        }
        publishReport(Json.encodeToString(report))
    }
}

private fun publishReport(value: String): Unit = js("""{
    globalThis.graphiksSuiteReport = value;
    document.getElementById('result').textContent = value;
}""")
