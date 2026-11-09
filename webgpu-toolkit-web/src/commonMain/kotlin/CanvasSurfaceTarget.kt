@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.toolkit.web

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.webgpu.browser.HTMLCanvasElement
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState

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
 * An HTML canvas as a [SurfaceTarget]: the page resizes the canvas under the embedder's
 * control, so every read of [state] refreshes the published extent from the canvas'
 * current size and moves to a fresh generation exactly when that size changed.
 */
class CanvasSurfaceTarget(
    /**
     * The canvas this target presents to.
     */
    val canvas: HTMLCanvasElement,
) : SurfaceTarget {
    private val mutableState = MutableStateFlow(currentState(generation = 0L))

    /**
     * Hot flow of the latest [TargetState]; every read refreshes the extent from the
     * canvas' current size, bumping the generation when it changed.
     */
    override val state: StateFlow<TargetState>
        get() {
            refresh()
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
