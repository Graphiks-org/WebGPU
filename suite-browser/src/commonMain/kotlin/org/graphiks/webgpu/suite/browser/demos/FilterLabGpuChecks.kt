@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.abs
import kotlin.test.*
import kotlinx.coroutines.CancellationException
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import org.graphiks.webgpu.suite.browser.CaseResult
import org.graphiks.webgpu.suite.demos.filterlab.*

internal val FilterLabCheckIds = listOf("filter-lab.compute-readback",
    "filter-lab.boundaries-comparison", "filter-lab.reset-source-lifecycle")

internal suspend fun filterLabGpuResults(device: GPUDevice): List<CaseResult> {
    val checks: List<suspend () -> Unit> = listOf({ checkFilters(device) }, { checkBoundaries(device) }, { checkLifecycle(device) })
    return FilterLabCheckIds.zip(checks).map { (id, check) ->
        try {
            filterValidationScope(device, check)
            CaseResult(id, "passed")
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Throwable) { CaseResult(id, "failed", failure.stackTraceToString()) }
    }
}

private fun fixture(width: Int = 17, height: Int = 9, opaque: Boolean = false): FilterImage =
    FilterImage(width, height, UByteArray(width*height*4) { i ->
        if (i % 4 == 3) (if (opaque) 255 else listOf(0,64,128,255)[i/4%4]).toUByte()
        else ((i*37 + i/4*11) % 256).toUByte()
    })

private suspend fun checkFilters(device: GPUDevice) {
    val image = fixture()
    FilterLabScene.create(device, GPUTextureFormat.RGBA8Unorm, image).use { scene ->
        for (filter in ImageFilter.entries) for (intensity in listOf(0f, 1f)) {
            val settings = FilterSettings(filter, intensity)
            assertFilterBytes(filterReference(image, settings), readFilter(device, scene, settings, result = true))
        }
        for (radius in listOf(1, 5)) {
            val settings = FilterSettings(ImageFilter.GaussianBlur, blurRadius = radius)
            assertFilterBytes(filterReference(image, settings), readFilter(device, scene, settings, result = true))
        }
        for (threshold in listOf(0f, 1f)) {
            val settings = FilterSettings(ImageFilter.Sobel, edgeThreshold = threshold)
            assertFilterBytes(filterReference(image, settings), readFilter(device, scene, settings, result = true))
        }
    }
}

private suspend fun checkBoundaries(device: GPUDevice) {
    for ((width, height) in listOf(1 to 1, 1 to 13, 13 to 1, 17 to 9)) {
        val image = fixture(width, height)
        FilterLabScene.create(device, GPUTextureFormat.RGBA8Unorm, image).use { scene ->
            for (filter in ImageFilter.entries) {
                val settings = FilterSettings(filter, edgeThreshold = 0f)
                assertFilterBytes(filterReference(image, settings), readFilter(device, scene, settings, result = true))
            }
        }
    }
    val image = fixture(17, 9, opaque = true)
    FilterLabScene.create(device, GPUTextureFormat.RGBA8Unorm, image).use { scene ->
        val settings = FilterSettings(ImageFilter.Invert)
        val filtered = filterReference(image, settings)
        for (split in listOf(0f, 0.5f, 1f)) {
            val rendered = readFilter(device, scene, settings, comparison = split)
            val expected = UByteArray(image.pixels.size) { i ->
                if ((i/4 % image.width + 0.5f) / image.width < split) image.pixels[i] else filtered[i]
            }
            assertFilterBytes(expected, rendered)
        }
    }
    val transparent = FilterImage(1, 1, ubyteArrayOf(255u, 0u, 0u, 0u))
    FilterLabScene.create(device, GPUTextureFormat.RGBA8Unorm, transparent).use { scene ->
        assertFilterBytes(ubyteArrayOf(82u,82u,82u,255u), readFilter(device, scene, FilterSettings()))
    }
}

