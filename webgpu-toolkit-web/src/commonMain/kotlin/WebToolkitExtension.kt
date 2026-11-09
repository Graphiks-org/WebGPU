@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.toolkit.web

import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPURequestAdapterOptions
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.browser.CanvasSurface
import org.graphiks.webgpu.browser.GPUCanvasAlphaMode
import org.graphiks.webgpu.browser.SurfaceConfiguration
import org.graphiks.webgpu.browser.getCanvasSurface
import org.graphiks.webgpu.browser.requestAdapter as requestBrowserAdapter
import org.graphiks.webgpu.descriptors.RequestAdapterOptions
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.ToolkitAlphaMode
import org.graphiks.webgpu.toolkit.ToolkitConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.ToolkitProgression
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/**
 * The web [WebGpuToolkitExtension]: it opens sessions driven by the browser event loop and
 * presents to HTML canvas targets through canvas surface handles.
 */
class WebToolkitExtension : WebGpuToolkitExtension {
    /**
     * Identifier of the browser binding this extension fronts: `"web"`.
     */
    override val id: String = "web"

    /**
     * Opens a toolkit session; the browser environment holds no session-wide resource, so
     * every opened instance is stateless until adapters or surfaces are requested.
     */
    override fun open(config: ToolkitConfig): ToolkitInstance = WebToolkitInstance()
}

/**
 * Returns the web toolkit extension fronting the browser WebGPU implementation.
 */
fun webToolkit(): WebToolkitExtension = WebToolkitExtension()

/**
 * Maps [hints] onto the request options handed to the browser: the feature level keeps its
 * descriptor default — `Core`, the level the browser tests exercise — while the power
 * preference and fallback hints are forwarded, with unset hints left at their WebGPU
 * defaults.
 */
internal fun toRequestAdapterOptions(hints: AdapterHints): GPURequestAdapterOptions =
    RequestAdapterOptions(
        powerPreference = hints.powerPreference,
        forceFallbackAdapter = hints.forceFallbackAdapter ?: false,
    )

/**
 * The only [ToolkitInstance] of the web extension: the browser event loop drives events,
 * the browser picks the adapter its canvases can present to, and presentation is owned by
 * the browser, so only surface creation carries web specifics.
 */
private class WebToolkitInstance : ToolkitInstance {
    /**
     * Frame work is driven by the browser event loop for every web session.
     */
    override val progression: ToolkitProgression = ToolkitProgression.EventLoop

    override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> =
        requestBrowserAdapter(toRequestAdapterOptions(hints))

    /**
     * The browser itself picks the adapter its canvases present with, so the lease only
     * documents which canvas the adapter is wanted for and selection delegates to
     * [requestAdapter].
     */
    override suspend fun requestAdapter(forTarget: TargetLease, hints: AdapterHints): Result<GPUAdapter> =
        requestAdapter(hints)

    /**
     * Creates a surface for the canvas of [lease], which must be a [WebCanvasLease]; the
     * adapter is the browser's own and carries no additional binding.
     */
    override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle {
        val canvasLease = lease as? WebCanvasLease
            ?: error("the web toolkit only creates surfaces for canvas leases, got ${lease::class.simpleName}")
        return WebCanvasSurfaceHandle(canvasLease.canvas.getCanvasSurface())
    }

    /**
     * The browser event loop already delivers every host event; nothing is pumped.
     */
    override fun pumpEvents() {
        // nothing to do on web
    }

    /**
     * The instance holds no browser-side resource of its own; only its surfaces do, and
     * their owners close them.
     */
    override fun close() {
        // nothing to do on web
    }
}

/**
 * The only [ToolkitSurfaceHandle] of the web extension: it wraps one [CanvasSurface]. The
 * browser owns the textures the canvas hands out, so acquired frames need no explicit
 * presentation or release.
 */
internal class WebCanvasSurfaceHandle(
    /**
     * The configured canvas context this handle draws to.
     */
    private val surface: CanvasSurface,
) : ToolkitSurfaceHandle {
    /**
     * The canvas' preferred format — falling back to `RGBA8Unorm` when the browser reports
     * none — as the only supported format, alongside the alpha compositing modes the browser
     * canvas supports (opaque and premultiplied; the native-only post-multiplied mode is never
     * offered here).
     */
    override val capabilities: SurfaceCapabilities
        get() {
            val preferred = surface.preferredCanvasFormat ?: GPUTextureFormat.RGBA8Unorm
            return SurfaceCapabilities(
                preferredFormat = preferred,
                formats = listOf(preferred),
                alphaModes = listOf(
                    ToolkitAlphaMode.Opaque,
                    ToolkitAlphaMode.Premultiplied,
                ),
            )
        }

    /**
     * Binds the canvas to the configuration's device with its format and alpha mode;
     * usage, view formats and color space keep their browser defaults.
     */
    override fun configure(configuration: PresentationConfiguration) {
        surface.configure(
            SurfaceConfiguration(
                device = configuration.device,
                format = configuration.format,
                alphaMode = when (configuration.alphaMode) {
                    ToolkitAlphaMode.Opaque -> GPUCanvasAlphaMode.Opaque
                    ToolkitAlphaMode.Premultiplied -> GPUCanvasAlphaMode.Premultiplied
                    ToolkitAlphaMode.PostMultiplied ->
                        error("The browser canvas does not support post-multiplied alpha.")
                },
            ),
        )
    }

    /**
     * Takes the canvas' current texture as a view, sized by the canvas; the browser hands
     * out a fresh browser-owned texture per frame.
     */
    override suspend fun acquireFrame(): AcquiredFrame =
        AcquiredFrame(
            view = surface.getCurrentTexture().texture.createView(),
            extent = Extent2D(width = surface.width, height = surface.height),
        )

    /**
     * Presentation is handled by the browser once the frame's passes are submitted.
     */
    override fun present(frame: AcquiredFrame) {
        // nothing to do on web: the browser presents its own textures
    }

    /**
     * A no-op: the frame's texture is owned by the browser, never by this handle.
     */
    override fun releaseFrame(frame: AcquiredFrame) {
        // nothing to do on web: the browser owns frame textures
    }

    /**
     * Unconfigures the canvas context; the device it was configured with is not owned by
     * the surface and is not closed.
     */
    override fun close() {
        surface.close()
    }
}
