package org.graphiks.webgpu.toolkit

import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView

/**
 * Formats and alpha modes a surface target can present with, led by the format the
 * implementation prefers for the target.
 *
 */
data class SurfaceCapabilities(
	/**
	 * Format the implementation recommends configuring the surface with.
	 *
	 */
	val preferredFormat: GPUTextureFormat,
	/**
	 * Every format the target can be configured with.
	 *
	 */
	val formats: List<GPUTextureFormat>,
	/**
	 * Every alpha mode the target can composite with.
	 *
	 */
	val alphaModes: List<ToolkitAlphaMode>,
)

/**
 * How a surface is bound to a device before frames are acquired from it.
 *
 */
data class PresentationConfiguration(
	/**
	 * Device that will render into the surface.
	 *
	 */
	val device: GPUDevice,
	/**
	 * Format of the swap chain textures, chosen from [SurfaceCapabilities.formats].
	 *
	 */
	val format: GPUTextureFormat,
	/**
	 * Alpha mode the surface composites with, chosen from [SurfaceCapabilities.alphaModes].
	 *
	 */
	val alphaMode: ToolkitAlphaMode = ToolkitAlphaMode.Opaque,
)

/**
 * One texture taken from a surface, ready to be rendered into and presented.
 *
 */
data class AcquiredFrame(
	/**
	 * View of the swap chain texture to render the frame into.
	 *
	 */
	val view: GPUTextureView,
	/**
	 * Physical size of the texture, matching the target extent at acquisition time.
	 *
	 */
	val extent: Extent2D,
)

/**
 * A drawable surface obtained from a [ToolkitInstance] for one retained [TargetLease].
 *
 */
interface ToolkitSurfaceHandle : AutoCloseable {
	/**
	 * Formats and alpha modes the surface supports.
	 *
	 */
	val capabilities: SurfaceCapabilities
	/**
	 * Binds the surface to a device with the chosen format and alpha mode.
	 *
	 */
	fun configure(configuration: PresentationConfiguration)
	/**
	 * Takes the next frame texture, suspending while the target is unavailable or its extent
	 * is zero.
	 *
	 */
	suspend fun acquireFrame(): AcquiredFrame
	/**
	 * Shows a rendered frame on the target.
	 *
	 */
	fun present(frame: AcquiredFrame)
	/**
	 * Returns a frame after [present] or on abandonment; a no-op on backends with
	 * browser-owned textures.
	 *
	 */
	fun releaseFrame(frame: AcquiredFrame)
}

/**
 * One live WebGPU session opened by a [WebGpuToolkitExtension]: it selects an adapter,
 * hands out surfaces for retained targets, and drives host events according to
 * [progression].
 *
 */
interface ToolkitInstance : AutoCloseable {
	/**
	 * How this session drives frame work.
	 *
	 */
	val progression: ToolkitProgression
	/**
	 * Selects an adapter for the session, or fails with a `Result` describing why none fits.
	 *
	 */
	suspend fun requestAdapter(hints: AdapterHints = AdapterHints()): Result<GPUAdapter>
	/**
	 * Selects an adapter that can present to that exact retained target (dawn4k semantics).
	 *
	 */
	suspend fun requestAdapter(forTarget: TargetLease, hints: AdapterHints = AdapterHints()): Result<GPUAdapter>
	/**
	 * Creates a surface for the retained target of [lease], to be drawn to with [adapter].
	 *
	 */
	fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle
	/**
	 * Drives one round of host events; how much work a call performs follows [progression].
	 *
	 */
	fun pumpEvents()
}

/**
 * Entry point of the toolkit: opens a [ToolkitInstance] from a [ToolkitConfig].
 * Implementations live in binding repositories; this repository ships only the web one.
 *
 */
interface WebGpuToolkitExtension {
	/**
	 * Identifier of the binding this extension fronts, such as `"web"` or `"dawn4k"`.
	 *
	 */
	val id: String
	/**
	 * Opens a toolkit session with the given configuration.
	 *
	 */
	fun open(config: ToolkitConfig = ToolkitConfig()): ToolkitInstance
}