private suspend fun checkLifecycle(device: GPUDevice) {
    val image = fixture(17, 9, opaque = true)
    FilterLabScene.create(device, GPUTextureFormat.RGBA8Unorm, image).use { scene ->
        val original = readFilter(device, scene, FilterSettings())
        readFilter(device, scene, FilterSettings(ImageFilter.Sepia), width = 34, height = 18)
        assertContentEquals(original, readFilter(device, scene, FilterSettings()))
        assertContentEquals(original, readFilter(device, scene, FilterSettings(), recompute = false))
        device.createTexture(TextureDescriptor(size = Extent3D(17u,9u), format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.RenderAttachment)).use { target -> target.createView().use { view ->
            device.createCommandEncoder().use { encoder ->
                assertFailsWith<IllegalArgumentException> { scene.encodeFrame(encoder,view,0,9,FilterSettings()) }
                assertFailsWith<IllegalArgumentException> { scene.encodeFrame(encoder,view,17,9,FilterSettings(), Float.NaN) }
                assertFailsWith<IllegalArgumentException> { scene.encodeFrame(encoder,view,17,9,FilterSettings(ImageFilter.Invert), recompute = false) }
                scene.close(); scene.close()
                assertFailsWith<IllegalStateException> { scene.encodeFrame(encoder,view,17,9,FilterSettings()) }
            }
        } }
    }
    val replacement = FilterImage(1,1,ubyteArrayOf(10u,20u,30u,40u))
    FilterLabScene.create(device,GPUTextureFormat.RGBA8Unorm,replacement).use { scene ->
        assertFilterBytes(ubyteArrayOf(245u,235u,225u,40u), readFilter(device,scene,FilterSettings(ImageFilter.Invert),result=true))
    }
}

private suspend fun readFilter(device: GPUDevice, scene: FilterLabScene, settings: FilterSettings,
    comparison: Float = 0f, result: Boolean = false, width: Int = scene.imageWidth,
    height: Int = scene.imageHeight, recompute: Boolean = true): UByteArray {
    val w = if (result) scene.imageWidth else width
    val h = if (result) scene.imageHeight else height
    val row = ((w*4 + 255)/256)*256
    return device.createTexture(TextureDescriptor(size=Extent3D(width.toUInt(),height.toUInt()),
        format=GPUTextureFormat.RGBA8Unorm,usage=GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc)).use { target ->
        target.createView().use { view ->
            device.createBuffer(BufferDescriptor(size=(row*h).toULong(),usage=GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { staging ->
                device.createCommandEncoder().use { encoder ->
                    scene.encodeFrame(encoder,view,width,height,settings,comparison,recompute)
                    if (result) scene.encodeResultCopy(encoder,staging) else encoder.copyTextureToBuffer(TexelCopyTextureInfo(target),
                        TexelCopyBufferInfo(staging,bytesPerRow=row.toUInt(),rowsPerImage=h.toUInt()),Extent3D(w.toUInt(),h.toUInt()))
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                try {
                    val raw=staging.getMappedRange().toUByteArray()
                    UByteArray(w*h*4) { i -> raw[(i/(w*4))*row+i%(w*4)] }
                } finally { staging.unmap() }
            }
        }
    }
}

private fun assertFilterBytes(expected: UByteArray, actual: UByteArray) {
    assertEquals(expected.size, actual.size)
    for (i in expected.indices) assertTrue(abs(expected[i].toInt()-actual[i].toInt()) <= if(i%4==3) 0 else 2,
        "RGBA byte $i: expected ${expected[i]}, got ${actual[i]}")
}

private suspend fun filterValidationScope(device: GPUDevice, block: suspend () -> Unit) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    var original: Throwable? = null
    try { block() } catch (failure: Throwable) { original=failure; throw failure }
    finally {
        try { assertNull(device.popErrorScope().getOrThrow(), "GPU validation error") }
        catch (failure: Throwable) { val body=original; if(body==null) throw failure else body.addSuppressed(failure) }
    }
}
