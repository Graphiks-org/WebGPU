@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.JsNumber
import kotlin.js.unsafeCast
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.browser.HTMLCanvasElement
import org.graphiks.webgpu.suite.demos.toolkit.runToolkitParticles
import org.graphiks.webgpu.toolkit.tck.ToolkitProtocolTck
import org.graphiks.webgpu.toolkit.web.CanvasSurfaceTarget
import org.graphiks.webgpu.toolkit.web.webToolkit

/** Canvas bound of the WebGPU default limits; the page holds no device to read the limit from. */
private const val MaximumCanvasDimension = 8192

/** Frames rendered per animation frame: one bounded toolkit pass — session to present. */
private const val FramesPerPass = 1

/**
 * Consecutive failed passes tolerated before the page stops: the surface lease protocol
 * re-negotiates on every pass, so a single failure (a resize racing a pass) recovers on the
 * next animation frame.
 */
private const val AllowedConsecutiveFailures = 2

private const val SourceUrl =
    "https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos/src/commonMain/kotlin/" +
        "org/graphiks/webgpu/suite/demos/toolkit"

private const val BadgeStyle =
    "margin-left: 0.6rem; vertical-align: middle; font-size: 0.62em; font-weight: 600; " +
        "letter-spacing: 0.08em; text-transform: uppercase; padding: 0.15rem 0.5rem; " +
        "border-radius: 999px; border: 1px solid currentColor; opacity: 0.75;"

/** The texts of the toolkit particle demo, hardcoded for both supported locales. */
internal data class ToolkitTexts(
    val title: String,
    val description: String,
    val badge: String,
    val running: String,
    val failed: String,
    val tckRunning: String,
    val tckPassed: String,
    val tckFailed: String,
    val source: String,
)

private fun toolkitTexts(locale: String): ToolkitTexts = if (locale == "fr") {
    ToolkitTexts(
        title = "Particules — toolkit",
        description = "La même scène portable de particules que la démo « particles », pilotée " +
            "par le toolkit WebGPU générique : chaque frame d'animation ouvre une session, " +
            "négocie la surface du canvas, rend une frame bornée et la présente à travers le " +
            "protocole strict. Ajoutez ?tck=1 pour exécuter les vérifications du protocole " +
            "toolkit dans ce navigateur.",
        badge = "toolkit",
        running = "Rendu via le toolkit…",
        failed = "La démo toolkit a échoué.",
        tckRunning = "Exécution des vérifications du protocole toolkit…",
        tckPassed = "Vérifications du protocole toolkit : réussies.",
        tckFailed = "Échec des vérifications du protocole toolkit :",
        source = "Source",
    )
} else {
    ToolkitTexts(
        title = "Particles — toolkit",
        description = "The same portable particle scene as the particles demo, driven through " +
            "the generic WebGPU toolkit: every animation frame opens a session, negotiates the " +
            "canvas surface, renders one bounded frame and presents it through the strict " +
            "protocol. Add ?tck=1 to run the toolkit protocol checks in this browser.",
        badge = "toolkit",
        running = "Rendering through the toolkit…",
        failed = "The toolkit demo failed.",
        tckRunning = "Running the toolkit protocol checks…",
        tckPassed = "Toolkit protocol checks: pass.",
        tckFailed = "Toolkit protocol checks failed:",
        source = "Source",
    )
}

/**
 * Entry point of the `?demo=toolkit-particles` route, the living integration example of the
 * generic toolkit: the existing particle scene rendered through [runToolkitParticles] against
 * the real web extension, with a small "toolkit" badge marking the code path. `?tck=1` replaces
 * the animation with the toolkit protocol checks, whose report is rendered and published as
 * `graphiksToolkitTckReport` for browser CI. A missing demo container or an unusable WebGPU
 * environment produces a visible diagnostic instead of a blank page.
 */
suspend fun showToolkitParticlesPage(locale: String) {
    ToolkitParticlesPage(toolkitTexts(locale)).start()
}

/**
 * Owns the canvas, the animation loop and the optional protocol-check run of the toolkit demo.
 *
 * The launcher is finite by design, so the page drives it in bounded passes from
 * `requestAnimationFrame`: one pass covers session, surface negotiation, the particle scene
 * and one presented frame, and the next pass starts only after the previous one completed,
 * keeping the loop self-paced. Because each pass rebuilds the scene from its reproducible
 * initial state, the canvas shows that state continuously re-rendered (and re-negotiated after
 * every resize) rather than an evolving simulation; the canvas page remains the demo for
 * watching particles travel. The page stops its loop on `pagehide` and after two consecutive
 * failed passes.
 */
private class ToolkitParticlesPage(private val texts: ToolkitTexts) : AutoCloseable {

    private val scope: CoroutineScope = MainScope()
    private val controller = newAbortController()
    private val extension = webToolkit()
    private val canvasElement = createElement("canvas")
    private val status = createElement("p")

