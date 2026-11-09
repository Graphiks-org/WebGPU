package org.graphiks.webgpu.suite.demos.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUAdapterInfo
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBindGroupDescriptor
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCommandBuffer
import org.graphiks.webgpu.GPUCommandBufferDescriptor
import org.graphiks.webgpu.GPUCommandEncoder
import org.graphiks.webgpu.GPUCommandEncoderDescriptor
import org.graphiks.webgpu.GPUCompilationInfo
import org.graphiks.webgpu.GPUCompilationMessage
import org.graphiks.webgpu.GPUComputePassDescriptor
import org.graphiks.webgpu.GPUComputePassEncoder
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUExtent3D
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPURenderBundle
import org.graphiks.webgpu.GPURenderPassDescriptor
import org.graphiks.webgpu.GPURenderPassEncoder
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.GPUSupportedFeatures
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.GPUTexelCopyBufferInfo
import org.graphiks.webgpu.GPUTexelCopyTextureInfo
import org.graphiks.webgpu.GPUColor
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.ToolkitConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.tck.SimulatedGpuAdapterInfo
import org.graphiks.webgpu.toolkit.tck.SimulatedGpuDevice
import org.graphiks.webgpu.toolkit.tck.SimulatedToolkitExtension
import org.graphiks.webgpu.toolkit.tck.SimulatedTextureView

/**
 * Protocol-level smoke test of [runToolkitParticles] against the contract kit's real
 * [SimulatedToolkitExtension]: no GPU exists, yet the launcher must walk the whole toolkit
 * protocol — session, target lease, surface negotiation, one presented frame per step and a
 * clean shutdown — while building and encoding the real [ParticleScene].
 *
 * The simulation scripts the protocol but leaves every GPU-resource member of its device
 * unscripted (they throw), and the particle scene needs shader modules, pipelines, buffers,
 * bind groups and command encoders. The test therefore wraps the simulation's instances with
 * [GpuScriptedToolkitExtension], which keeps every protocol call on the simulation — adapters,
 * surfaces, frames, presents, releases, pumps and closes still land in its records — and only
 * replaces the device seam with inert no-op resources, exactly the "no real GPU" posture of a
 * protocol smoke.
 */
class ToolkitParticleSceneTest {
    @Test
    fun runsTheParticleSceneThroughTheToolkitProtocolForTwoFrames() = runTest {
        val extension = SimulatedToolkitExtension()
        val target = extension.newTarget()
        repeat(2) {
            extension.frames.send(AcquiredFrame(view = SimulatedTextureView(), extent = target.state.value.extent))
        }

        runToolkitParticles(GpuScriptedToolkitExtension(extension), target, frames = 2)

        assertEquals(2, extension.records.presents.size, "the simulation must record one present per frame")
        assertEquals(2, extension.records.releases.size, "the simulation must release every presented frame")
        assertEquals(1, extension.records.configures.size, "the surface must be negotiated exactly once")
    }

    @Test
    fun rejectsNonPositiveFrameCounts() = runTest {
        val extension = SimulatedToolkitExtension()
        assertFailsWith<IllegalArgumentException> {
            runToolkitParticles(GpuScriptedToolkitExtension(extension), extension.newTarget(), frames = 0)
        }
    }
}

/**
 * The real [SimulatedToolkitExtension] with its device seam made GPU-scriptable: the protocol
 * (instances, adapters, surfaces, frames, recording) stays the simulation's own, but the
 * session's device also survives the particle scene's resource creation through inert no-op
 * GPU objects.
 */
private class GpuScriptedToolkitExtension(
    private val simulation: SimulatedToolkitExtension,
) : WebGpuToolkitExtension {
    override val id: String = simulation.id

    private val scriptedDevice = InertResourceDevice(simulation.device)

    override fun open(config: ToolkitConfig): ToolkitInstance =
        GpuScriptedInstance(simulation.open(config), scriptedDevice)
}

/**
 * A simulation instance whose adapter hands out the GPU-scripted device; surface creation,
 * event pumping and closing still delegate to the simulation so its records stay complete.
 */
private class GpuScriptedInstance(
    simulated: ToolkitInstance,
    private val device: GPUDevice,
) : ToolkitInstance by simulated {
    override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> =
        Result.success(ScriptedGpuAdapter(device))

    override suspend fun requestAdapter(forTarget: TargetLease, hints: AdapterHints): Result<GPUAdapter> =
        requestAdapter(hints)
}

/** Adapter fixture handing out the scripted device, mirroring the simulation's own adapter. */
private class ScriptedGpuAdapter(
    private val device: GPUDevice,
) : GPUAdapter {
    override val features: GPUSupportedFeatures = emptySet()
    override val limits: GPUSupportedLimits = InertGpuLimits
    override val info: GPUAdapterInfo = SimulatedGpuAdapterInfo

    override suspend fun requestDevice(descriptor: GPUDeviceDescriptor?): Result<GPUDevice> =
        Result.success(device)

    override fun close() = Unit
}

