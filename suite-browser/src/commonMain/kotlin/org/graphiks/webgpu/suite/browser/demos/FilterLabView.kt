@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlinx.serialization.Serializable
import org.graphiks.webgpu.suite.demos.filterlab.*

@Serializable
internal data class FilterLabTexts(
    val title: String, val description: String, val privacy: String,
    val importImage: String, val embedded: String, val reset: String, val filter: String,
    val grayscale: String, val sepia: String, val invert: String, val blur: String, val sobel: String,
    val intensity: String, val radius: String, val threshold: String, val comparison: String,
    val original: String, val filtered: String, val loading: String, val importing: String,
    val unavailable: String, val failed: String, val lost: String, val reload: String,
    val importFailed: String, val dimensions: String, val reduced: String,
    val lesson: String, val computeHelp: String, val edgeHelp: String, val colorHelp: String,
    val computeShader: String, val renderShader: String, val source: String,
)

internal class FilterLabView(val texts: FilterLabTexts) {
    val container = createElement("section")
    val canvas = createElement("canvas")
    val file = createElement("input")
    val embedded = button("embedded",texts.embedded)
    val reset = button("reset",texts.reset)
    val filter = createElement("select")
    val intensity = range("intensity","0","100","1")
    val radius = range("radius","1","5","1")
    val threshold = range("threshold","0","1","0.01")
    val comparison = range("comparison","0","100","1")
    val reload = button("reload",texts.reload)
    private val status = createElement("p")
    private val sourceInfo = createElement("p")
    private val radiusLabel = label("radius",texts.radius,radius)
    private val thresholdLabel = label("threshold",texts.threshold,threshold)
    private val gpuControls = listOf(file,embedded,reset,filter,intensity,radius,threshold,comparison)

    init {
        elementById("validation")?.hidden=true
        val root=elementById("demo-root") ?: error("Missing demo root")
        root.hidden=false; root.setText("")
        container.setClass("demo filter-demo"); container.setAttribute("data-ready","false")
        container.appendChild(createElement("h1").also { it.setText(texts.title) })
        for(text in listOf(texts.description,texts.privacy)) container.appendChild(createElement("p").also { it.setText(text) })
        canvas.setAttribute("id","filter-canvas"); canvas.setAttribute("aria-label",texts.comparison)
        container.appendChild(createElement("div").also { it.setClass("demo-stage"); it.appendChild(canvas) })
        container.appendChild(createElement("p").also { it.setText(texts.original+" ← | → "+texts.filtered) })
        file.setAttribute("id","filter-file"); file.setAttribute("type","file")
        file.setAttribute("accept","image/png,image/jpeg,image/webp")
        filter.setAttribute("id","filter-mode")
        for((mode,title) in ImageFilter.entries.zip(listOf(texts.grayscale,texts.sepia,texts.invert,texts.blur,texts.sobel))) {
            filter.appendChild(createElement("option").also { it.setAttribute("value",mode.name); it.setText(title) })
        }
        val controls=createElement("div").also { it.setClass("demo-controls") }
        for(node in listOf(label("file",texts.importImage,file),embedded,label("mode",texts.filter,filter),
            label("intensity",texts.intensity,intensity),radiusLabel,thresholdLabel,
            label("comparison",texts.comparison,comparison),reset)) controls.appendChild(node)
        container.appendChild(controls); container.appendChild(sourceInfo)
        val lesson=createElement("details"); lesson.setAttribute("id","filter-lesson")
        lesson.appendChild(createElement("summary").also { it.setText(texts.lesson) })
        for(text in listOf(texts.computeHelp,texts.edgeHelp,texts.colorHelp)) lesson.appendChild(createElement("p").also { it.setText(text) })
        for((title,code) in listOf(texts.computeShader to FilterLabComputeWgsl,texts.renderShader to FilterLabRenderWgsl)) {
            lesson.appendChild(createElement("h2").also { it.setText(title) }); lesson.appendChild(wgslBlock(code))
        }
        container.appendChild(lesson)
        status.setAttribute("id","filter-status"); status.setAttribute("role","status"); status.setClass("demo-status")
        container.appendChild(status); reload.hidden=true; container.appendChild(reload)
        container.appendChild(createElement("a").also {
            it.setAttribute("href","https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/filterlab")
            it.setText(texts.source)
        })
        root.appendChild(container); update(FilterLabControls(),false)
    }
    fun update(controls: FilterLabControls, enabled: Boolean) {
        for(node in gpuControls) node.disabled=!enabled
        filter.value=controls.settings.filter.name
        intensity.value=(controls.settings.intensity*100).toString(); radius.value=controls.settings.blurRadius.toString()
        threshold.value=controls.settings.edgeThreshold.toString(); comparison.value=(controls.comparison*100).toString()
        radiusLabel.hidden=controls.settings.filter != ImageFilter.GaussianBlur
        thresholdLabel.hidden=controls.settings.filter != ImageFilter.Sobel
    }
    fun readSettings() = FilterSettings(ImageFilter.valueOf(filter.value),intensity.value.toFloat()/100,
        radius.value.toInt(),threshold.value.toFloat())
    fun message(text: String, error: Boolean=false) { status.setText(text); status.setAttribute("data-error",error.toString()) }
    fun source(name: String,width: Int,height: Int,reduced: Boolean=false) {
        sourceInfo.setText(name+" — "+texts.dimensions.replace("{width}",width.toString()).replace("{height}",height.toString())+
            if(reduced) " — "+texts.reduced else "")
    }
    private fun label(id: String,text: String,input: DomElement)=createElement("label").also {
        it.setAttribute("for","filter-$id"); it.setText(text); it.appendChild(input)
    }
    private fun range(id: String,min: String,max: String,step: String)=createElement("input").also {
        it.setAttribute("id","filter-$id"); it.setAttribute("type","range"); it.setAttribute("min",min)
        it.setAttribute("max",max); it.setAttribute("step",step)
    }
    private fun button(id: String,text: String)=createElement("button").also {
        it.setAttribute("id","filter-$id"); it.setAttribute("type","button"); it.setText(text)
    }
}
