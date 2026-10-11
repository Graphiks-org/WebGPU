@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.demos.filterlab

import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*

/** Owns its allocations, borrows device/encoder/target. Submit before changing uniforms again. */
class FilterLabScene private constructor(
    private val device: GPUDevice,
    val imageWidth: Int,
    val imageHeight: Int,
    private val result: GPUTexture,
    private val computeUniform: GPUBuffer,
    private val renderUniform: GPUBuffer,
    private val compute: GPUComputePipeline,
    private val render: GPURenderPipeline,
    private val computeGroup: GPUBindGroup,
    private val renderGroup: GPUBindGroup,
    private val owned: List<AutoCloseable>,
) : AutoCloseable {
    private var closed = false
    private var computed = false
    private var lastSettings: FilterSettings? = null
    val resultRowBytes: Int get() = ((imageWidth * 4 + 255) / 256) * 256
    val resultCopyBytes: ULong get() = resultRowBytes.toULong() * imageHeight.toUInt()

    fun encodeFrame(encoder: GPUCommandEncoder, target: GPUTextureView, width: Int, height: Int,
        settings: FilterSettings, comparison: Float = 0.5f, recompute: Boolean = true) {
        check(!closed) { "Filter lab scene is closed." }
        require(width > 0 && height > 0)
        validateFilterSettings(settings)
        require(comparison.isFinite() && comparison in 0f..1f)
        require(recompute || (computed && lastSettings == settings)) { "Compute is required for new settings." }
        if (recompute) {
            // Header and trailing floats occupy two aligned vec4 blocks.
            val bytes = ArrayBuffer.allocate(32uL)
            bytes.setUInt(0uL, imageWidth.toUInt()); bytes.setUInt(4uL, imageHeight.toUInt())
            bytes.setUInt(8uL, settings.filter.ordinal.toUInt()); bytes.setUInt(12uL, settings.blurRadius.toUInt())
            bytes.setFloat(16uL, settings.intensity); bytes.setFloat(20uL, settings.edgeThreshold)
            device.queue.writeBuffer(computeUniform, 0uL, bytes)
            val pass = encoder.beginComputePass()
            pass.setPipeline(compute); pass.setBindGroup(0u, computeGroup)
            pass.dispatchWorkgroups(((imageWidth + 7) / 8).toUInt(), ((imageHeight + 7) / 8).toUInt())
            pass.end()
            computed = true; lastSettings = settings
        }
        device.queue.writeBuffer(renderUniform, 0uL, ArrayBuffer.of(floatArrayOf(width.toFloat(), height.toFloat(), comparison, 0f)))
        val pass = encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
            RenderPassColorAttachment(view = target, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                clearValue = Color(0.035, 0.045, 0.065, 1.0)))))
        pass.setPipeline(render); pass.setBindGroup(0u, renderGroup); pass.draw(3u); pass.end()
    }

    /** Aligned RGBA8 copy, useful for portable readback consumers. */
    fun encodeResultCopy(encoder: GPUCommandEncoder, destination: GPUBuffer) {
        check(!closed && computed) { "No computed image available." }
        require(destination.size >= resultCopyBytes)
        require(GPUBufferUsage.CopyDst in destination.usage)
        encoder.copyTextureToBuffer(TexelCopyTextureInfo(result), TexelCopyBufferInfo(destination,
            bytesPerRow = resultRowBytes.toUInt(), rowsPerImage = imageHeight.toUInt()),
            Extent3D(imageWidth.toUInt(), imageHeight.toUInt()))
    }

    override fun close() {
        if (closed) return
        closed = true
        release(owned)
    }

    companion object {
        fun create(device: GPUDevice, format: GPUTextureFormat, image: FilterImage): FilterLabScene {
            validateFilterImage(image, minOf(2048u, device.limits.maxTextureDimension2D).toInt())
            val limits = device.limits
            require(limits.maxComputeInvocationsPerWorkgroup >= 64u && limits.maxComputeWorkgroupSizeX >= 8u &&
                limits.maxComputeWorkgroupSizeY >= 8u && limits.maxComputeWorkgroupsPerDimension >=
                ((maxOf(image.width, image.height) + 7) / 8).toUInt() && limits.maxSampledTexturesPerShaderStage >= 2u &&
                limits.maxStorageTexturesPerShaderStage >= 1u && limits.maxUniformBufferBindingSize >= 32uL && limits.maxBufferSize >= 32uL)
            val owned = mutableListOf<AutoCloseable>()
            fun <T : AutoCloseable> own(value: T): T = value.also { owned.add(it) }
            try {
                val size = Extent3D(image.width.toUInt(), image.height.toUInt())
                val source = own(device.createTexture(TextureDescriptor(size = size, format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst)))
                val result = own(device.createTexture(TextureDescriptor(size = size, format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.StorageBinding or GPUTextureUsage.CopySrc)))
                val sourceView = own(source.createView()); val resultView = own(result.createView())
                fun uniform(size: ULong) = own(device.createBuffer(BufferDescriptor(size = size,
                    usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst)))
                val computeUniform = uniform(32uL); val renderUniform = uniform(16uL)
                val computeLayout = own(device.createBindGroupLayout(BindGroupLayoutDescriptor(entries = listOf(
                    BindGroupLayoutEntry(0u, GPUShaderStage.Compute, texture = TextureBindingLayout()),
                    BindGroupLayoutEntry(1u, GPUShaderStage.Compute, storageTexture = StorageTextureBindingLayout(format = GPUTextureFormat.RGBA8Unorm)),
                    BindGroupLayoutEntry(2u, GPUShaderStage.Compute, buffer = BufferBindingLayout(minBindingSize = 32uL)),
                ))))
                val computePipelineLayout = own(device.createPipelineLayout(PipelineLayoutDescriptor(listOf(computeLayout))))
                val compute = device.createShaderModule(ShaderModuleDescriptor(code = FilterLabComputeWgsl)).use { shader ->
                    own(device.createComputePipeline(ComputePipelineDescriptor(layout = computePipelineLayout,
                        compute = ProgrammableStage(shader, entryPoint = "main"))))
                }
                val renderLayout = own(device.createBindGroupLayout(BindGroupLayoutDescriptor(entries = listOf(
                    BindGroupLayoutEntry(0u, GPUShaderStage.Fragment, texture = TextureBindingLayout()),
                    BindGroupLayoutEntry(1u, GPUShaderStage.Fragment, texture = TextureBindingLayout()),
                    BindGroupLayoutEntry(2u, GPUShaderStage.Fragment, buffer = BufferBindingLayout(minBindingSize = 16uL)),
                ))))
                val renderPipelineLayout = own(device.createPipelineLayout(PipelineLayoutDescriptor(listOf(renderLayout))))
                val render = device.createShaderModule(ShaderModuleDescriptor(code = FilterLabRenderWgsl)).use { shader ->
                    own(device.createRenderPipeline(RenderPipelineDescriptor(layout = renderPipelineLayout,
                        vertex = VertexState(module = shader, entryPoint = "vertexMain"), fragment = FragmentState(module = shader,
                            entryPoint = "fragmentMain", targets = listOf(ColorTargetState(format))))))
                }
                val computeGroup = own(device.createBindGroup(BindGroupDescriptor(computeLayout, listOf(
                    BindGroupEntry(0u, sourceView), BindGroupEntry(1u, resultView), BindGroupEntry(2u, BufferBinding(computeUniform))))))
                val renderGroup = own(device.createBindGroup(BindGroupDescriptor(renderLayout, listOf(
                    BindGroupEntry(0u, sourceView), BindGroupEntry(1u, resultView), BindGroupEntry(2u, BufferBinding(renderUniform))))))
                device.queue.writeTexture(TexelCopyTextureInfo(source), ArrayBuffer.of(image.pixels),
                    TexelCopyBufferLayout(bytesPerRow = (image.width * 4).toUInt(), rowsPerImage = image.height.toUInt()), size)
                return FilterLabScene(device, image.width, image.height, result, computeUniform, renderUniform,
                    compute, render, computeGroup, renderGroup, owned)
            } catch (failure: Throwable) { release(owned, failure); throw failure }
        }
    }
}

private fun release(resources: List<AutoCloseable>, original: Throwable? = null) {
    var failure = original
    for (resource in resources.asReversed()) try { resource.close() } catch (error: Throwable) {
        val prior = failure
        if (prior == null) failure = error else prior.addSuppressed(error)
    }
    if (original == null) failure?.let { throw it }
}