/**
 * The simulation's device with the resource members the particle scene needs replaced by inert
 * no-op resources; lifecycle members (queue, loss, close) keep the simulation's scripted
 * behavior so the session shutdown protocol runs unchanged.
 */
private class InertResourceDevice(
    simulated: SimulatedGpuDevice,
) : GPUDevice by simulated {
    override val limits: GPUSupportedLimits = InertGpuLimits

    override fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule = InertShaderModule()

    override fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): GPUComputePipeline =
        InertComputePipeline()

    override fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): GPURenderPipeline =
        InertRenderPipeline()

    override fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer = InertBuffer()

    override fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup = InertBindGroup()

    override fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor?): GPUCommandEncoder =
        InertCommandEncoder()
}

/** Plausible desktop limits: the scene's limit checks pass and cap the count at the demo ceiling. */
private object InertGpuLimits : GPUSupportedLimits {
    override val maxTextureDimension1D: UInt = 8192u
    override val maxTextureDimension2D: UInt = 8192u
    override val maxTextureDimension3D: UInt = 2048u
    override val maxTextureArrayLayers: UInt = 256u
    override val maxBindGroups: UInt = 4u
    override val maxBindGroupsPlusVertexBuffers: UInt = 24u
    override val maxImmediateSize: UInt = 0u
    override val maxBindingsPerBindGroup: UInt = 1000u
    override val maxDynamicUniformBuffersPerPipelineLayout: UInt = 8u
    override val maxDynamicStorageBuffersPerPipelineLayout: UInt = 8u
    override val maxSampledTexturesPerShaderStage: UInt = 16u
    override val maxSamplersPerShaderStage: UInt = 16u
    override val maxStorageBuffersPerShaderStage: UInt = 8u
    override val maxStorageBuffersInVertexStage: UInt = 0u
    override val maxStorageBuffersInFragmentStage: UInt = 0u
    override val maxStorageTexturesPerShaderStage: UInt = 8u
    override val maxStorageTexturesInVertexStage: UInt = 0u
    override val maxStorageTexturesInFragmentStage: UInt = 0u
    override val maxUniformBuffersPerShaderStage: UInt = 12u
    override val maxUniformBufferBindingSize: ULong = 65536uL
    override val maxStorageBufferBindingSize: ULong = 134217728uL
    override val minUniformBufferOffsetAlignment: UInt = 256u
    override val minStorageBufferOffsetAlignment: UInt = 256u
    override val maxVertexBuffers: UInt = 8u
    override val maxBufferSize: ULong = 268435456uL
    override val maxVertexAttributes: UInt = 16u
    override val maxVertexBufferArrayStride: UInt = 2048u
    override val maxInterStageShaderVariables: UInt = 16u
    override val maxColorAttachments: UInt = 8u
    override val maxColorAttachmentBytesPerSample: UInt = 32u
    override val maxComputeWorkgroupStorageSize: UInt = 32768u
    override val maxComputeInvocationsPerWorkgroup: UInt = 256u
    override val maxComputeWorkgroupSizeX: UInt = 256u
    override val maxComputeWorkgroupSizeY: UInt = 256u
    override val maxComputeWorkgroupSizeZ: UInt = 64u
    override val maxComputeWorkgroupsPerDimension: UInt = 65535u
}

private object InertCompilationInfo : GPUCompilationInfo {
    override val messages: List<GPUCompilationMessage> = emptyList()
}

/** [GPUShaderModule] fixture: compilation is empty and closing is a no-op. */
private class InertShaderModule : GPUShaderModule {
    override var label: String = ""

    override suspend fun getCompilationInfo(): Result<GPUCompilationInfo> = Result.success(InertCompilationInfo)

    override fun close() = Unit
}

/** [GPUBindGroupLayout] fixture. */
private class InertBindGroupLayout : GPUBindGroupLayout {
    override var label: String = ""

    override fun close() = Unit
}

/** [GPUBindGroup] fixture. */
private class InertBindGroup : GPUBindGroup {
    override var label: String = ""

    override fun close() = Unit
}

/** [GPUComputePipeline] fixture handing out inert layouts. */
private class InertComputePipeline : GPUComputePipeline {
    override var label: String = ""

    override fun getBindGroupLayout(index: UInt): GPUBindGroupLayout = InertBindGroupLayout()

    override fun close() = Unit
}

/** [GPURenderPipeline] fixture handing out inert layouts. */
private class InertRenderPipeline : GPURenderPipeline {
    override var label: String = ""

    override fun getBindGroupLayout(index: UInt): GPUBindGroupLayout = InertBindGroupLayout()

    override fun close() = Unit
}

