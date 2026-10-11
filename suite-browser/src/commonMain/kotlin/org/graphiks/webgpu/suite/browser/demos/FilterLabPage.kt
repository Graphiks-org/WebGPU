@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.js.JsNumber
import kotlin.js.unsafeCast
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.*
import org.graphiks.webgpu.browser.*
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.browser.setDocumentLang
import org.graphiks.webgpu.suite.demos.filterlab.*

suspend fun showFilterLabPage(locale: String) = coroutineScope {
    val controller=newAbortController()
    var closed=false
    val loading=launch(start=CoroutineStart.LAZY) {
        try {
            val texts=Json.decodeFromString<FilterLabTexts>(fetchText("demos/filter-lab.$locale.json"))
            if(!closed) { setDocumentLang(locale); FilterLabPage(FilterLabView(texts)).start() }
        } catch(cancelled: CancellationException) { throw cancelled
        } catch(failure: Throwable) { if(!closed) showDemoFailure("Cannot load filter lab for '$locale': ${failure.message}") }
    }
    windowRef().listen("pagehide",controller) { closed=true; loading.cancel() }
    try { loading.join() } finally { controller.abort() }
}

private class FilterLabPage(private val view: FilterLabView): AutoCloseable {
    private val controls=FilterLabControls()
    private val scope=MainScope()
    private val controller=newAbortController()
    private var adapter: Adapter?=null
    private var device: GPUDevice?=null
    private var surface: CanvasSurface?=null
    private var scene: FilterLabScene?=null
    private var frame: JsNumber?=null
    private var watch: CityLayoutWatch?=null
    private var ready=false
    private var closed=false
    private var failed=false
    private var dirty=true
    // Error scopes are a device stack. Serialize only allocation, not image decoding.
    private val allocations=kotlinx.coroutines.sync.Mutex()

