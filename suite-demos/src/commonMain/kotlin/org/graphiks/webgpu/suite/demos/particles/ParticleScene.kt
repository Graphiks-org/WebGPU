package org.graphiks.webgpu.suite.demos.particles

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCommandEncoder
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.GPUVertexStepMode
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.VertexAttribute
import org.graphiks.webgpu.descriptors.VertexBufferLayout
import org.graphiks.webgpu.descriptors.VertexState

/** Radius of one particle, in simulation units. */
internal const val ParticleRadius = 0.018f

/** Largest frame delta the scene accepts; longer frames are clamped by the runner anyway. */
internal const val MaximumDeltaSeconds = 0.05f

private const val ParticleBufferSize = 16uL

/**
 * A portable particle scene: a compute pass updates positions and velocities in a GPU storage
 * buffer, then a render pass draws that same buffer as an instanced vertex buffer.
 *
 * The scene owns its GPU resources and never touches the device, the canvas or the DOM. The runner
 * provides the [GPUDevice], the target [GPUTextureFormat] and, for each frame, the encoder and the
 * color target view. [particleBuffer] is exposed for readback-style composition but stays owned by
 * the scene: a consumer must not close it.
 *
 * One [encodeFrame] call corresponds to one submission. Write the parameters, encode the compute
 * and render passes, then submit the finished command buffer once; do not encode several frames
 * with different parameters before submitting them.
 */
