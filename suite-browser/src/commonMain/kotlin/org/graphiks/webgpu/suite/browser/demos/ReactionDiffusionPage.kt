@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.JsNumber
import kotlin.js.unsafeCast
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.browser.Adapter
import org.graphiks.webgpu.browser.CanvasSurface
import org.graphiks.webgpu.browser.HTMLCanvasElement
import org.graphiks.webgpu.browser.SurfaceConfiguration
import org.graphiks.webgpu.browser.getCanvasSurface
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.browser.setDocumentLang
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionBrush
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDiffusionScene
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDisplay
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPalette
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionParameters
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset

suspend fun showReactionDiffusionPage(locale: String) = coroutineScope {
    val bootstrapController = newAbortController()
    var closed = false
    val loading = launch(start = CoroutineStart.LAZY) {
        try {
            val texts = Json.decodeFromString<ReactionTexts>(fetchText("demos/reaction-diffusion.$locale.json"))
            if (closed) return@launch
            setDocumentLang(locale)
            ReactionDemoPage(ReactionDiffusionView(texts)).start()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            if (!closed) showDemoFailure("Cannot load the reaction-diffusion page for '$locale': ${failure.message}")
        }
    }
    windowRef().listen("pagehide", bootstrapController) { closed = true; loading.cancel() }
    try {
        loading.join()
    } finally {
        bootstrapController.abort()
    }
}

private class ReactionDemoPage(private val view: ReactionDiffusionView) : AutoCloseable {
    private val controls = ReactionControls()
    private val scope = MainScope()
    private val controller = newAbortController()
    private var adapter: Adapter? = null
    private var device: GPUDevice? = null
    private var surface: CanvasSurface? = null
    private var scene: ReactionDiffusionScene? = null
    private var frameHandle: JsNumber? = null
    private var pointerId: Int? = null
    private var pendingBrush: ReactionBrush? = null
    private var ready = false
    private var failed = false
    private var closed = false