    private var section: DomElement? = null
    private var target: CanvasSurfaceTarget? = null
    private var frameHandle: JsNumber? = null
    private var running = false
    private var consecutiveFailures = 0
    private var closed = false

    suspend fun start() {
        if (!buildDom()) {
            showDemoFailure("The demo container is missing from the page.")
            return
        }
        target = CanvasSurfaceTarget(canvasElement.unsafeCast<HTMLCanvasElement>())
        windowRef().listen("pagehide", controller) { close() }

        if (queryParameter("tck") == "1") {
            runTckChecks()
        } else {
            status.setText(texts.running)
            startLoop()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        stopLoop()
        scope.cancel()
        controller.abort()
    }

    private fun buildDom(): Boolean {
        val root = elementById("demo-root") ?: return false
        elementById("validation")?.hidden = true
        root.hidden = false
        root.setText("")

        val created = createElement("section")
        created.setClass("demo")
        section = created

        val heading = createElement("h1")
        heading.setText(texts.title)
        val badge = createElement("span")
        badge.setText(texts.badge)
        badge.setAttribute("style", BadgeStyle)
        heading.appendChild(badge)
        created.appendChild(heading)

        val description = createElement("p")
        description.setText(texts.description)
        created.appendChild(description)

        val stage = createElement("div")
        stage.setClass("demo-stage")
        canvasElement.setAttribute("id", "toolkit-particles-canvas")
        stage.appendChild(canvasElement)
        created.appendChild(stage)

        status.setClass("demo-status")
        status.setAttribute("id", "demo-status")
        status.setAttribute("role", "status")
        created.appendChild(status)

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

        created.appendChild(nav)
        root.appendChild(created)
        return true
    }

    /**
     * Runs the toolkit protocol checks against the real browser extension and this page's
     * canvas target, then renders the report — one line per failure, or the pass line — and
     * publishes the failures as `graphiksToolkitTckReport` for tooling.
     */
    private suspend fun runTckChecks() {
        status.setText(texts.tckRunning)
        val canvasTarget = target ?: return
        sizeCanvas()
        val report = try {
            ToolkitProtocolTck.runAll(extension, canvasTarget)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            fail(texts.tckFailed + " " + failure.message)
            return
        }
        publishTckReport(Json.encodeToString(report.failures))
        if (report.failures.isEmpty()) {
            status.setText(texts.tckPassed)
        } else {
            status.setAttribute("data-error", "true")
            status.setText(texts.tckFailed)
            val failures = createElement("ul")
            for (failure in report.failures) {
                val item = createElement("li")
                item.setText(failure)
                failures.appendChild(item)
            }
            section?.appendChild(failures)
        }
    }

    private fun startLoop() {
        if (closed || frameHandle != null) return
        frameHandle = requestAnimationFrame(::onFrame)
    }

    private fun stopLoop() {
        val handle = frameHandle ?: return
        frameHandle = null
        cancelAnimationFrame(handle)
    }

    private fun onFrame(timestamp: Double) {
        frameHandle = null
        if (closed) return
        if (running) {
            // The previous pass has not completed; retry on the next animation frame.
            startLoop()
            return
        }
        running = true
        scope.launch {
            try {
                val canvasTarget = target
                if (canvasTarget != null && sizeCanvas()) {
                    runToolkitParticles(extension, canvasTarget, FramesPerPass)
                }
                consecutiveFailures = 0
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                if (++consecutiveFailures >= AllowedConsecutiveFailures) {
                    fail(texts.failed + " " + failure.message)
                    return@launch
                }
            } finally {
                running = false
            }
            startLoop()
        }
    }

    /**
     * Sizes the canvas' drawing buffer from its CSS size and the device pixel ratio, capped by
     * the WebGPU default dimension limit. A zero CSS size (hidden canvas) is skipped: the
     * toolkit suspends frame acquisition while the target extent is zero.
     */
    private fun sizeCanvas(): Boolean {
        val sized = canvasElement.unsafeCast<DomCanvas>()
        val cssWidth = sized.clientWidth
        val cssHeight = sized.clientHeight
        if (cssWidth <= 0 || cssHeight <= 0) return false
        val ratio = pixelRatio()
        val width = min((cssWidth * ratio).toInt(), MaximumCanvasDimension).coerceAtLeast(1)
        val height = min((cssHeight * ratio).toInt(), MaximumCanvasDimension).coerceAtLeast(1)
        if (sized.width != width) sized.width = width
        if (sized.height != height) sized.height = height
        return true
    }

    private fun fail(message: String) {
        if (closed) return
        stopLoop()
        status.setClass("demo-status")
        status.setAttribute("data-error", "true")
        status.setText(message)
    }
}

/** Publishes the toolkit protocol-check failures as a compact JSON array for tooling. */
private fun publishTckReport(failuresJson: String): Unit = js("""{
    globalThis.graphiksToolkitTckReport = failuresJson;
}""")
