package org.graphiks.webgpu.suite.browser

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.suite.acid.foundationCases

fun main() {
    MainScope().launch {
        val adapter = requestAdapter().getOrThrow()
        try {
            val device = adapter.requestDevice().getOrThrow()
            try {
                foundationCases().single().run(device)
                showResult("PASS buffers.mapped-at-creation")
            } finally {
                device.close()
            }
        } finally {
            adapter.close()
        }
    }
}

private fun showResult(value: String): Unit =
    js("{ document.getElementById('result').textContent = value; }")
