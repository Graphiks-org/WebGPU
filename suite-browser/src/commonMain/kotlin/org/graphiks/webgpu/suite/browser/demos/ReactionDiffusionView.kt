package org.graphiks.webgpu.suite.browser.demos

import kotlinx.serialization.Serializable
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDisplay
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPalette
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionSimulationShader
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionBrushShader
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionRenderShader

@Serializable
internal data class ReactionTexts(
    val title: String, val description: String, val paint: String,
    val pause: String, val resume: String, val step: String, val reset: String,
    val preset: String, val presetLabels: List<String>, val speed: String,
    val feed: String, val kill: String, val palette: String, val paletteLabels: List<String>,
    val display: String, val displayLabels: List<String>, val source: String,
    val loading: String, val unavailable: String, val failed: String, val lost: String, val reload: String,
    val lesson: String, val substances: String, val parametersHelp: String, val pingPong: String,
    val brushHelp: String, val colorHelp: String, val speedHelp: String,
    val simulationShader: String, val brushShader: String, val renderShader: String,
)

/** Only DOM construction and localized presentation; GPU lifecycle lives in the page. */
internal class ReactionDiffusionView(val texts: ReactionTexts) {
    val root = elementById("demo-root") ?: error("The demo container is missing from the page.")
    val container = createElement("section")
    val canvas = createElement("canvas")
    val pause = button("pause", texts.pause)
    val step = button("step", texts.step)
    val reset = button("reset", texts.reset)
    val preset = select("preset", ReactionPreset.entries.map { it.name }, texts.presetLabels)
    val speed = range("speed", "1", "16", "1")
    val feed = range("feed", "0", "0.1", "0.0001")
    val kill = range("kill", "0", "0.1", "0.0001")
    val palette = select("palette", ReactionPalette.entries.map { it.name }, texts.paletteLabels)
    val display = select("display", ReactionDisplay.entries.map { it.name }, texts.displayLabels)
    val status = createElement("p")
    val reload = button("reload", texts.reload)
    private val gpuControls = listOf(pause, step, reset, preset, speed, feed, kill, palette, display)

    init {
        elementById("validation")?.hidden = true
        root.hidden = false
        root.setText("")
        container.setClass("demo reaction-demo")
        container.setAttribute("data-ready", "false")
        val title = createElement("h1").also { it.setText(texts.title) }
        container.appendChild(title)
        container.appendChild(createElement("p").also { it.setText(texts.description) })
        container.appendChild(createElement("p").also { it.setText(texts.paint) })
        canvas.setAttribute("id", "reaction-canvas")
        canvas.setAttribute("aria-label", texts.paint)
        container.appendChild(createElement("div").also { it.setClass("demo-stage"); it.appendChild(canvas) })
        val buttons = createElement("div").also { it.setClass("demo-controls") }
        for (button in listOf(pause, step, reset)) buttons.appendChild(button)
        container.appendChild(buttons)
        val controls = createElement("div").also { it.setClass("demo-controls") }
        for ((name, label, input) in listOf(
            Triple("preset", texts.preset, preset), Triple("speed", texts.speed, speed),
            Triple("feed", texts.feed, feed), Triple("kill", texts.kill, kill),
            Triple("palette", texts.palette, palette), Triple("display", texts.display, display),
        )) {
            val element = createElement("label")
            element.setAttribute("for", "reaction-$name")
            element.setText(label)
            element.appendChild(input)
            controls.appendChild(element)
        }
        container.appendChild(controls)
        appendLesson()
        status.setAttribute("id", "reaction-status")
        status.setAttribute("role", "status")
        status.setClass("demo-status")
        container.appendChild(status)
        reload.hidden = true
        container.appendChild(reload)
        val source = createElement("a")
        source.setAttribute("href", "https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/reactiondiffusion")
        source.setText(texts.source)
        container.appendChild(source)
        root.appendChild(container)
        update(ReactionControls(), false)
    }

    fun update(controls: ReactionControls, enabled: Boolean) {
        for (element in gpuControls) element.disabled = !enabled
        step.disabled = !enabled || !controls.paused
        pause.setText(if (controls.paused) texts.resume else texts.pause)
        speed.value = controls.speed.toString()
        feed.value = controls.parameters.feed.toString()
        kill.value = controls.parameters.kill.toString()
        palette.value = controls.palette.name
        display.value = controls.display.name
    }

    private fun appendLesson() {
        val lesson = createElement("details")
        lesson.setAttribute("id", "reaction-lesson")
        lesson.appendChild(createElement("summary").also { it.setText(texts.lesson) })
        for (text in listOf(texts.substances, texts.parametersHelp, texts.pingPong, texts.brushHelp,
            texts.colorHelp, texts.speedHelp)) {
            lesson.appendChild(createElement("p").also { it.setText(text) })
        }
        for ((label, shader) in listOf(texts.simulationShader to ReactionSimulationShader,
            texts.brushShader to ReactionBrushShader, texts.renderShader to ReactionRenderShader)) {
            lesson.appendChild(createElement("h2").also { it.setText(label) })
            val code = createElement("code").also { it.setText(shader) }
            lesson.appendChild(createElement("pre").also { it.appendChild(code) })
        }
        container.appendChild(lesson)
    }

    private fun button(name: String, text: String) = createElement("button").also {
        it.setAttribute("id", "reaction-$name"); it.setAttribute("type", "button"); it.setText(text)
    }
    private fun range(name: String, min: String, max: String, step: String) = createElement("input").also {
        it.setAttribute("id", "reaction-$name"); it.setAttribute("type", "range")
        it.setAttribute("min", min); it.setAttribute("max", max); it.setAttribute("step", step)
    }
    private fun select(name: String, values: List<String>, labels: List<String>) = createElement("select").also { select ->
        require(values.size == labels.size) { "Missing labels for $name" }
        select.setAttribute("id", "reaction-$name")
        for ((value, label) in values.zip(labels)) select.appendChild(createElement("option").also {
            it.setAttribute("value", value); it.setText(label)
        })
    }
}
