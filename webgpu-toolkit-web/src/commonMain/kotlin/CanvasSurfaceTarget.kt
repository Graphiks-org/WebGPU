@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.toolkit.web

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.webgpu.browser.HTMLCanvasElement
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import kotlin.js.JsAny
import kotlin.js.js

/**
 * The minimal `ResizeObserver` handle the target needs: created against the canvas, it can
 * only be disconnected.
 */
private external interface CanvasResizeWatch : JsAny {
    fun disconnect()
}

/**
 * Observes [canvas] with a browser `ResizeObserver` and calls [onResize] every time its box
 * changes, returning the watch to disconnect later — or `null` when the browser offers no
 * `ResizeObserver`, leaving the caller on the pull path.
 */
private fun observeCanvasResizes(canvas: HTMLCanvasElement, onResize: () -> Unit): CanvasResizeWatch? = js("""{
    if (typeof globalThis.ResizeObserver !== 'function') return null;
    const observer = new globalThis.ResizeObserver(function () { onResize(); });
    observer.observe(canvas);
    return observer;
}""")

/**
 * A [TargetLease] on an HTML canvas: on top of the generation it was created against, it
 * carries the canvas it was retained for, so surface creation reaches the element.
 */
class WebCanvasLease(
    /**
     * The canvas this lease was retained for.
     */
    val canvas: HTMLCanvasElement,
    /**
     * The [TargetState.generation] this lease was created against.
     */
    override val generation: Long,
) : TargetLease

/**
 * An HTML canvas as a [SurfaceTarget], push-based when the browser allows it: a
 * `ResizeObserver` on the canvas drives the state refresh, so [state] genuinely emits when the
 * page resizes the canvas and acquirers suspended in `withFrame` resume without an unrelated
 * read. When the browser offers no `ResizeObserver`, the target falls back to pull: every read
 * of [state] refreshes the published extent from the canvas' current size.
 *
 * A size change always moves the target to a fresh generation, and in v1 a generation change
 * kills the surfaces leased against the old one: using such a surface fails fast on its next
 * frame, and only a fresh surface pass — releasing and re-leasing through the surface module's
 * `withSurface` — negotiates a new surface. Renegotiating in place is v2 work. The observer
 * lives as long as the target, which in turn lives as long as the page's canvas; the target
 * has no close hook in v1.
 */
class CanvasSurfaceTarget(
    /**
     * The canvas this target presents to.
     */
    val canvas: HTMLCanvasElement,
) : SurfaceTarget {
    private val resizeWatch: CanvasResizeWatch? = observeCanvasResizes(canvas) { refresh() }

    private val mutableState = MutableStateFlow(currentState(generation = 0L))

    /**
     * Hot flow of the latest [TargetState]. Driven by the resize observer when the browser
     * has one; without one, every read refreshes the extent from the canvas' current size,
     * bumping the generation when it changed.
     */
    override val state: StateFlow<TargetState>
        get() {
            if (resizeWatch == null) refresh() // pull fallback without a ResizeObserver
            return mutableState
        }

    /**
     * Retains the canvas with its current size, bound to the generation that size maps to.
     */
    override suspend fun retain(): TargetLease {
        val state = state.value
        return WebCanvasLease(canvas, state.generation)
    }

    /**
     * A no-op: the canvas outlives its leases and owns its own lifetime.
     */
    override suspend fun retire(lease: TargetLease) {
        // nothing to do on web: the canvas owns its lifetime
    }

    /**
     * Re-publishes the target state when the canvas size changed, bumping the generation;
     * an unchanged size keeps the current generation.
     */
    private fun refresh() {
        val current = mutableState.value
        val extent = currentExtent()
        if (current.extent == extent) return
        mutableState.value = current.copy(generation = current.generation + 1L, extent = extent)
    }

    /**
     * Snapshot of the target state at [generation], sized by the canvas; a canvas is
     * always available for drawing.
     */
    private fun currentState(generation: Long): TargetState =
        TargetState(
            generation = generation,
            extent = currentExtent(),
            available = true,
        )

    private fun currentExtent(): Extent2D =
        Extent2D(width = canvas.width.toUInt(), height = canvas.height.toUInt())
}
