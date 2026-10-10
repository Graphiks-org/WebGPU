package org.graphiks.webgpu.suite.demos.reactiondiffusion

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCommandEncoder
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferBindingLayout
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.StorageTextureBindingLayout
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.VertexState

private const val StateRowBytes = ReactionGridSize * 16
private const val StateBytes = StateRowBytes * ReactionGridSize

/**
 * Portable Gray–Scott scene. Owns its resources but borrows the device and frame target.
 * Each encodeFrame must be followed by one submission before encoding another frame:
 * uniforms are queue writes, not snapshots embedded in the command buffer.
 */
class ReactionDiffusionScene private constructor(
    private val device: GPUDevice,
    private val textures: List<GPUTexture>,
    private val simulationBuffer: GPUBuffer,
    private val brushBuffer: GPUBuffer,
    private val appearanceBuffer: GPUBuffer,
    private val simulationPipeline: GPUComputePipeline,
    private val brushPipeline: GPUComputePipeline,
    private val renderPipeline: GPURenderPipeline,
    private val simulationGroups: List<GPUBindGroup>,
    private val brushGroups: List<GPUBindGroup>,
    private val renderGroups: List<GPUBindGroup>,
    private val owned: List<AutoCloseable>,
) : AutoCloseable {
    private var current = 0
    private var closed = false

    /** Restores both ping-pong textures; parameters and appearance belong to the caller. */
    fun reset(initial: FloatArray = initialReactionState()) {
        check(!closed) { "The reaction-diffusion scene is closed." }
        validateReactionState(initial)
        val data = ArrayBuffer.of(initial)
        for (texture in textures) {
            device.queue.writeTexture(TexelCopyTextureInfo(texture), data,
                TexelCopyBufferLayout(bytesPerRow = StateRowBytes.toUInt(), rowsPerImage = ReactionGridSize.toUInt()),
                Extent3D(ReactionGridSize.toUInt(), ReactionGridSize.toUInt()))
        }
        current = 0
    }

    /** Encodes optional injection, zero to sixteen fixed steps and a full-screen render. Does not submit. */
    fun encodeFrame(
        encoder: GPUCommandEncoder,
        target: GPUTextureView,
        width: Int,
        height: Int,
        steps: Int,
        parameters: ReactionParameters,
        palette: ReactionPalette = ReactionPalette.Ocean,
        display: ReactionDisplay = ReactionDisplay.Color,
        brush: ReactionBrush? = null,
    ) {
        check(!closed) { "The reaction-diffusion scene is closed." }
        require(width > 0 && height > 0) { "Frame dimensions must be positive, was ${width}x$height" }
        validateReactionSteps(steps)
        validateReactionParameters(parameters)
        if (brush != null) validateReactionBrush(brush)

        if (brush != null) {
            device.queue.writeBuffer(brushBuffer, 0uL,
                ArrayBuffer.of(floatArrayOf(brush.x.toFloat(), brush.y.toFloat(), 6f, 0f)))
            encodeCompute(encoder, brushPipeline, brushGroups[current])
            current = 1 - current
        }
        device.queue.writeBuffer(simulationBuffer, 0uL,
            ArrayBuffer.of(floatArrayOf(parameters.feed, parameters.kill, 0f, 0f)))
        repeat(steps) {
            encodeCompute(encoder, simulationPipeline, simulationGroups[current])
            current = 1 - current
        }
        device.queue.writeBuffer(appearanceBuffer, 0uL,
            ArrayBuffer.of(floatArrayOf(palette.ordinal.toFloat(), display.ordinal.toFloat(), 0f, 0f)))
        val render = encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
            RenderPassColorAttachment(view = target, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                clearValue = Color(0.0, 0.0, 0.0, 1.0)),
        )))
        render.setPipeline(renderPipeline)
        render.setBindGroup(0u, renderGroups[current])
        render.draw(3u)
        render.end()
    }

    /** Copies the current [A, B, 0, 1] float state into a borrowed CopyDst buffer (4096 bytes/row). */
    fun encodeStateCopy(encoder: GPUCommandEncoder, destination: GPUBuffer) {
        check(!closed) { "The reaction-diffusion scene is closed." }
        require(destination.size >= StateBytes.toULong()) { "State copy requires $StateBytes bytes" }
        encoder.copyTextureToBuffer(TexelCopyTextureInfo(textures[current]),
            TexelCopyBufferInfo(destination, bytesPerRow = StateRowBytes.toUInt(), rowsPerImage = ReactionGridSize.toUInt()),
            Extent3D(ReactionGridSize.toUInt(), ReactionGridSize.toUInt()))
    }

    override fun close() {
        if (closed) return
        closed = true
        closeResources(owned)
    }

    companion object {
        /** Validates CPU input and device limits before allocating; releases partial creations on failure. */
        fun create(device: GPUDevice, format: GPUTextureFormat, initial: FloatArray = initialReactionState()): ReactionDiffusionScene {
            validateReactionState(initial)
            validateLimits(device.limits)
            val owned = mutableListOf<AutoCloseable>()
            fun <T : AutoCloseable> own(resource: T): T = resource.also { owned.add(it) }
            try {
                val textures = List(2) { own(device.createTexture(TextureDescriptor(
                    size = Extent3D(256u, 256u), format = GPUTextureFormat.RGBA32Float,
                    usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.StorageBinding or
                        GPUTextureUsage.CopySrc or GPUTextureUsage.CopyDst,
                ))) }
                val views = textures.map { own(it.createView()) }
                fun uniform() = own(device.createBuffer(BufferDescriptor(size = 16uL,
                    usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst)))
                val simulationBuffer = uniform()
                val brushBuffer = uniform()
                val appearanceBuffer = uniform()
                val computeLayout = own(device.createBindGroupLayout(BindGroupLayoutDescriptor(entries = listOf(
                    BindGroupLayoutEntry(binding = 0u, visibility = GPUShaderStage.Compute,
                        texture = TextureBindingLayout(sampleType = GPUTextureSampleType.UnfilterableFloat)),
                    BindGroupLayoutEntry(binding = 1u, visibility = GPUShaderStage.Compute,
                        storageTexture = StorageTextureBindingLayout(format = GPUTextureFormat.RGBA32Float)),
                    BindGroupLayoutEntry(binding = 2u, visibility = GPUShaderStage.Compute,
                        buffer = BufferBindingLayout(minBindingSize = 16uL)),
                ))))
                val computePipelineLayout = own(device.createPipelineLayout(PipelineLayoutDescriptor(listOf(computeLayout))))
                fun compute(code: String, entry: String) = device.createShaderModule(ShaderModuleDescriptor(code = code)).use { shader ->
                    own(device.createComputePipeline(ComputePipelineDescriptor(
                        compute = ProgrammableStage(shader, entryPoint = entry), layout = computePipelineLayout)))
                }
                val simulationPipeline = compute(ReactionSimulationShader, "simulate")
                val brushPipeline = compute(ReactionBrushShader, "paint")
                fun computeGroups(buffer: GPUBuffer) = List(2) { i -> own(device.createBindGroup(BindGroupDescriptor(
                    layout = computeLayout, entries = listOf(BindGroupEntry(0u, views[i]),
                        BindGroupEntry(1u, views[1 - i]), BindGroupEntry(2u, BufferBinding(buffer))),
                ))) }
                val simulationGroups = computeGroups(simulationBuffer)
                val brushGroups = computeGroups(brushBuffer)
                val renderLayout = own(device.createBindGroupLayout(BindGroupLayoutDescriptor(entries = listOf(
                    BindGroupLayoutEntry(binding = 0u, visibility = GPUShaderStage.Fragment,
                        texture = TextureBindingLayout(sampleType = GPUTextureSampleType.UnfilterableFloat)),
                    BindGroupLayoutEntry(binding = 1u, visibility = GPUShaderStage.Fragment,
                        buffer = BufferBindingLayout(minBindingSize = 16uL)),
                ))))
                val renderPipelineLayout = own(device.createPipelineLayout(PipelineLayoutDescriptor(listOf(renderLayout))))
                val renderPipeline = device.createShaderModule(ShaderModuleDescriptor(code = ReactionRenderShader)).use { shader ->
                    own(device.createRenderPipeline(RenderPipelineDescriptor(
                        layout = renderPipelineLayout, vertex = VertexState(module = shader, entryPoint = "vertexMain"),
                        fragment = FragmentState(module = shader, entryPoint = "fragmentMain", targets = listOf(ColorTargetState(format))),
                    )))
                }
                val renderGroups = List(2) { i -> own(device.createBindGroup(BindGroupDescriptor(layout = renderLayout,
                    entries = listOf(BindGroupEntry(0u, views[i]), BindGroupEntry(1u, BufferBinding(appearanceBuffer)))))) }
                return ReactionDiffusionScene(device, textures, simulationBuffer, brushBuffer, appearanceBuffer,
                    simulationPipeline, brushPipeline, renderPipeline, simulationGroups, brushGroups, renderGroups, owned)
                    .also { it.reset(initial) }
            } catch (failure: Throwable) {
                closeResources(owned, failure)
                throw failure
            }
        }
    }
}

