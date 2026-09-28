@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.min
import kotlin.js.JsNumber
import kotlin.js.unsafeCast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.browser.Adapter
import org.graphiks.webgpu.browser.CanvasSurface
import org.graphiks.webgpu.browser.HTMLCanvasElement
import org.graphiks.webgpu.browser.SurfaceConfiguration
import org.graphiks.webgpu.browser.getCanvasSurface
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount

private const val MaxDeltaSeconds = 0.05f

/** The localized texts of the particle demo, loaded from `demos/particles.<locale>.json`. */
@Serializable
internal data class ParticleTexts(
    val title: String,
    val description: String,
    val pause: String,
    val resume: String,
    val reset: String,
    val count: String,
    val source: String,
    val unavailable: String,
    val failed: String,
    val loading: String,
)

private val ProposedCounts = listOf(256, 1024, 4096, 16384, 65536)
private const val DefaultParticleCount = 4096
private const val SourceUrl =
    "https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/particles"

/**
 * Entry point of the `?demo=particles` route.
 *
 * Loads the localized texts, then hands the page over to [ParticleDemoPage]. A missing text resource
 * or an unusable WebGPU environment produces a visible diagnostic instead of a blank page.
 */
suspend fun showParticlesPage(locale: String) {
    val texts = try {
        Json.decodeFromString<ParticleTexts>(fetchText("demos/particles.$locale.json"))
    } catch (failure: Throwable) {
        showDemoFailure("Cannot load the demo texts for '$locale': ${failure.message}")
        return
    }
    ParticleDemoPage(texts).start()
}

/** Shows a plain diagnostic in the demo container, used before the page can localize its own messages. */
internal fun showDemoFailure(message: String) {
    val root = elementById("demo-root") ?: return
    elementById("validation")?.hidden = true
    root.hidden = false
    root.setText("")
    val paragraph = createElement("p")
    paragraph.setClass("demo-status")
    paragraph.setAttribute("data-error", "true")
    paragraph.setText(message)
    root.appendChild(paragraph)
}

/**
 * Owns the canvas, the controls and the animation loop of the particle demo.
 *
 * CPU work is limited to building the initial particles once and to resizing: from then on the
 * compute pass updates the storage buffer and the render pass reads it directly as an instanced
 * vertex buffer, without a CPU readback between the two passes. The page closes its GPU resources on
 * `pagehide` and stops the loop on a fatal GPU error.
 */
private class ParticleDemoPage(private val texts: ParticleTexts) : AutoCloseable {

    private val scope: CoroutineScope = MainScope()
    private val controller = newAbortController()

    private val pauseButton = createElement("button")
    private val resetButton = createElement("button")
    private val countSelect = createElement("select")
    private val status = createElement("p")
    private val canvasElement = createElement("canvas")

    private var adapter: Adapter? = null
    private var device: GPUDevice? = null
    private var surface: CanvasSurface? = null
    private var scene: ParticleScene? = null
    private var format: GPUTextureFormat? = null
    private var frameHandle: JsNumber? = null
    private var lastFrameTime: Double? = null
    private var availableCounts: List<Int> = emptyList()
    private var paused = false
    private var transitioning = false
    private var closed = false

    suspend fun start() {
        if (!buildDom()) {
            showDemoFailure("The demo container is missing from the page.")
            return
        }
        registerControls()
        status.setText(texts.loading)

        val failure = initialize()
        if (failure != null) {
            fail(failure)
            return
        }
        status.setText("")
        updatePauseButton()
        renderCurrentFrame(0f)
        startLoop()
    }

    override fun close() {
        if (closed) return
        closed = true
        stopLoop()
        scope.cancel()
        controller.abort()
        closeGpu()
    }

