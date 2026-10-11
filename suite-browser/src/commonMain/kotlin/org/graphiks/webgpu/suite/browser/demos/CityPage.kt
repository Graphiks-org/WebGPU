@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.JsNumber
import kotlin.js.unsafeCast
import kotlin.math.min
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.*
import org.graphiks.webgpu.browser.*
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.browser.setDocumentLang
import org.graphiks.webgpu.suite.demos.instancedcity.*

suspend fun showCityPage(locale: String)=coroutineScope {
    val bootstrap=newAbortController()
    var closed=false
    val loading=launch(start=CoroutineStart.LAZY) {
        try {
            val texts=Json.decodeFromString<CityTexts>(fetchText("demos/instanced-city.$locale.json"))
            if(closed) return@launch
            setDocumentLang(locale); CityPage(CityView(texts)).start()
        } catch(cancelled: CancellationException) { throw cancelled }
        catch(failure: Throwable) { if(!closed) showDemoFailure("Cannot load city texts for '$locale': ${failure.message}") }
    }
    windowRef().listen("pagehide",bootstrap) { closed=true; loading.cancel() }
    try { loading.join() } finally { bootstrap.abort() }
}

private class CityPage(private val view: CityView): AutoCloseable {
    private val controls=CityControls(cityReducedMotion())
    private val controller=newAbortController()
    private val scope=MainScope()
    private var adapter: Adapter?=null
    private var device: GPUDevice?=null
    private var surface: CanvasSurface?=null
    private var scene: InstancedCityScene?=null
    private var frame: JsNumber?=null
    private var pointer: Int?=null
    private var ready=false
    private var closed=false
    private var failed=false
    private var replacing=false
    private var layoutWatch: CityLayoutWatch?=null