private fun encodeCompute(encoder: GPUCommandEncoder, pipeline: GPUComputePipeline, group: GPUBindGroup) {
    val pass = encoder.beginComputePass()
    pass.setPipeline(pipeline)
    pass.setBindGroup(0u, group)
    pass.dispatchWorkgroups(32u, 32u)
    pass.end()
}

private fun validateLimits(limits: GPUSupportedLimits) {
    require(limits.maxTextureDimension2D >= 256u && limits.maxComputeInvocationsPerWorkgroup >= 64u &&
        limits.maxComputeWorkgroupSizeX >= 8u && limits.maxComputeWorkgroupSizeY >= 8u &&
        limits.maxComputeWorkgroupsPerDimension >= 32u && limits.maxSampledTexturesPerShaderStage >= 1u &&
        limits.maxStorageTexturesPerShaderStage >= 1u && limits.maxUniformBuffersPerShaderStage >= 1u &&
        limits.maxBufferSize >= 16uL && limits.maxUniformBufferBindingSize >= 16uL) {
        "This device cannot run the 256x256 reaction-diffusion scene with 8x8 compute workgroups."
    }
}

private fun closeResources(resources: List<AutoCloseable>, original: Throwable? = null) {
    var failure = original
    for (resource in resources.asReversed()) {
        try { resource.close() } catch (error: Throwable) {
            val previous = failure
            if (previous == null) failure = error else previous.addSuppressed(error)
        }
    }
    if (original == null) failure?.let { throw it }
}