    fun start() {
        installListeners(); view.message(view.texts.loading)
        scope.launch {
            try { initialize() } catch(cancelled: CancellationException) { throw cancelled
            } catch(failure: Throwable) { fail(view.texts.failed+" "+failure.message) }
        }
    }
    private suspend fun initialize() {
        val a=requestAdapter().getOrElse { fail(view.texts.unavailable); return }
        if(closed || failed) { a.close(); return }; adapter=a
        val d=a.requestDevice(DeviceDescriptor(onUncapturedError=GPUUncapturedErrorCallback {
            fail(view.texts.failed+" "+it.message)
        })).getOrElse { fail(view.texts.unavailable); return }
        if(closed || failed) { d.close(); return }; device=d
        scope.launch {
            try { d.awaitLost().getOrThrow(); if(!closed && !failed) fail(view.texts.lost)
            } catch(cancelled: CancellationException) { throw cancelled
            } catch(failure: Throwable) { if(!closed) fail(view.texts.failed+" "+failure.message) }
        }
        val s=view.canvas.unsafeCast<HTMLCanvasElement>().getCanvasSurface(); surface=s
        val format=s.preferredCanvasFormat ?: error("No canvas format")
        s.configure(SurfaceConfiguration(d,format))
        val limit=minOf(2048u,d.limits.maxTextureDimension2D).toInt()
        val image=embeddedFilterImage()
        require(image.width<=limit && image.height<=limit) { "Device cannot display the illustration." }
        val candidate=createValidatedScene(d,format,image)
        if(closed || failed) { candidate.close(); return }
        scene=candidate; ready=true; view.source(view.texts.embedded,image.width,image.height)
        view.message(""); view.update(controls,true); view.container.setAttribute("data-ready","true"); requestFrame()
    }
    private suspend fun createValidatedScene(d: GPUDevice,format: GPUTextureFormat,image: FilterImage): FilterLabScene {
        allocations.lock()
        try {
            d.pushErrorScope(GPUErrorFilter.Validation); d.pushErrorScope(GPUErrorFilter.OutOfMemory)
            var candidate: FilterLabScene?=null
            var failure: Throwable?=null
            try { candidate=FilterLabScene.create(d,format,image) } catch(error: Throwable) { failure=error }
            // Cancellation must not strand scopes or a newly created candidate.
            withContext(NonCancellable) {
                repeat(2) {
                    try { d.popErrorScope().getOrThrow()?.let { throw IllegalStateException(it.message) } }
                    catch(error: Throwable) { val prior=failure; if(prior==null) failure=error else prior.addSuppressed(error) }
                }
            }
            if(!currentCoroutineContext().isActive) failure=CancellationException("Image allocation cancelled")
            failure?.let { error ->
                try { candidate?.close() } catch(cleanup: Throwable) { error.addSuppressed(cleanup) }
                throw error
            }
            return checkNotNull(candidate)
        } finally { allocations.unlock() }
    }
    private fun importImage(useEmbedded: Boolean=false) {
        val d=device ?: return; val format=surface?.preferredCanvasFormat ?: return
        if(!ready || closed || failed) return
        val file=if(useEmbedded) null else selectedFilterFile(view.file) ?: return
        val generation=controls.beginImport()
        view.message(view.texts.importing)
        scope.launch {
            try {
                val decoded=if(file!=null) decodeFilterImage(file,minOf(2048u,d.limits.maxTextureDimension2D).toInt()) else null
                if(!controls.acceptsImport(generation) || failed) return@launch
                val image=decoded?.toFilterImage() ?: embeddedFilterImage()
                val candidate=createValidatedScene(d,format,image)
                if(!controls.acceptsImport(generation) || failed) { candidate.close(); return@launch }
                val previous=scene; scene=candidate; dirty=true; previous?.close()
                view.source(decoded?.name ?: view.texts.embedded,image.width,image.height,
                    decoded!=null && (decoded.width!=decoded.originalWidth || decoded.height!=decoded.originalHeight))
                view.message(""); requestFrame()
            } catch(cancelled: CancellationException) { throw cancelled
            } catch(failure: Throwable) {
                if(controls.acceptsImport(generation) && !failed) view.message(view.texts.importFailed,true)
            } finally { if(file!=null && controls.acceptsImport(generation)) view.file.value="" }
        }
    }
    private fun installListeners() {
        watch=watchCityLayout(view.canvas) { requestFrame() }
        windowRef().listen("pagehide",controller) { close() }
        windowRef().listen("resize",controller) { requestFrame() }
        documentRef().listen("visibilitychange",controller) { requestFrame() }
        view.file.listen("change",controller) { importImage() }
        view.embedded.listen("click",controller) { importImage(true) }
        for(node in listOf(view.filter,view.intensity,view.radius,view.threshold)) node.listen("input",controller) {
            if(ready) {
                controls.update(view.readSettings(),controls.comparison); dirty=true
                view.update(controls,true); requestFrame()
            }
        }
        view.comparison.listen("input",controller) { if(ready) {
            controls.update(controls.settings,view.comparison.value.toFloat()/100); requestFrame()
        } }
        view.reset.listen("click",controller) { if(ready) {
            controls.reset(); dirty=true; view.update(controls,true); requestFrame()
        } }
        view.reload.listen("click",controller) { reloadCityPage() }
    }
    private fun requestFrame() {
        if(!ready || closed || failed || frame!=null) return
        frame=requestAnimationFrame {
            frame=null
            if(!ready || closed || failed) return@requestAnimationFrame
            try { render() } catch(failure: Throwable) { fail(view.texts.failed+" "+failure.message) }
        }
    }
    private fun render() {
        val d=device ?: return; val s=surface ?: return; val current=scene ?: return
        val canvas=view.canvas.unsafeCast<DomCanvas>()
        if(!cityCanvasVisible(view.canvas) || canvas.clientWidth<=0 || canvas.clientHeight<=0) return
        val max=d.limits.maxTextureDimension2D.toInt()
        val width=(canvas.clientWidth*pixelRatio()).toInt().coerceIn(1,max)
        val height=(canvas.clientHeight*pixelRatio()).toInt().coerceIn(1,max)
        if(canvas.width!=width) canvas.width=width
        if(canvas.height!=height) canvas.height=height
        s.getCurrentTexture().texture.createView().use { target -> d.createCommandEncoder().use { encoder ->
            current.encodeFrame(encoder,target,width,height,controls.settings,controls.comparison,dirty)
            encoder.finish().use { d.queue.submit(listOf(it)) }
        } }
        dirty=false
    }
    private fun fail(message: String) {
        if(closed || failed) return
        failed=true; ready=false; controls.close()
        stop(); watch?.disconnect(); watch=null; scope.cancel()
        view.container.setAttribute("data-ready","false"); view.update(controls,false)
        view.message(message,true); view.reload.hidden=false; closeGpu()
    }
    private fun stop() { frame?.let { cancelAnimationFrame(it) }; frame=null }
    override fun close() {
        if(closed) return
        closed=true; ready=false; controls.close(); stop()
        watch?.disconnect(); watch=null; controller.abort(); scope.cancel(); closeGpu()
    }
    private fun closeGpu() {
        val resources=listOfNotNull(scene,surface,device,adapter)
        scene=null; surface=null; device=null; adapter=null
        var failure: Throwable?=null
        for(resource in resources) try { resource.close() } catch(error: Throwable) {
            val prior=failure; if(prior==null) failure=error else prior.addSuppressed(error)
        }
        failure?.let { if(!closed) view.message(view.texts.failed+" "+it.message,true) }
    }
}
