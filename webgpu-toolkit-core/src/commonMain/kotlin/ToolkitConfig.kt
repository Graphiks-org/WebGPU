package org.graphiks.webgpu.toolkit

import org.graphiks.webgpu.GPUPowerPreference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Timeouts guarding the startup and shutdown of a toolkit session.
 *
 */
data class ToolkitConfig(
	/**
	 * How long startup may take before the session gives up.
	 *
	 */
	val startupTimeout: Duration = 10.seconds,
	/**
	 * How long shutdown may take before the session gives up.
	 *
	 */
	val shutdownTimeout: Duration = 10.seconds,
)

/**
 * Hints for adapter selection, mirroring the optional fields of the WebGPU request options;
 * `null` leaves each decision to the implementation.
 *
 */
data class AdapterHints(
	/**
	 * Hint whether adapter selection should favor lower power use or higher performance.
	 *
	 */
	val powerPreference: GPUPowerPreference? = null,
	/**
	 * When not `null`, forces selection of a fallback adapter (`true`) or forbids it (`false`).
	 *
	 */
	val forceFallbackAdapter: Boolean? = null,
)

/**
 * How a toolkit session drives frame work: an implementation either pumps frames itself,
 * runs on an event loop, or leaves the pacing entirely to the caller.
 *
 */
enum class ToolkitProgression {
	/**
	 * The session pumps frame work on its own.
	 *
	 */
	Pumped,
	/**
	 * The session is driven by the host event loop.
	 *
	 */
	EventLoop,
	/**
	 * The caller drives every frame step explicitly.
	 *
	 */
	Manual,
}

/**
 * Alpha mode a surface composites with.
 *
 */
enum class ToolkitAlphaMode {
	/**
	 * Pixels are opaque; the alpha channel is ignored.
	 *
	 */
	Opaque,
	/**
	 * Pixels are premultiplied alpha.
	 *
	 */
	Premultiplied,
}