/** [GPUBuffer] fixture: unmapped, unusable for reads, and free to close. */
private class InertBuffer : GPUBuffer {
    override var label: String = ""
    override val size: ULong = 0uL
    override val usage: GPUBufferUsage = GPUBufferUsage.None
    override val mapState: GPUBufferMapState = GPUBufferMapState.Unmapped

    override suspend fun mapAsync(mode: GPUMapMode, offset: ULong, size: ULong?): Result<Unit> =
        Result.failure(UnsupportedOperationException("inert buffers never map"))

    override fun getMappedRange(offset: ULong, size: ULong?): ArrayBuffer =
        throw UnsupportedOperationException("inert buffers never map")

    override fun unmap() = Unit

    override fun close() = Unit
}

/** [GPUCommandBuffer] fixture. */
private class InertCommandBuffer : GPUCommandBuffer {
    override var label: String = ""

    override fun close() = Unit
}

/** [GPUCommandEncoder] fixture: passes are inert encoders and every copy is a no-op. */
private class InertCommandEncoder : GPUCommandEncoder {
    override var label: String = ""

    override fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder = InertRenderPassEncoder()

    override fun beginComputePass(descriptor: GPUComputePassDescriptor?): GPUComputePassEncoder =
        InertComputePassEncoder()

    override fun copyBufferToBuffer(
        source: GPUBuffer,
        sourceOffset: ULong,
        destination: GPUBuffer,
        destinationOffset: ULong,
        size: ULong?,
    ) = Unit

    override fun copyBufferToTexture(
        source: GPUTexelCopyBufferInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D,
    ) = Unit

    override fun copyTextureToBuffer(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyBufferInfo,
        copySize: GPUExtent3D,
    ) = Unit

    override fun copyTextureToTexture(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D,
    ) = Unit

    override fun clearBuffer(buffer: GPUBuffer, offset: ULong, size: ULong?) = Unit

    override fun resolveQuerySet(
        querySet: GPUQuerySet,
        firstQuery: UInt,
        queryCount: UInt,
        destination: GPUBuffer,
        destinationOffset: ULong,
    ) = Unit

    override fun finish(descriptor: GPUCommandBufferDescriptor?): GPUCommandBuffer = InertCommandBuffer()

    override fun pushDebugGroup(groupLabel: String) = Unit

    override fun popDebugGroup() = Unit

    override fun insertDebugMarker(markerLabel: String) = Unit

    override fun close() = Unit
}

/** [GPUComputePassEncoder] fixture: every command is a no-op. */
private class InertComputePassEncoder : GPUComputePassEncoder {
    override var label: String = ""

    override fun setPipeline(pipeline: GPUComputePipeline) = Unit

    override fun setBindGroup(index: UInt, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) = Unit

    override fun setImmediates(rangeOffset: UInt, data: ArrayBuffer, dataOffset: ULong, dataSize: ULong?) = Unit

    override fun dispatchWorkgroups(workgroupCountX: UInt, workgroupCountY: UInt, workgroupCountZ: UInt) = Unit

    override fun dispatchWorkgroupsIndirect(indirectBuffer: GPUBuffer, indirectOffset: ULong) = Unit

    override fun pushDebugGroup(groupLabel: String) = Unit

    override fun popDebugGroup() = Unit

    override fun insertDebugMarker(markerLabel: String) = Unit

    override fun end() = Unit
}

/** [GPURenderPassEncoder] fixture: every command is a no-op. */
private class InertRenderPassEncoder : GPURenderPassEncoder {
    override var label: String = ""

    override fun setPipeline(pipeline: GPURenderPipeline) = Unit

    override fun setBindGroup(index: UInt, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) = Unit

    override fun setImmediates(rangeOffset: UInt, data: ArrayBuffer, dataOffset: ULong, dataSize: ULong?) = Unit

    override fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float) = Unit

    override fun setScissorRect(x: UInt, y: UInt, width: UInt, height: UInt) = Unit

    override fun setBlendConstant(color: GPUColor) = Unit

    override fun setStencilReference(reference: UInt) = Unit

    override fun beginOcclusionQuery(queryIndex: UInt) = Unit

    override fun endOcclusionQuery() = Unit

    override fun executeBundles(bundles: List<GPURenderBundle>) = Unit

    override fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: GPUIndexFormat,
        offset: ULong,
        size: ULong?,
    ) = Unit

    override fun setVertexBuffer(slot: UInt, buffer: GPUBuffer?, offset: ULong, size: ULong?) = Unit

    override fun draw(vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt) = Unit

    override fun drawIndexed(
        indexCount: UInt,
        instanceCount: UInt,
        firstIndex: UInt,
        baseVertex: Int,
        firstInstance: UInt,
    ) = Unit

    override fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: ULong) = Unit

    override fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: ULong) = Unit

    override fun pushDebugGroup(groupLabel: String) = Unit

    override fun popDebugGroup() = Unit

    override fun insertDebugMarker(markerLabel: String) = Unit

    override fun end() = Unit
}
