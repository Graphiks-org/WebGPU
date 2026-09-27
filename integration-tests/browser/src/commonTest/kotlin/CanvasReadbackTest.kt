@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.integration

import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.browser.HTMLCanvasElement
import org.graphiks.webgpu.browser.SurfaceConfiguration
import org.graphiks.webgpu.browser.getCanvasSurface
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

private external interface IntegrationElement : JsAny {
    fun appendChild(child: JsAny): JsAny
}

private external interface IntegrationDocument : JsAny {
    val body: IntegrationElement
    fun createElement(tagName: String): HTMLCanvasElement
}

private fun integrationDocument(): IntegrationDocument = js("globalThis.document")

class CanvasReadbackTest {

    @Test
    fun clearsCanvasAndReadsPixels() = runTest(timeout = 60.seconds) {
        val adapter = requestAdapter().getOrThrow()
        val device = adapter.requestDevice().getOrThrow()
        val canvas = integrationDocument().createElement("canvas")
        canvas.width = 4
        canvas.height = 4
        integrationDocument().body.appendChild(canvas)
        val surface = canvas.getCanvasSurface()
        val format = surface.preferredCanvasFormat ?: error("No preferred canvas format available.")
        surface.configure(
            SurfaceConfiguration(
                device = device,
                format = format,
                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            ),
        )
        val staging = device.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
        try {
            val current = surface.getCurrentTexture()
            device.pushErrorScope(GPUErrorFilter.Validation)
            val encoder = device.createCommandEncoder()
            val pass = encoder.beginRenderPass(
                RenderPassDescriptor(
                    colorAttachments = listOf(
                        RenderPassColorAttachment(
                            view = current.texture.createView(),
                            loadOp = GPULoadOp.Clear,
                            storeOp = GPUStoreOp.Store,
                            clearValue = Color(1.0, 0.0, 0.0, 1.0),
                        ),
                    ),
                ),
            )
            pass.end()
            encoder.copyTextureToBuffer(
                TexelCopyTextureInfo(texture = current.texture),
                TexelCopyBufferInfo(buffer = staging, bytesPerRow = 256u, rowsPerImage = 4u),
                Extent3D(4u, 4u, 1u),
            )
            device.queue.submit(listOf(encoder.finish()))
            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            assertNull(device.popErrorScope().getOrThrow())
            val pixels = staging.getMappedRange().toUByteArray()
            val expected = when (format) {
                GPUTextureFormat.BGRA8Unorm -> ubyteArrayOf(0u, 0u, 255u, 255u)
                GPUTextureFormat.RGBA8Unorm -> ubyteArrayOf(255u, 0u, 0u, 255u)
                else -> error("Unexpected canvas format for an 8-bit readback: $format")
            }
            assertContentEquals(expected, pixels.copyOfRange(0, 4))
            staging.unmap()
        } finally {
            staging.close()
            surface.close()
            device.close()
            adapter.close()
        }
    }
}