    private fun buildDom(): Boolean {
        val root = elementById("demo-root") ?: return false
        elementById("validation")?.hidden = true
        root.hidden = false
        root.setText("")

        val section = createElement("section")
        section.setClass("demo")

        val heading = createElement("h1")
        heading.setText(texts.title)
        section.appendChild(heading)

        val description = createElement("p")
        description.setText(texts.description)
        section.appendChild(description)

        val stage = createElement("div")
        stage.setClass("demo-stage")
        canvasElement.setAttribute("id", "particles-canvas")
        stage.appendChild(canvasElement)
        section.appendChild(stage)

        val controls = createElement("div")
        controls.setClass("demo-controls")

        pauseButton.setAttribute("type", "button")
        controls.appendChild(pauseButton)

        resetButton.setAttribute("type", "button")
        controls.appendChild(resetButton)

        val countLabel = createElement("label")
        countLabel.setAttribute("for", "demo-count")
        countLabel.setText(texts.count)
        controls.appendChild(countLabel)

        countSelect.setAttribute("id", "demo-count")
        controls.appendChild(countSelect)

        section.appendChild(controls)

        status.setClass("demo-status")
        status.setAttribute("id", "demo-status")
        status.setAttribute("role", "status")
        section.appendChild(status)

        val nav = createElement("nav")
        nav.setClass("demo-nav")

        val validationLink = createElement("a")
        validationLink.setAttribute("href", "./")
        validationLink.setText("Validation")
        nav.appendChild(validationLink)

        val sourceLink = createElement("a")
        sourceLink.setAttribute("href", SourceUrl)
        sourceLink.setAttribute("target", "_blank")
        sourceLink.setAttribute("rel", "noreferrer")
        sourceLink.setText(texts.source)
        nav.appendChild(sourceLink)

        section.appendChild(nav)
        root.appendChild(section)
        return true
    }

    private fun registerControls() {
        pauseButton.listen("click", controller) { togglePause() }
        resetButton.listen("click", controller) { resetParticles() }
        countSelect.listen("change", controller) { onCountChanged() }
        windowRef().listen("resize", controller) { renderCurrentFrame(0f) }
        documentRef().listen("visibilitychange", controller) { lastFrameTime = null }
        windowRef().listen("pagehide", controller) { close() }
    }

    /** Acquires adapter, device, surface and the first scene under a validation scope. */
    private suspend fun initialize(): String? {
        val acquiredAdapter = requestAdapter().getOrElse { return unavailable(it) }
        adapter = acquiredAdapter

        val acquiredDevice = acquiredAdapter.requestDevice(
            DeviceDescriptor(
                onUncapturedError = GPUUncapturedErrorCallback { onUncapturedError(it.message) },
            ),
        ).getOrElse {
            acquiredAdapter.close()
            adapter = null
            return unavailable(it)
        }
        device = acquiredDevice

        var scopeFailure: Throwable? = null
        acquiredDevice.pushErrorScope(GPUErrorFilter.Validation)
        try {
            val acquiredSurface = canvasElement.unsafeCast<HTMLCanvasElement>().getCanvasSurface()
            surface = acquiredSurface
            val preferredFormat = acquiredSurface.preferredCanvasFormat
                ?: error("The canvas does not expose a preferred WebGPU format.")
            format = preferredFormat
            acquiredSurface.configure(SurfaceConfiguration(acquiredDevice, preferredFormat))

            val maximum = maxParticleCount(acquiredDevice.limits)
            availableCounts = ProposedCounts.filter { it <= maximum }
            if (availableCounts.isEmpty()) {
                error("This device cannot render any of the proposed particle counts (maximum $maximum).")
            }
            fillCountOptions()
            val initialCount = availableCounts.lastOrNull { it <= DefaultParticleCount } ?: availableCounts.first()
            scene = ParticleScene.create(acquiredDevice, preferredFormat, initialParticles(initialCount))
            countSelect.value = initialCount.toString()
        } catch (failure: Throwable) {
            scopeFailure = failure
        }
        val validationError = acquiredDevice.popErrorScope().getOrNull()

        return when {
            scopeFailure != null -> {
                closeGpu()
                texts.failed + " " + scopeFailure.message
            }
            validationError != null -> {
                closeGpu()
                texts.failed + " " + validationError.message
            }
            else -> null
        }
    }

    private fun fillCountOptions() {
        countSelect.setText("")
        for (count in availableCounts) {
            val option = createElement("option")
            option.setAttribute("value", count.toString())
            option.setText(count.toString())
            countSelect.appendChild(option)
        }
    }

    private fun togglePause() {
        if (closed || transitioning) return
        paused = !paused
        lastFrameTime = null
        updatePauseButton()
        if (paused) stopLoop() else startLoop()
    }

    private fun updatePauseButton() {
        pauseButton.setText(if (paused) texts.resume else texts.pause)
    }

    private fun resetParticles() {
        if (closed || transitioning) return
        val currentScene = scene ?: return
        currentScene.reset(initialParticles(currentScene.count))
        lastFrameTime = null
        renderCurrentFrame(0f)
    }

