package org.graphiks.webgpu.toolkit.surface

import kotlinx.coroutines.flow.Flow
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.ToolkitSession
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/**
 * Something that happened to a [Window] outside the render loop: the user or platform asked to
 * close it, or it was resized. Input events are out of scope for v1.
 */
sealed interface WindowEvent {
    /**
     * The user or platform asked to close the window; hosts decide what the request means for
     * the window's lifetime.
     */
    data object CloseRequested : WindowEvent

    /**
     * The window was resized to [logical] logical pixels; the window republishes its target
     * state with the matching physical extent.
     */
    data class Resized(val logical: Extent2D) : WindowEvent
}

/**
 * Whether a windowing host owns the native windows it opens or borrows ones provided by an
 * embedder. A host that owns its windows tears them down when the composition helpers exit; a
 * borrowed window's lifetime belongs to whoever lent it.
 */
enum class WindowRuntime {
    /**
     * The host created the native window and destroys it when [withWindow] exits.
     */
    Owned,

    /**
     * The window was lent by an embedder; [withWindow] leaves it open on exit.
     */
    Borrowed,
}

/**
 * Parameters for opening a [Window]: its title and logical size in pixels. The physical extent
 * the window publishes through its target state is the platform's mapping of this logical size.
 */
data class WindowConfig(
    /**
     * Title shown with the window's frame, where the platform has one.
     */
    val title: String = "webgpu-toolkit",
    /**
     * Requested width in logical pixels.
     */
    val logicalWidth: Int = 1280,
    /**
     * Requested height in logical pixels.
     */
    val logicalHeight: Int = 720,
)

/**
 * A windowing-neutral window: a [SurfaceTarget] whose lifecycle a [WindowHost] opened — it
 * publishes target state for the lease protocol, and additionally publishes [events] such as
 * close requests and resizes, and reports whether its host owns it. Closing an
 * [WindowRuntime.Owned] window destroys it; a [WindowRuntime.Borrowed] window ignores [close],
 * its lifetime belonging to the embedder that lent it.
 */
interface Window : SurfaceTarget, AutoCloseable {
    /**
     * Hot flow of the events this window publishes: [WindowEvent.CloseRequested] when the user
     * or platform asked to close it, [WindowEvent.Resized] when its size changed.
     */
    val events: Flow<WindowEvent>

    /**
     * Whether [WindowHost.open] created this window or an embedder lent it.
     */
    val runtime: WindowRuntime
}

/**
 * Opens windows for the windowing-neutral [Window] contract. Concrete windowing systems live in
 * provider repositories — GLFW and any other backend are provider artifacts; nothing in this
 * repository names one. Hosts advertise through [runtime] whether the windows they open are
 * theirs to destroy or borrowed from an embedder.
 */
interface WindowHost {
    /**
     * Whether the windows this host opens are [WindowRuntime.Owned] or [WindowRuntime.Borrowed].
     */
    val runtime: WindowRuntime

    /**
     * Opens a window with [config] that publishes target state and events.
     */
    suspend fun open(config: WindowConfig = WindowConfig()): Window
}

/**
 * Opens a window on [host] with [config], runs [withSurface] against that window as the
 * presentation target, and runs [block] with the session, the window and the negotiated
 * surface. On every exit path — normal return, throw or cancellation — the window is closed
 * when its [Window.runtime] is [WindowRuntime.Owned]; a [WindowRuntime.Borrowed] window stays
 * open, its lifetime belonging to the embedder that lent it.
 */
suspend fun <R> withWindow(
    extension: WebGpuToolkitExtension,
    host: WindowHost,
    config: WindowConfig = WindowConfig(),
    hints: AdapterHints = AdapterHints(),
    block: suspend (ToolkitSession, Window, ToolkitSurface) -> R,
): R {
    val window = host.open(config)
    try {
        return withSurface(extension, window, hints) { session, surface ->
            block(session, window, surface)
        }
    } finally {
        if (window.runtime == WindowRuntime.Owned) {
            window.close()
        }
    }
}