class ParticleScene private constructor(
    private val device: GPUDevice,
    val count: Int,
    val particleBuffer: GPUBuffer,
    private val parametersBuffer: GPUBuffer,
    private val parametersData: ArrayBuffer,
    private val computePipeline: GPUComputePipeline,
    private val computeGroup: GPUBindGroup,
    private val renderPipeline: GPURenderPipeline,
    private val renderGroup: GPUBindGroup,
) : AutoCloseable {

    private var closed = false

    /**
     * Replaces the particle state with [initial] without recreating the scene.
     *
     * The array must hold exactly [count] particles in the same `[x, y, vx, vy]` layout. Callers
     * usually pass the same reproducible array the scene was created with. Changing the count needs
     * a new scene instead.
     */
    fun reset(initial: FloatArray) {
        check(!closed) { "The particle scene is closed." }
        require(initial.size == count * FloatsPerParticle) {
            "Reset expects ${count * FloatsPerParticle} floats for $count particles, was ${initial.size}."
        }
        require(initial.all { it.isFinite() }) { "Particle data must be finite." }
        writeParticles(initial)
    }

    /**
     * Encodes one compute-and-render frame into [encoder], writing to the supplied [target] view.
     *
     * The parameters are uploaded through the queue, the particle buffer is updated with a compute
     * pass (skipped when [deltaSeconds] is zero, so the current state renders as-is), and the same
     * buffer is then drawn as an instanced vertex buffer. The method borrows [encoder] and [target]
     * and does not submit; the caller submits the finished command buffer once.
     */
    fun encodeFrame(
        encoder: GPUCommandEncoder,
        target: GPUTextureView,
        width: Int,
        height: Int,
        deltaSeconds: Float,
    ) {
        check(!closed) { "The particle scene is closed." }
        require(width > 0 && height > 0) { "Frame dimensions must be positive, was ${width}x$height." }
        require(deltaSeconds.isFinite() && deltaSeconds in 0f..MaximumDeltaSeconds) {
            "Delta must be finite and within 0..$MaximumDeltaSeconds, was $deltaSeconds."
        }

        parametersData.setFloats(
            0uL,
            floatArrayOf(deltaSeconds, width.toFloat() / height.toFloat(), ParticleRadius, 0f),
        )
        device.queue.writeBuffer(parametersBuffer, 0uL, parametersData)

        if (deltaSeconds > 0f) {
            val compute = encoder.beginComputePass()
            compute.setPipeline(computePipeline)
            compute.setBindGroup(0u, computeGroup)
            compute.dispatchWorkgroups(((count + ParticleWorkgroupSize - 1) / ParticleWorkgroupSize).toUInt())
            compute.end()
        }

        val render = encoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = target,
                        loadOp = GPULoadOp.Clear,
                        storeOp = GPUStoreOp.Store,
                        clearValue = Color(0.0, 0.0, 0.0, 1.0),
                    ),
                ),
            ),
        )
        render.setPipeline(renderPipeline)
        render.setBindGroup(0u, renderGroup)
        render.setVertexBuffer(0u, particleBuffer)
        render.draw(6u, count.toUInt())
        render.end()
    }

    /** Closes the resources the scene owns. It is idempotent and never closes the device. */
    override fun close() {
        if (closed) return
        closed = true
        computeGroup.close()
        renderGroup.close()
        renderPipeline.close()
        computePipeline.close()
        parametersBuffer.close()
        particleBuffer.close()
    }

    private fun writeParticles(initial: FloatArray) {
        device.queue.writeBuffer(particleBuffer, 0uL, ArrayBuffer.of(initial))
    }

    companion object {
        /**
         * Validates [initial] and builds every GPU resource the scene needs.
         *
         * Data is checked on the CPU before any allocation: the array must be a whole number of
         * `[x, y, vx, vy]` particles, the count must be within [MaxParticleCount], values must be
         * finite and initial positions must stay inside the simulation square. Partial creations
         * are closed in reverse order when any step fails.
         */
        fun create(device: GPUDevice, format: GPUTextureFormat, initial: FloatArray): ParticleScene {
            validateInitial(initial)
            val count = initial.size / FloatsPerParticle

            var computeShader: GPUShaderModule? = null
            var renderShader: GPUShaderModule? = null
            var computePipeline: GPUComputePipeline? = null
            var renderPipeline: GPURenderPipeline? = null
            var computeLayout: GPUBindGroupLayout? = null
            var renderLayout: GPUBindGroupLayout? = null
            var particleBuffer: GPUBuffer? = null
            var parametersBuffer: GPUBuffer? = null
            var computeGroup: GPUBindGroup? = null
            var renderGroup: GPUBindGroup? = null
            try {
                computeShader = device.createShaderModule(ShaderModuleDescriptor(code = ParticleComputeShader))
                renderShader = device.createShaderModule(ShaderModuleDescriptor(code = ParticleRenderShader))
                computePipeline = device.createComputePipeline(
                    ComputePipelineDescriptor(
                        compute = ProgrammableStage(computeShader, entryPoint = "update"),
                    ),
                )
                renderPipeline = device.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(
                            module = renderShader,
                            entryPoint = "vertexMain",
                            buffers = listOf(
                                VertexBufferLayout(
                                    arrayStride = BytesPerParticle.toULong(),
                                    attributes = listOf(VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 0u)),
                                    stepMode = GPUVertexStepMode.Instance,
                                ),
                            ),
                        ),
                        fragment = FragmentState(
                            targets = listOf(ColorTargetState(format)),
                            module = renderShader,
                            entryPoint = "fragmentMain",
                        ),
                    ),
                )
                computeLayout = computePipeline.getBindGroupLayout(0u)
                renderLayout = renderPipeline.getBindGroupLayout(0u)
                particleBuffer = device.createBuffer(
                    BufferDescriptor(
                        size = (initial.size * 4).toULong(),
                        usage = GPUBufferUsage.Storage or GPUBufferUsage.Vertex or
                            GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc,
                    ),
                )
                parametersBuffer = device.createBuffer(
                    BufferDescriptor(size = ParticleBufferSize, usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst),
                )
                computeGroup = device.createBindGroup(
                    BindGroupDescriptor(
                        layout = computeLayout,
                        entries = listOf(
                            BindGroupEntry(0u, BufferBinding(particleBuffer)),
                            BindGroupEntry(1u, BufferBinding(parametersBuffer)),
                        ),
                    ),
                )
                renderGroup = device.createBindGroup(
                    BindGroupDescriptor(
                        layout = renderLayout,
                        entries = listOf(BindGroupEntry(0u, BufferBinding(parametersBuffer))),
                    ),
                )
                device.queue.writeBuffer(particleBuffer, 0uL, ArrayBuffer.of(initial))
                val scene = ParticleScene(
                    device = device,
                    count = count,
                    particleBuffer = particleBuffer,
                    parametersBuffer = parametersBuffer,
                    parametersData = ArrayBuffer.allocate(ParticleBufferSize),
                    computePipeline = computePipeline,
                    computeGroup = computeGroup,
                    renderPipeline = renderPipeline,
                    renderGroup = renderGroup,
                )
                // The shader modules and the two automatic layouts are only needed while building;
                // the scene keeps the buffers, pipelines and bind groups. Null each one as it is
                // released so a failure while releasing a later temporary does not close it twice.
                renderShader.close()
                renderShader = null
                computeShader.close()
                computeShader = null
                renderLayout.close()
                renderLayout = null
                computeLayout.close()
                computeLayout = null
                return scene
            } catch (failure: Throwable) {
                // Close everything created so far, in reverse creation order, buffers included.
                renderGroup?.close()
                computeGroup?.close()
                parametersBuffer?.close()
                particleBuffer?.close()
                renderLayout?.close()
                computeLayout?.close()
                renderPipeline?.close()
                computePipeline?.close()
                renderShader?.close()
                computeShader?.close()
                throw failure
            }
        }

        private fun validateInitial(initial: FloatArray) {
            require(initial.size % FloatsPerParticle == 0) {
                "Particle data must hold ${FloatsPerParticle} floats per particle, was ${initial.size}."
            }
            val count = initial.size / FloatsPerParticle
            require(count in 1..MaxParticleCount) {
                "Particle count must be in 1..$MaxParticleCount, was $count."
            }
            require(initial.all { it.isFinite() }) { "Particle data must be finite." }
            for (index in 0 until count) {
                val x = initial[index * FloatsPerParticle]
                val y = initial[index * FloatsPerParticle + 1]
                require(x in -0.95f..0.95f && y in -0.95f..0.95f) {
                    "Particle $index starts at ($x, $y), outside the simulation square."
                }
            }
        }
    }
}
