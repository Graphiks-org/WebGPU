@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.graphiks.webgpu.toolkit.web

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.browser.HTMLCanvasElement
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.TargetState
import kotlin.js.JsAny
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals

/** A canvas stand-in: a plain object with mutable `width`/`height` attributes. */
private external interface TestCanvas : JsAny {
    var width: Int
    var height: Int
}

private fun newTestCanvas(): TestCanvas = js("({ width: 100, height: 80 })")

/** Installs a ResizeObserver stub capturing the callback the target registers; the original is kept for restore. */
private fun stubResizeObserver(): Unit = js("""{
    globalThis.__originalResizeObserver = globalThis.ResizeObserver;
    globalThis.__capturedResizeCallback = null;
    globalThis.ResizeObserver = function (callback) {
        globalThis.__capturedResizeCallback = callback;
        this.observe = function () {};
        this.disconnect = function () {};
    };
}""")

/** Makes ResizeObserver unavailable, so targets constructed now take the pull path. */
private fun removeResizeObserver(): Unit = js("""{
    globalThis.__originalResizeObserver = globalThis.ResizeObserver;
    globalThis.ResizeObserver = undefined;
}""")

/** Restores the ResizeObserver reference saved by [stubResizeObserver] or [removeResizeObserver]. */
private fun restoreResizeObserver(): Unit = js("""{
    globalThis.ResizeObserver = globalThis.__originalResizeObserver;
    delete globalThis.__originalResizeObserver;
    delete globalThis.__capturedResizeCallback;
}""")

/** Delivers one resize notification to the callback the target registered with the stub. */
private fun fireCapturedResize(): Unit = js("globalThis.__capturedResizeCallback([])")

class CanvasSurfaceTargetTest {
    @Test
    fun resizeObserverDrivesTheStatePush() {
        stubResizeObserver()
        try {
            val canvas = newTestCanvas().unsafeCast<HTMLCanvasElement>()
            val target = CanvasSurfaceTarget(canvas)
            val initial = target.state.value
            assertEquals(Extent2D(width = 100u, height = 80u), initial.extent)
            assertEquals(0L, initial.generation)
            // With the observer present, reads must NOT refresh: a silent attribute change
            // stays invisible until the observer reports it.
            canvas.width = 150
            val pulled = target.state.value
            assertEquals(Extent2D(width = 100u, height = 80u), pulled.extent)
            assertEquals(0L, pulled.generation)
            // The observer callback pushes the new extent and bumps the generation.
            canvas.width = 200
            fireCapturedResize()
            val pushed = target.state.value
            assertEquals(Extent2D(width = 200u, height = 80u), pushed.extent)
            assertEquals(1L, pushed.generation)
        } finally {
            restoreResizeObserver()
        }
    }

    @Test
    fun stateFlowEmitsOnTheResizeCallback() = runTest {
        stubResizeObserver()
        try {
            val canvas = newTestCanvas().unsafeCast<HTMLCanvasElement>()
            val target = CanvasSurfaceTarget(canvas)
            val emissions = mutableListOf<TargetState>()
            val collector = launch { target.state.collect { emissions += it } }
            advanceUntilIdle()
            assertEquals(1, emissions.size)
            canvas.width = 640
            fireCapturedResize()
            advanceUntilIdle()
            assertEquals(2, emissions.size)
            assertEquals(Extent2D(width = 640u, height = 80u), emissions[1].extent)
            assertEquals(1L, emissions[1].generation)
            collector.cancelAndJoin()
        } finally {
            restoreResizeObserver()
        }
    }

    @Test
    fun pullFallbackRefreshesOnEveryReadWithoutResizeObserver() {
        removeResizeObserver()
        try {
            val canvas = newTestCanvas().unsafeCast<HTMLCanvasElement>()
            val target = CanvasSurfaceTarget(canvas)
            canvas.width = 320
            val state = target.state.value
            assertEquals(Extent2D(width = 320u, height = 80u), state.extent)
            assertEquals(1L, state.generation)
        } finally {
            restoreResizeObserver()
        }
    }
}
