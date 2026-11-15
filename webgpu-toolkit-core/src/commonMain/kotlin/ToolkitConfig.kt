package org.graphiks.webgpu.toolkit

import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUPowerPreference
import org.graphiks.webgpu.GPUTextureFormat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Everything a toolkit session is configured with: its timeouts, the device it asks for and
 * how it drives frame work.
 *
 */
data class SessionConfig(
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
	/**
	 * Descriptor handed to `requestDevice`; its required features and limits are what the
	 * session's device is created with. `null` requests the implementation's defaults.
	 *
	 */
	val deviceDescriptor: GPUDeviceDescriptor? = null,
	/**
	 * How the session should drive frame work, or `null` to use the extension's own choice.
	 * An extension that cannot honour the request fails startup instead of silently using
	 * another progression.
	 *
	 */
	val progression: Progression? = null,
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
enum class Progression {
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
enum class AlphaMode {
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
	/**
	 * Pixels are post-multiplied (non-premultiplied) alpha. Offered by native composite alpha
	 * modes; the browser canvas has no equivalent, so the web extension never lists it.
	 *
	 */
	PostMultiplied,
}

/**
 * When presented frames become visible relative to the display refresh.
 *
 */
enum class PresentMode {
	/**
	 * Presentation is synchronized with the display; the queue may grow when rendering is slow.
	 *
	 */
	Fifo,
	/**
	 * Like [Fifo], but a late frame may be shown earlier instead of waiting a full refresh.
	 *
	 */
	FifoRelaxed,
	/**
	 * Frames are shown as soon as they are presented; tearing is possible.
	 *
	 */
	Immediate,
	/**
	 * A presented frame replaces the queued one, so the queue never grows past one frame.
	 *
	 */
	Mailbox,
}

/**
 * How a surface picks its configuration from what a target advertises.
 *
 * A `required` value must be advertised or configuration fails; otherwise the first entry of
 * the matching `preferred` list the target supports wins, and when none matches the target's
 * own preferred value is used. Every list is tried in order, so the first entry is the most
 * wanted.
 *
 */
data class PresentationPolicy(
	/**
	 * Formats tried in order; a pipeline rendering into frames must match the chosen one.
	 *
	 */
	val preferredFormats: List<GPUTextureFormat> = emptyList(),
	/**
	 * A format the target must support; takes precedence over [preferredFormats].
	 *
	 */
	val requiredFormat: GPUTextureFormat? = null,
	/**
	 * Alpha modes tried in order.
	 *
	 */
	val preferredAlphaModes: List<AlphaMode> = emptyList(),
	/**
	 * An alpha mode the target must support; takes precedence over [preferredAlphaModes].
	 *
	 */
	val requiredAlphaMode: AlphaMode? = null,
	/**
	 * Present modes tried in order. Targets that cannot choose one advertise only [PresentMode.Fifo].
	 *
	 */
	val preferredPresentModes: List<PresentMode> = emptyList(),
	/**
	 * A present mode the target must support; takes precedence over [preferredPresentModes].
	 *
	 */
	val requiredPresentMode: PresentMode? = null,
)
