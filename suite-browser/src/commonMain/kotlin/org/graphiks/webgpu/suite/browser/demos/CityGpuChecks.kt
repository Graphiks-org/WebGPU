@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.*
import kotlinx.coroutines.CancellationException
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import org.graphiks.webgpu.suite.browser.CaseResult
import org.graphiks.webgpu.suite.demos.instancedcity.*

internal val CityCheckIds = listOf("instanced-city.instances-render-readback",
    "instanced-city.depth-occlusion", "instanced-city.camera-resize-lifecycle")
private val CheckCamera = CityCamera(0f, (15 * PI / 180).toFloat(), 100f)
private fun separateBuildings() = floatArrayOf(-12f,0f,8f,12f,10f,0f,0f,0f, 12f,0f,16f,12f,14f,1f,0f,0f)
private fun overlappingBuildings() = floatArrayOf(0f,12f,12f,12f,14f,0f,0f,0f, 0f,-12f,12f,12f,14f,1f,0f,0f)

internal suspend fun cityGpuResults(device: GPUDevice): List<CaseResult> {
    val checks: List<suspend () -> Unit> = listOf(
        { checkInstances(device) }, { checkDepth(device) }, { checkResize(device) })
    return CityCheckIds.zip(checks).map { (id, check) ->
        try { cityValidationScope(device, check); CaseResult(id, "passed") }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Throwable) { CaseResult(id, "failed", failure.stackTraceToString()) }
    }
}

private suspend fun checkInstances(device: GPUDevice) {
    InstancedCityScene.create(device, GPUTextureFormat.RGBA8Unorm, separateBuildings()).use { scene ->
        val p = renderCityPixels(device, scene)
        assertCityPixel(p,256,100,140,0,255,255)
        for ((x,y) in listOf(156 to 140,180 to 140,156 to 112)) assertCityPixel(p,256,x,y,255,0,255)
        val i=(112*256+100)*4
        assertTrue(p[i+1].toInt() < 250, "Height10 must not cover the tall-building pixel")
        // Exercise the actual Neon shader too, not only its constant-color verification branch.
        val neon = renderCityPixels(device,scene,display=CityDisplay.Neon)
        assertTrue(neon.indices.any { it % 4 != 3 && neon[it].toInt() > 100 }, "Neon render must contain visible color, not just alpha")
        for ((x,channel) in listOf(100 to 1,156 to 0)) {
            val i=(140*256+x)*4
            assertTrue(neon[i+channel].toInt() >= 50,
                "Subpixel windows must retain neon energy: x=$x channel=$channel got=${neon[i+channel]}")
        }
    }
}

private suspend fun checkDepth(device: GPUDevice) {
    val overlap = overlappingBuildings()
    for (data in listOf(overlap, overlap.copyOfRange(8,16) + overlap.copyOfRange(0,8))) {
        InstancedCityScene.create(device,GPUTextureFormat.RGBA8Unorm,data).use { scene ->
            assertCityPixel(renderCityPixels(device,scene),256,128,128,0,255,255)
        }
    }
    InstancedCityScene.create(device,GPUTextureFormat.RGBA8Unorm,overlap.copyOfRange(8,16)).use { scene ->
        assertCityPixel(renderCityPixels(device,scene),256,128,128,255,0,255)
    }
}

private suspend fun checkResize(device: GPUDevice) {
    InstancedCityScene.create(device,GPUTextureFormat.RGBA8Unorm,overlappingBuildings()).use { scene ->
        val first=renderCityPixels(device,scene)
        assertCityPixel(first,256,128,128,0,255,255)
        assertCityPixel(renderCityPixels(device,scene,camera=CheckCamera.copy(azimuth=PI.toFloat())),256,128,128,255,0,255)
        assertCityPixel(renderCityPixels(device,scene,512,256),512,256,128,0,255,255)
        assertContentEquals(first,renderCityPixels(device,scene))
        device.createTexture(TextureDescriptor(size=Extent3D(256u,256u),format=GPUTextureFormat.RGBA8Unorm,
            usage=GPUTextureUsage.RenderAttachment)).use { target -> target.createView().use { view ->
            device.createCommandEncoder().use { encoder ->
                for (bad in listOf(Float.NaN,0f,2.1f)) assertFailsWith<IllegalArgumentException> {
                    scene.encodeFrame(encoder,view,256,256,CheckCamera,heightScale=bad)
                }
                assertFailsWith<IllegalArgumentException> { scene.encodeFrame(encoder,view,0,256,CheckCamera) }
                assertFailsWith<IllegalArgumentException> { scene.encodeFrame(encoder,view,256,256,CheckCamera.copy(distance=0f)) }
                scene.close(); scene.close()
                assertFailsWith<IllegalStateException> { scene.encodeFrame(encoder,view,256,256,CheckCamera) }
            }
        } }
        device.queue.onSubmittedWorkDone().getOrThrow()
    }
}

private suspend fun renderCityPixels(device: GPUDevice, scene: InstancedCityScene,
    width: Int=256,height: Int=256,camera: CityCamera=CheckCamera,display: CityDisplay=CityDisplay.InstanceColors): UByteArray {
    val row=((width*4+255)/256)*256
    return device.createTexture(TextureDescriptor(size=Extent3D(width.toUInt(),height.toUInt()),
        format=GPUTextureFormat.RGBA8Unorm,usage=GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc)).use { target ->
        target.createView().use { view ->
            device.createBuffer(BufferDescriptor(size=(row*height).toULong(),usage=GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { staging ->
                device.createCommandEncoder().use { encoder ->
                    scene.encodeFrame(encoder,view,width,height,camera,display=display)
                    encoder.copyTextureToBuffer(TexelCopyTextureInfo(target),TexelCopyBufferInfo(staging,
                        bytesPerRow=row.toUInt(),rowsPerImage=height.toUInt()),Extent3D(width.toUInt(),height.toUInt()))
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                try {
                    val raw=staging.getMappedRange().toUByteArray()
                    UByteArray(width*height*4) { i -> raw[(i/(width*4))*row+i%(width*4)] }
                } finally { staging.unmap() }
            }
        }
    }
}

private fun assertCityPixel(p: UByteArray,width: Int,x: Int,y: Int,r: Int,g: Int,b: Int) {
    val i=(y*width+x)*4
    for ((channel,want) in listOf(r,g,b,255).withIndex())
        assertTrue(abs(p[i+channel].toInt()-want)<=2,"Pixel($x,$y) channel$channel: expected$want got${p[i+channel]}")
}

private suspend fun cityValidationScope(device: GPUDevice, block: suspend () -> Unit) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    var original: Throwable?=null
    try { block() } catch (failure: Throwable) { original=failure; throw failure }
    finally {
        try { val error=device.popErrorScope().getOrThrow(); assertNull(error,"GPU validation: ${error?.message}") }
        catch (failure: Throwable) { val body=original; if(body==null) throw failure else body.addSuppressed(failure) }
    }
}