    private fun onCountChanged() {
        if (closed || transitioning) return
        val requested = countSelect.value.toIntOrNull() ?: return
        if (requested == scene?.count) return

        transitioning = true
        setControlsEnabled(false)
        stopLoop()
        scope.launch {
            try {
                device?.queue?.onSubmittedWorkDone()?.getOrThrow()
                scene?.close()
                scene = null
                val failure = createScene(requested)
                if (failure != null) {
                    fail(failure)
                    return@launch
                }
                lastFrameTime = null
                renderCurrentFrame(0f)
                transitioning = false
                setControlsEnabled(true)
                if (!paused) startLoop()
            } catch (failure: Throwable) {
                fail(texts.failed + " " + failure.message)
            }
        }
    }

    /** Recreates the scene for [count] under a validation scope, leaving the device and surface in place. */
    private suspend fun createScene(count: Int): String? {
        val currentDevice = device ?: return texts.failed
        val currentFormat = format ?: return texts.failed
        var scopeFailure: Throwable? = null
        currentDevice.pushErrorScope(GPUErrorFilter.Validation)
        try {
            scene = ParticleScene.create(currentDevice, currentFormat, initialParticles(count))
        } catch (failure: Throwable) {
            scopeFailure = failure
        }
        val validationError = currentDevice.popErrorScope().getOrNull()
        return when {
            scopeFailure != null -> texts.failed + " " + scopeFailure.message
            validationError != null -> texts.failed + " " + validationError.message
            else -> null
        }
    }

    private fun startLoop() {
        if (closed || paused || frameHandle != null) return
        frameHandle = requestAnimationFrame(::onFrame)
    }

    private fun stopLoop() {
        val handle = frameHandle ?: return
        frameHandle = null
        cancelAnimationFrame(handle)
    }

    private fun onFrame(timestamp: Double) {
        frameHandle = null
        if (closed || paused) return
        val previous = lastFrameTime
        lastFrameTime = timestamp
        val deltaSeconds = if (previous == null) {
            0f
        } else {
            ((timestamp - previous) / 1000.0).toFloat().coerceIn(0f, MaxDeltaSeconds)
        }
        renderCurrentFrame(deltaSeconds)
        startLoop()
    }

    /**
     * Renders one frame into the canvas.
     *
     * The physical size is derived from the CSS size and the device pixel ratio, capped by the
     * device limit, and a zero CSS size (hidden canvas) is skipped. The current texture is borrowed
     * from the canvas: its view is closed after submission and the texture is never destroyed.
     */
    private fun renderCurrentFrame(deltaSeconds: Float) {
        val currentDevice = device ?: return
        val currentSurface = surface ?: return
        val currentScene = scene ?: return

        val sized = canvasElement.unsafeCast<DomCanvas>()
        val cssWidth = sized.clientWidth
        val cssHeight = sized.clientHeight
        if (cssWidth <= 0 || cssHeight <= 0) return

        val ratio = pixelRatio()
        val maxDimension = currentDevice.limits.maxTextureDimension2D.toInt()
        val width = min((cssWidth * ratio).toInt(), maxDimension).coerceAtLeast(1)
        val height = min((cssHeight * ratio).toInt(), maxDimension).coerceAtLeast(1)
        if (sized.width != width) sized.width = width
        if (sized.height != height) sized.height = height

        val view = currentSurface.getCurrentTexture().texture.createView()
        try {
            val encoder = currentDevice.createCommandEncoder()
            try {
                currentScene.encodeFrame(encoder, view, width, height, deltaSeconds)
                val commandBuffer = encoder.finish()
                try {
                    currentDevice.queue.submit(listOf(commandBuffer))
                } finally {
                    commandBuffer.close()
                }
            } finally {
                encoder.close()
            }
        } finally {
            view.close()
        }
    }

    private fun onUncapturedError(message: String) {
        if (closed) return
        fail(texts.failed + " " + message)
    }

    private fun fail(message: String) {
        if (closed) return
        stopLoop()
        status.setClass("demo-status")
        status.setAttribute("data-error", "true")
        status.setText(message)
        setControlsEnabled(false)
        closeGpu()
    }

    private fun setControlsEnabled(enabled: Boolean) {
        pauseButton.disabled = !enabled
        resetButton.disabled = !enabled
        countSelect.disabled = !enabled
    }

    /** Closes owned GPU resources in reverse creation order; never closes the device itself. */
    private fun closeGpu() {
        scene?.close()
        scene = null
        surface?.close()
        surface = null
        device?.close()
        device = null
        adapter?.close()
        adapter = null
        format = null
    }

    private fun unavailable(failure: Throwable): String =
        texts.unavailable + (failure.message?.let { " $it" } ?: "")
}
