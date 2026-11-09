package org.graphiks.webgpu.toolkit

import kotlinx.coroutines.flow.StateFlow

/**
 * Width and height of a render target surface.
 *
 */
data class Extent2D(val width: UInt, val height: UInt)

/**
 * Snapshot of the lifecycle state of a [SurfaceTarget] published through its state flow.
 *
 */
data class TargetState(
	/**
	 * Counter that is fresh whenever the native handle changes identity.
	 *
	 */
	val generation: Long,
	/**
	 * Size of the target in physical pixels.
	 *
	 */
	val extent: Extent2D,
	/**
	 * Whether the target can currently be retained for drawing.
	 *
	 */
	val available: Boolean,
)

/**
 * A retained lease on a surface target, valid until it is handed back to [SurfaceTarget.retire].
 * It is an open interface so platform extensions can attach their own handle data; the web
 * extension adds the canvas it drew on.
 *
 */
interface TargetLease {
	/**
	 * The [TargetState.generation] this lease was created against.
	 *
	 */
	val generation: Long
}

/**
 * A drawable destination whose lifecycle is tracked by generations: the target reports its
 * current [TargetState], hands out leases with [retain] while it is available, and reclaims a
 * lease with [retire].
 *
 */
interface SurfaceTarget {
	/**
	 * Hot flow of the latest [TargetState] of this target.
	 *
	 */
	val state: StateFlow<TargetState>

	/**
	 * Retains the target for drawing and returns a [TargetLease] bound to the current generation.
	 *
	 */
	suspend fun retain(): TargetLease

	/**
	 * Returns a lease obtained from [retain], releasing the target for its next lifecycle step.
	 *
	 */
	suspend fun retire(lease: TargetLease)
}
