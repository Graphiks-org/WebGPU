@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.unsafeCast
import kotlinx.serialization.Serializable
import org.graphiks.webgpu.suite.demos.instancedcity.*

@Serializable
internal data class CityTexts(val title: String,val description: String,val gesture: String,
    val density: String,val height: String,val rotation: String,val distance: String,val resetView: String,
    val loading: String,val unavailable: String,val failed: String,val lost: String,val reload: String,
    val source: String,val lesson: String,val instancingHelp: String,val depthHelp: String,
    val proceduralHelp: String,val drawHelp: String,val buildingShader: String,val groundShader: String)

/** Localized DOM only. No device ownership or animation in this view. */
internal class CityView(val texts: CityTexts) {
    val root=elementById("demo-root") ?: error("Missing demo container")
    val container=createElement("section")
    val canvas=createElement("canvas")
    val density=createElement("select")
    val height=range("height","0.5","2","0.1")
    val rotation=createElement("input")
    val distance=range("distance","100","300","1")
    val reset=button("reset-view",texts.resetView)
    val status=createElement("p")
    val reload=button("reload",texts.reload)
    private val counts=createElement("p")
    private val gpuControls=listOf(density,height,rotation,distance,reset)

    init {
        elementById("validation")?.hidden=true; root.hidden=false; root.setText("")
        container.setClass("demo city-demo"); container.setAttribute("data-ready","false")
        container.appendChild(createElement("h1").also { it.setText(texts.title) })
        for(text in listOf(texts.description,texts.gesture)) container.appendChild(createElement("p").also { it.setText(text) })
        canvas.setAttribute("id","city-canvas"); canvas.setAttribute("aria-label",texts.gesture)
        container.appendChild(createElement("div").also { it.setClass("demo-stage"); it.appendChild(canvas) })
        density.setAttribute("id","city-density")
        rotation.setAttribute("id","city-rotation"); rotation.setAttribute("type","checkbox")
        val controls=createElement("div").also { it.setClass("demo-controls") }
        for((id,text,input) in listOf(Triple("density",texts.density,density),Triple("height",texts.height,height),
            Triple("rotation",texts.rotation,rotation),Triple("distance",texts.distance,distance))) {
            val label=createElement("label"); label.setAttribute("for","city-$id"); label.setText(text)
            label.appendChild(input); controls.appendChild(label)
        }
        controls.appendChild(reset); container.appendChild(controls)
        val lesson=createElement("details"); lesson.setAttribute("id","city-lesson")
        lesson.appendChild(createElement("summary").also { it.setText(texts.lesson) })
        for(text in listOf(texts.instancingHelp,texts.depthHelp,texts.proceduralHelp))
            lesson.appendChild(createElement("p").also { it.setText(text) })
        lesson.appendChild(counts)
        for((label,shader) in listOf(texts.buildingShader to CityBuildingShader,texts.groundShader to CityGroundShader)) {
            lesson.appendChild(createElement("h2").also { it.setText(label) })
            lesson.appendChild(wgslBlock(shader))
        }
        container.appendChild(lesson)
        status.setAttribute("id","city-status"); status.setAttribute("role","status"); status.setClass("demo-status")
        container.appendChild(status); reload.hidden=true; container.appendChild(reload)
        container.appendChild(createElement("a").also {
            it.setAttribute("href","https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/instancedcity")
            it.setText(texts.source)
        })
        root.appendChild(container)
        update(CityControls(),false)
    }

    fun setDensities(options: List<CityDensity>) {
        density.setText("")
        for(option in options) density.appendChild(createElement("option").also {
            it.setAttribute("value",option.name); it.setText(option.count.toString())
        })
    }
    fun update(controls: CityControls,enabled: Boolean,replacing: Boolean=false) {
        for(input in gpuControls) input.disabled=!enabled
        density.disabled=!enabled || replacing
        density.value=controls.density.name; height.value=controls.heightScale.toString()
        distance.value=controls.camera.distance.toString()
        rotation.unsafeCast<CityCheckbox>().checked=controls.rotating
        counts.setText(texts.drawHelp.replace("{buildings}",controls.density.count.toString())
            .replace("{triangles}",(controls.density.count*12+2).toString()))
    }
    fun message(text: String,error: Boolean=false) {
        status.setText(text); status.setAttribute("data-error",error.toString())
    }
    private fun range(name: String,min: String,max: String,step: String)=createElement("input").also {
        it.setAttribute("id","city-$name"); it.setAttribute("type","range")
        it.setAttribute("min",min); it.setAttribute("max",max); it.setAttribute("step",step)
    }
    private fun button(name: String,text: String)=createElement("button").also {
        it.setAttribute("id","city-$name"); it.setAttribute("type","button"); it.setText(text)
    }
}