    fun start() {
        installListeners(); view.message(view.texts.loading)
        scope.launch {
            try { initialize() } catch(cancelled: CancellationException) { throw cancelled }
            catch(failure: Throwable) { fail(view.texts.failed+" "+failure.message) }
        }
    }
    private suspend fun initialize() {
        val a=requestAdapter().getOrElse { fail(view.texts.unavailable+" "+it.message); return }
        if(closed || failed) { a.close(); return }; adapter=a
        val d=a.requestDevice(DeviceDescriptor(onUncapturedError=GPUUncapturedErrorCallback {
            fail(view.texts.failed+" "+it.message)
        })).getOrElse { fail(view.texts.unavailable+" "+it.message); return }
        if(closed || failed) { d.close(); return }; device=d
        scope.launch {
            try { val lost=d.awaitLost().getOrThrow(); if(!closed && !failed) fail(view.texts.lost+" "+lost.message) }
            catch(cancelled: CancellationException) { throw cancelled }
            catch(failure: Throwable) { fail(view.texts.failed+" "+failure.message) }
        }
        val options=InstancedCityScene.supportedDensities(d.limits)
        require(options.isNotEmpty()) { "Device limits cannot render any city density" }
        controls.density=options.last(); view.setDensities(options)
        val s=view.canvas.unsafeCast<HTMLCanvasElement>().getCanvasSurface(); surface=s
        val format=s.preferredCanvasFormat ?: error("No preferred canvas format")
        s.configure(SurfaceConfiguration(d,format))
        val created=createValidatedScene(d,format,controls.density)
        if(closed || failed) { created.close(); return }
        scene=created; ready=true; view.message(""); view.update(controls,true)
        render(0.0)
        if(!failed) { view.container.setAttribute("data-ready","true"); if(controls.rotating) requestFrame() }
    }
    private suspend fun createValidatedScene(d: GPUDevice,format: GPUTextureFormat,density: CityDensity): InstancedCityScene {
        d.pushErrorScope(GPUErrorFilter.Validation)
        d.pushErrorScope(GPUErrorFilter.OutOfMemory)
        var candidate: InstancedCityScene?=null
        var failure: Throwable?=null
        try {
            candidate=InstancedCityScene.create(d,format,initialCityInstances(density))
            val canvas=view.canvas.unsafeCast<DomCanvas>()
            val max=d.limits.maxTextureDimension2D.toInt()
            val width=if(canvas.clientWidth>0) min((canvas.clientWidth*pixelRatio()).toInt(),max).coerceAtLeast(1) else canvas.width.coerceIn(1,max)
            val height=if(canvas.clientHeight>0) min((canvas.clientHeight*pixelRatio()).toInt(),max).coerceAtLeast(1) else canvas.height.coerceIn(1,max)
            candidate.prepareTarget(width,height)
        }
        catch(error: Throwable) { failure=error }
        try {
            repeat(2) {
                try {
                    val error=d.popErrorScope().getOrThrow()
                    if(error!=null) throw IllegalStateException(error.message)
                } catch(error: Throwable) { failure?.addSuppressed(error) ?: run { failure=error } }
            }
            failure?.let { throw it }
            return checkNotNull(candidate)
        } catch(error: Throwable) {
            try { candidate?.close() } catch(cleanup: Throwable) { error.addSuppressed(cleanup) }
            throw error
        }
    }
    private fun replaceDensity(density: CityDensity) {
        val d=device ?: return; val format=surface?.preferredCanvasFormat ?: return
        if(!ready || replacing || density==controls.density) return
        replacing=true; view.update(controls,true,true)
        scope.launch {
            try {
                val candidate=createValidatedScene(d,format,density)
                if(closed || failed) { candidate.close(); return@launch }
                val previous=scene; scene=candidate; controls.density=density
                previous?.close(); view.message(""); requestFrame()
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(failure: Throwable) { if(!closed && !failed) view.message(view.texts.failed+" "+failure.message,true) }
            finally { replacing=false; if(!closed && !failed) view.update(controls,true) }
        }
    }
    private fun installListeners() {
        layoutWatch=watchCityLayout(view.canvas) { controls.resetClock(); requestFrame() }
        windowRef().listen("pagehide",controller) { close() }
        windowRef().listen("resize",controller) { requestFrame() }
        documentRef().listen("visibilitychange",controller) { controls.resetClock(); requestFrame() }
        view.density.listen("change",controller) { if(ready) replaceDensity(CityDensity.valueOf(view.density.value)) }
        view.height.listen("input",controller) { if(ready) { controls.heightScale=view.height.value.toFloat(); requestFrame() } }
        view.distance.listen("input",controller) { if(ready) { controls.setDistance(view.distance.value.toFloat()); requestFrame() } }
        view.rotation.listen("change",controller) {
            if(ready) { controls.rotating=view.rotation.unsafeCast<CityCheckbox>().checked; stopLoop(); requestFrame() }
        }
        view.reset.listen("click",controller) { if(ready) { controls.resetView(); update(); requestFrame() } }
        view.reload.listen("click",controller) { reloadCityPage() }
        val canvas=view.canvas.unsafeCast<ReactionPointerCanvas>()
        view.canvas.listen("pointerdown",controller) { raw ->
            val e=raw.unsafeCast<ReactionPointerEvent>()
            if(ready && e.button==0 && controls.pointerDown(e.pointerId,e.clientX,e.clientY)) {
                pointer=e.pointerId; canvas.setPointerCapture(e.pointerId)
            }
        }
        view.canvas.listen("pointermove",controller) { raw ->
            val e=raw.unsafeCast<ReactionPointerEvent>()
            if(ready && pointer==e.pointerId) { controls.pointerMove(e.pointerId,e.clientX,e.clientY); update(); requestFrame() }
        }
        for(name in listOf("pointerup","pointercancel","lostpointercapture")) view.canvas.listen(name,controller) { raw ->
            val e=raw.unsafeCast<ReactionPointerEvent>()
            if(pointer==e.pointerId) { releasePointer(); requestFrame() }
        }
        view.canvas.addEventListener("wheel",{ raw ->
            if(ready) {
                val e=raw.unsafeCast<CityWheelEvent>()
                e.preventDefault(); controls.wheel(e.deltaY,e.deltaMode,canvas.getBoundingClientRect().height)
                update(); requestFrame()
            }
        },cityWheelOptions(controller))
    }
    private fun update() { if(ready) view.update(controls,true,replacing) }
    private fun requestFrame() {
        if(!ready || closed || failed || frame!=null) return
        frame=requestAnimationFrame { time ->
            frame=null
            if(!ready || closed || failed) return@requestAnimationFrame
            try { render(time) } catch(failure: Throwable) { fail(view.texts.failed+" "+failure.message) }
            if(ready && controls.rotating) requestFrame()
        }
    }
    private fun render(time: Double) {
        val d=device ?: return; val s=surface ?: return; val current=scene ?: return
        val canvas=view.canvas.unsafeCast<DomCanvas>()
        if(!cityCanvasVisible(view.canvas) || canvas.clientWidth<=0 || canvas.clientHeight<=0) {
            controls.advance(time,false); return
        }
        controls.advance(time,true)
        val max=d.limits.maxTextureDimension2D.toInt()
        val width=min((canvas.clientWidth*pixelRatio()).toInt(),max).coerceAtLeast(1)
        val height=min((canvas.clientHeight*pixelRatio()).toInt(),max).coerceAtLeast(1)
        if(canvas.width!=width) canvas.width=width
        if(canvas.height!=height) canvas.height=height
        s.getCurrentTexture().texture.createView().use { target -> d.createCommandEncoder().use { encoder ->
            current.encodeFrame(encoder,target,width,height,controls.camera,controls.heightScale,cityGridStep(controls.density))
            encoder.finish().use { d.queue.submit(listOf(it)) }
        } }
    }
    private fun stopLoop() { frame?.let { cancelAnimationFrame(it) }; frame=null; controls.resetClock() }
    private fun releasePointer() {
        val id=pointer; pointer=null; controls.clearPointer()
        val canvas=view.canvas.unsafeCast<ReactionPointerCanvas>()
        if(id!=null && canvas.hasPointerCapture(id)) canvas.releasePointerCapture(id)
    }
    private fun fail(message: String) {
        if(closed || failed) return
        failed=true; ready=false; stopLoop(); releasePointer()
        layoutWatch?.disconnect(); layoutWatch=null
        view.container.setAttribute("data-ready","false"); view.message(message,true)
        view.reload.hidden=false; view.update(controls,false); scope.cancel()
        closeGpu()?.let { view.message(message+" "+it.message,true) }
    }
    override fun close() {
        if(closed) return
        closed=true; ready=false; view.container.setAttribute("data-ready","false")
        layoutWatch?.disconnect(); layoutWatch=null
        stopLoop(); releasePointer(); controller.abort(); scope.cancel(); closeGpu()
    }
    private fun closeGpu(): Throwable? {
        val resources=listOfNotNull(scene,surface,device,adapter)
        scene=null; surface=null; device=null; adapter=null
        var failure: Throwable?=null
        for(resource in resources) try { resource.close() } catch(error: Throwable) {
            val first=failure; if(first==null) failure=error else first.addSuppressed(error)
        }
        return failure
    }
}