    fun start() {
        registerControls()
        view.status.setText(view.texts.loading)
        scope.launch {
            try { initialize() } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) { fail(view.texts.failed + " " + failure.message) }
        }
    }

    private suspend fun initialize() {
        val acquiredAdapter = requestAdapter().getOrElse { fail(view.texts.unavailable + " " + it.message); return }
        if (closed || failed) { acquiredAdapter.close(); return }
        adapter = acquiredAdapter
        val acquiredDevice = acquiredAdapter.requestDevice(DeviceDescriptor(
            onUncapturedError = GPUUncapturedErrorCallback { fail(view.texts.failed + " " + it.message) },
        )).getOrElse { fail(view.texts.unavailable + " " + it.message); return }
        if (closed || failed) { acquiredDevice.close(); return }
        device = acquiredDevice
        scope.launch {
            try {
                val lost = acquiredDevice.awaitLost().getOrThrow()
                if (!closed && !failed) fail(view.texts.lost + " " + lost.message)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Throwable) { fail(view.texts.failed + " " + failure.message) }
        }
        acquiredDevice.pushErrorScope(GPUErrorFilter.Validation)
        var creationFailure: Throwable? = null
        try {
            val acquiredSurface = view.canvas.unsafeCast<HTMLCanvasElement>().getCanvasSurface()
            surface = acquiredSurface
            val format = acquiredSurface.preferredCanvasFormat ?: error("Missing preferred canvas format")
            acquiredSurface.configure(SurfaceConfiguration(acquiredDevice, format))
            scene = ReactionDiffusionScene.create(acquiredDevice, format)
        } catch (failure: Throwable) { creationFailure = failure }
        val validationError = acquiredDevice.popErrorScope().getOrThrow()
        if (closed || failed) return
        creationFailure?.let { throw it }
        if (validationError != null) error(validationError.message)
        ready = true
        view.status.setText("")
        view.update(controls, true)
        renderFrame(advance = false)
        if (!failed) {
            view.container.setAttribute("data-ready", "true")
            requestFrame()
        }
    }

    private fun registerControls() {
        view.pause.listen("click", controller) {
            if (ready) { controls.togglePause(); view.update(controls, true); stopLoop(); requestFrame() }
        }
        view.step.listen("click", controller) { if (ready) { controls.requestStep(); requestFrame() } }
        view.reset.listen("click", controller) { if (ready) reset() }
        view.preset.listen("change", controller) {
            if (ready) {
                controls.selectPreset(ReactionPreset.valueOf(view.preset.value))
                view.update(controls, true)
                reset()
            }
        }
        view.speed.listen("input", controller) { if (ready) { controls.speed = view.speed.value.toInt(); requestFrame() } }
        fun parametersChanged() {
            if (!ready) return
            controls.parameters = ReactionParameters(view.feed.value.toFloat(), view.kill.value.toFloat())
            requestFrame()
        }
        view.feed.listen("input", controller) { parametersChanged() }
        view.kill.listen("input", controller) { parametersChanged() }
        view.palette.listen("change", controller) {
            if (ready) { controls.palette = ReactionPalette.valueOf(view.palette.value); requestFrame() }
        }
        view.display.listen("change", controller) {
            if (ready) { controls.display = ReactionDisplay.valueOf(view.display.value); requestFrame() }
        }
        view.reload.listen("click", controller) { reloadReactionPage() }
        windowRef().listen("resize", controller) { requestFrame() }
        windowRef().listen("pagehide", controller) { close() }
        registerPointers()
    }

    private fun reset() {
        controls.clearPendingStep()
        pendingBrush = null
        try { scene?.reset(); requestFrame() } catch (failure: Throwable) { fail(view.texts.failed + " " + failure.message) }
    }

    private fun registerPointers() {
        val canvas = view.canvas.unsafeCast<ReactionPointerCanvas>()
        fun update(event: ReactionPointerEvent) {
            val rect = canvas.getBoundingClientRect()
            pendingBrush = reactionBrushAt(event.clientX, event.clientY, rect.left, rect.top, rect.width, rect.height)
            requestFrame()
        }
        view.canvas.listen("pointerdown", controller) { raw ->
            val event = raw.unsafeCast<ReactionPointerEvent>()
            if (ready && pointerId == null && event.button == 0) {
                canvas.setPointerCapture(event.pointerId)
                pointerId = event.pointerId
                update(event)
            }
        }
        view.canvas.listen("pointermove", controller) { raw ->
            val event = raw.unsafeCast<ReactionPointerEvent>()
            if (ready && pointerId == event.pointerId) update(event)
        }
        for (name in listOf("pointerup", "pointercancel", "lostpointercapture")) view.canvas.listen(name, controller) { raw ->
            val event = raw.unsafeCast<ReactionPointerEvent>()
            if (pointerId == event.pointerId) {
                if (name == "pointercancel") pendingBrush = null
                pointerId = null
                if (canvas.hasPointerCapture(event.pointerId)) canvas.releasePointerCapture(event.pointerId)
            }
        }
    }

    private fun requestFrame() {
        if (!ready || closed || failed || frameHandle != null) return
        frameHandle = requestAnimationFrame {
            frameHandle = null
            if (!ready || closed || failed) return@requestAnimationFrame
            try { renderFrame() } catch (failure: Throwable) { fail(view.texts.failed + " " + failure.message) }
            if (ready && !controls.paused) requestFrame()
        }
    }

    private fun renderFrame(advance: Boolean = true) {
        val currentDevice = device ?: return
        val currentScene = scene ?: return
        val currentSurface = surface ?: return
        val canvas = view.canvas.unsafeCast<DomCanvas>()
        if (canvas.clientWidth <= 0 || canvas.clientHeight <= 0 || !reactionCanvasVisible(view.canvas)) return
        val maxDimension = currentDevice.limits.maxTextureDimension2D.toInt()
        val width = min((canvas.clientWidth * pixelRatio()).toInt(), maxDimension).coerceAtLeast(1)
        val height = min((canvas.clientHeight * pixelRatio()).toInt(), maxDimension).coerceAtLeast(1)
        if (canvas.width != width) canvas.width = width
        if (canvas.height != height) canvas.height = height
        val steps = if (advance) controls.takeSteps() else 0
        val brush = pendingBrush
        pendingBrush = null
        currentSurface.getCurrentTexture().texture.createView().use { target ->
            currentDevice.createCommandEncoder().use { encoder ->
                currentScene.encodeFrame(encoder, target, width, height, steps, controls.parameters,
                    controls.palette, controls.display, brush)
                encoder.finish().use { currentDevice.queue.submit(listOf(it)) }
            }
        }
    }

    private fun stopLoop() {
        frameHandle?.let { cancelAnimationFrame(it) }
        frameHandle = null
    }

    private fun fail(message: String) {
        if (closed || failed) return
        failed = true
        ready = false
        stopLoop()
        releasePointer()
        view.container.setAttribute("data-ready", "false")
        view.status.setAttribute("data-error", "true")
        view.status.setText(message)
        view.reload.hidden = false
        view.update(controls, false)
        scope.cancel()
        closeGpu()
    }

    private fun releasePointer() {
        val canvas = view.canvas.unsafeCast<ReactionPointerCanvas>()
        val id = pointerId
        pointerId = null
        pendingBrush = null
        if (id != null && canvas.hasPointerCapture(id)) canvas.releasePointerCapture(id)
    }

    override fun close() {
        if (closed) return
        closed = true
        ready = false
        view.container.setAttribute("data-ready", "false")
        stopLoop()
        releasePointer()
        controller.abort()
        scope.cancel()
        closeGpu()
    }

    private fun closeGpu() {
        scene?.close(); scene = null
        surface?.close(); surface = null
        device?.close(); device = null
        adapter?.close(); adapter = null
    }
}
