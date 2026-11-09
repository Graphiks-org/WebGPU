package org.graphiks.webgpu.toolkit.tck

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUAdapterInfo
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBindGroupDescriptor
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPUBindGroupLayoutDescriptor
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUCommandBuffer
import org.graphiks.webgpu.GPUCommandEncoder
import org.graphiks.webgpu.GPUCommandEncoderDescriptor
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUExtent3D
import org.graphiks.webgpu.GPUPipelineLayout
import org.graphiks.webgpu.GPUPipelineLayoutDescriptor
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.GPUQueue
import org.graphiks.webgpu.GPURenderBundleEncoder
import org.graphiks.webgpu.GPURenderBundleEncoderDescriptor
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUSampler
import org.graphiks.webgpu.GPUSamplerDescriptor
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.GPUSupportedFeatures
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.GPUTexelCopyBufferLayout
import org.graphiks.webgpu.GPUTexelCopyTextureInfo
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureDescriptor
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.ToolkitAlphaMode
import org.graphiks.webgpu.toolkit.ToolkitConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.ToolkitProgression
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/**
 * Every call the simulation observed, in order. Targets record their [retires] and [retains]
 * here when they were created through [SimulatedToolkitExtension.newTarget]; surface handles
 * record [configures], [presents] and [releases]; instances count [pumps] and [instanceCloses].
 */
class RecordedCalls {
    /** Leases handed out through [SurfaceTarget.retain], in order. */
    val retains = mutableListOf<TargetLease>()

    /** Leases handed back through [SurfaceTarget.retire], in order. */
    val retires = mutableListOf<TargetLease>()

    /** Configurations surfaces were negotiated with, in order. */
    val configures = mutableListOf<PresentationConfiguration>()

    /** Frames shown through [ToolkitSurfaceHandle.present], in order. */
    val presents = mutableListOf<AcquiredFrame>()

    /** Frames handed back through [ToolkitSurfaceHandle.releaseFrame], in order. */
    val releases = mutableListOf<AcquiredFrame>()

    /** Times live instances pumped host events. */
    var pumps: Int = 0
        internal set

    /** Times instances were closed; an idempotent session shutdown closes exactly once. */
    var instanceCloses: Int = 0
        internal set
}

/**
 * [TargetLease] of the simulation carrying only its generation.
 */
data class SimulatedLease(
    override val generation: Long,
) : TargetLease

/**
 * Public scriptable [SurfaceTarget] of the contract kit: publish target state through
 * [mutableState], script [retain] results into [retainScript] (a stale lease rolls the surface
 * protocol back), or flip [alwaysStale] to sabotage the protocol so every unscripted lease is
 * one generation behind the published state and never converges. Every [retain] and [retire]
 * is recorded in [records].
 */
class SimulatedTarget(
    /** State the target starts with; drawable by default. */
    initial: TargetState = DEFAULT_STATE,
    /** Call recorder; defaults to a private one, shared with the extension by [SimulatedToolkitExtension.newTarget]. */
    val records: RecordedCalls = RecordedCalls(),
) : SurfaceTarget {
    /** Scriptable state flow; publish new generations, extents or availability through it. */
    val mutableState = MutableStateFlow(initial)

    override val state: StateFlow<TargetState> = mutableState

    /** Leases [retain] hands out, in order; when empty, a fresh lease for the current generation. */
    val retainScript = Channel<TargetLease>(Channel.UNLIMITED)

    /** When `true`, every unscripted lease is one generation behind the state, so retain never converges. */
    var alwaysStale: Boolean = false

    override suspend fun retain(): TargetLease {
        val current = mutableState.value.generation
        val scripted = retainScript.tryReceive().getOrNull()
        val lease = scripted
            ?: SimulatedLease(generation = if (alwaysStale && current > 0L) current - 1 else current)
        records.retains += lease
        return lease
    }

    override suspend fun retire(lease: TargetLease) {
        records.retires += lease
    }

    companion object {
        /** Drawable default: generation 1, 640 by 480, available. */
        val DEFAULT_STATE = TargetState(generation = 1L, extent = Extent2D(width = 640u, height = 480u), available = true)
    }
}

/**
 * Public scriptable [WebGpuToolkitExtension] of the contract kit: it opens instances with
 * `Pumped` progression that hand out the fake [device], create surfaces fed from [frames], and
 * record every protocol call in [records]. Script device loss with [loseDevice] and a stalling
 * teardown with [hangShutdownUntil]; binding repositories run [ToolkitProtocolTck] against
 * their real extensions with these fixtures to validate the protocol paths without a GPU.
 */
class SimulatedToolkitExtension : WebGpuToolkitExtension {
    override val id: String = "simulated"

    /** Every call the simulation observed; see [RecordedCalls]. */
    val records = RecordedCalls()

    /** Device every adapter request hands out; lose it with [loseDevice]. */
    val device = SimulatedGpuDevice()

    /** Frame source of every surface the simulation creates; feed it to script frame availability. */
    val frames = Channel<AcquiredFrame>(Channel.UNLIMITED)

    /**
     * When non-null, closing the device defers its loss notification until this signal
     * completes, modeling a backend whose teardown stalls past the shutdown timeout.
     */
    var hangShutdownUntil: CompletableDeferred<Unit>?
        get() = device.hangShutdownUntil
        set(value) {
            device.hangShutdownUntil = value
        }

    /** Completes the fake device's loss with the scripted reason, `Unknown` by default. */
    fun loseDevice(
        reason: GPUDeviceLostReason = GPUDeviceLostReason.Unknown,
        message: String = "simulated device lost",
    ) {
        device.lose(reason, message)
    }

    /** Creates a scriptable target whose target-side calls record into this extension's [records]. */
    fun newTarget(initial: TargetState = SimulatedTarget.DEFAULT_STATE): SimulatedTarget =
        SimulatedTarget(initial, records)

    override fun open(config: ToolkitConfig): ToolkitInstance = SimulatedInstance()

    /** [ToolkitInstance] the simulation opens: `Pumped` progression, every call recorded. */
    inner class SimulatedInstance : ToolkitInstance {
        override val progression: ToolkitProgression = ToolkitProgression.Pumped

        override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> =
            Result.success(SimulatedGpuAdapter(device))

        override suspend fun requestAdapter(forTarget: TargetLease, hints: AdapterHints): Result<GPUAdapter> =
            requestAdapter(hints)

        override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle =
            SimulatedSurfaceHandle()

        override fun pumpEvents() {
            records.pumps++
        }

        override fun close() {
            records.instanceCloses++
        }
    }

    /** [ToolkitSurfaceHandle] the simulation creates: frames come from [frames], calls land in [records]. */
    inner class SimulatedSurfaceHandle : ToolkitSurfaceHandle {
        override val capabilities: SurfaceCapabilities = DEFAULT_CAPABILITIES

        override fun configure(configuration: PresentationConfiguration) {
            records.configures += configuration
        }

        override suspend fun acquireFrame(): AcquiredFrame = frames.receive()

        override fun present(frame: AcquiredFrame) {
            records.presents += frame
        }

        override fun releaseFrame(frame: AcquiredFrame) {
            records.releases += frame
        }

        override fun close() = Unit
    }

    companion object {
        /** Browser-like defaults: BGRA preferred, both formats, both alpha modes. */
        val DEFAULT_CAPABILITIES = SurfaceCapabilities(
            preferredFormat = GPUTextureFormat.BGRA8Unorm,
            formats = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm),
            alphaModes = listOf(ToolkitAlphaMode.Opaque, ToolkitAlphaMode.Premultiplied),
        )
    }
}

/** Simple loss info the simulation completes [SimulatedGpuDevice.awaitLost] with. */
class SimulatedDeviceLostInfo(
    override val reason: GPUDeviceLostReason,
    override val message: String,
) : GPUDeviceLostInfo

/** [GPUDevice] fixture: queue is stable, loss is scripted, `close` sets a flag and resolves loss. */
class SimulatedGpuDevice : GPUDevice {
    private var lost = CompletableDeferred<Result<GPUDeviceLostInfo>>()

    /** Queue handed out by [queue]; sessions must expose exactly this object. */
    val simulatedQueue = SimulatedGpuQueue()

    /** Whether the device was closed since it was last handed out. */
    var closed: Boolean = false
        private set

    /**
     * When non-null, [close] records the close but defers the loss notification until the
     * signal completes, modeling a backend whose teardown stalls; a shutdown bounded by its
     * timeout then observes `ShutdownTimedOut` while cleanup keeps retrying.
     */
    var hangShutdownUntil: CompletableDeferred<Unit>? = null
        set(value) {
            field = value
            value?.invokeOnCompletion { notifyLossIfClosed() }
        }

    /** Completes [awaitLost] successfully; defaults map to a real (non-`Destroyed`) loss. */
    fun lose(
        reason: GPUDeviceLostReason = GPUDeviceLostReason.Unknown,
        message: String = "simulated device lost",
    ) {
        lost.complete(Result.success(SimulatedDeviceLostInfo(reason, message)))
    }

    /**
     * Restarts the loss lifecycle for the next session: every adapter request hands this device
     * out anew, so a close (or loss) observed by a previous session does not swallow the next
     * one's scripted loss. A pending loss is left untouched.
     */
    internal fun rearmForNextSession() {
        if (lost.isCompleted) {
            closed = false
            lost = CompletableDeferred()
        }
    }

    override var label: String = ""

    override val features: GPUSupportedFeatures = emptySet()

    override val limits: GPUSupportedLimits = SimulatedGpuLimits

    override val adapterInfo: GPUAdapterInfo = SimulatedGpuAdapterInfo

    override val queue: GPUQueue
        get() = simulatedQueue

    override suspend fun awaitLost(): Result<GPUDeviceLostInfo> = lost.await()

    override fun close() {
        closed = true
        notifyLossIfClosed()
    }

    private fun notifyLossIfClosed() {
        val signal = hangShutdownUntil
        if (closed && (signal == null || signal.isCompleted)) {
            lost.complete(
                Result.success(SimulatedDeviceLostInfo(GPUDeviceLostReason.Destroyed, "simulated device closed")),
            )
        }
    }

    override fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer = unused()

    override fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture = unused()

    override fun createSampler(descriptor: GPUSamplerDescriptor?): GPUSampler = unused()

    override fun createBindGroupLayout(descriptor: GPUBindGroupLayoutDescriptor): GPUBindGroupLayout = unused()

    override fun createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): GPUPipelineLayout = unused()

    override fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup = unused()

    override fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule = unused()

    override fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): GPUComputePipeline = unused()

    override fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): GPURenderPipeline = unused()

    override suspend fun createComputePipelineAsync(descriptor: GPUComputePipelineDescriptor): Result<GPUComputePipeline> =
        unused()

    override suspend fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Result<GPURenderPipeline> =
        unused()

    override fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor?): GPUCommandEncoder = unused()

    override fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): GPURenderBundleEncoder =
        unused()

    override fun createQuerySet(descriptor: GPUQuerySetDescriptor): GPUQuerySet = unused()

    override fun pushErrorScope(filter: GPUErrorFilter) = Unit

    override suspend fun popErrorScope(): Result<GPUError?> = unused()
}

/** [GPUQueue] fixture: every operation is a no-op. */
class SimulatedGpuQueue : GPUQueue {
    override var label: String = ""

    override fun submit(commandBuffers: List<GPUCommandBuffer>) = Unit

    override suspend fun onSubmittedWorkDone(): Result<Unit> = Result.success(Unit)

    override fun writeBuffer(
        buffer: GPUBuffer,
        bufferOffset: ULong,
        data: ArrayBuffer,
        dataOffset: ULong,
        size: ULong?,
    ) = Unit

    override fun writeTexture(
        destination: GPUTexelCopyTextureInfo,
        data: ArrayBuffer,
        dataLayout: GPUTexelCopyBufferLayout,
        size: GPUExtent3D,
    ) = Unit
}

/** [GPUAdapter] fixture handing out the session's [device], re-armed for a fresh loss lifecycle. */
class SimulatedGpuAdapter(
    private val device: SimulatedGpuDevice,
) : GPUAdapter {
    override val features: GPUSupportedFeatures = emptySet()

    override val limits: GPUSupportedLimits = SimulatedGpuLimits

    override val info: GPUAdapterInfo = SimulatedGpuAdapterInfo

    override suspend fun requestDevice(descriptor: GPUDeviceDescriptor?): Result<GPUDevice> {
        device.rearmForNextSession()
        return Result.success(device)
    }

    override fun close() = Unit
}

/** Zeroed limits; no simulation needs real limit values. */
object SimulatedGpuLimits : GPUSupportedLimits {
    override val maxTextureDimension1D: UInt = 0u
    override val maxTextureDimension2D: UInt = 0u
    override val maxTextureDimension3D: UInt = 0u
    override val maxTextureArrayLayers: UInt = 0u
    override val maxBindGroups: UInt = 0u
    override val maxBindGroupsPlusVertexBuffers: UInt = 0u
    override val maxImmediateSize: UInt = 0u
    override val maxBindingsPerBindGroup: UInt = 0u
    override val maxDynamicUniformBuffersPerPipelineLayout: UInt = 0u
    override val maxDynamicStorageBuffersPerPipelineLayout: UInt = 0u
    override val maxSampledTexturesPerShaderStage: UInt = 0u
    override val maxSamplersPerShaderStage: UInt = 0u
    override val maxStorageBuffersPerShaderStage: UInt = 0u
    override val maxStorageBuffersInVertexStage: UInt = 0u
    override val maxStorageBuffersInFragmentStage: UInt = 0u
    override val maxStorageTexturesPerShaderStage: UInt = 0u
    override val maxStorageTexturesInVertexStage: UInt = 0u
    override val maxStorageTexturesInFragmentStage: UInt = 0u
    override val maxUniformBuffersPerShaderStage: UInt = 0u
    override val maxUniformBufferBindingSize: ULong = 0u
    override val maxStorageBufferBindingSize: ULong = 0u
    override val minUniformBufferOffsetAlignment: UInt = 0u
    override val minStorageBufferOffsetAlignment: UInt = 0u
    override val maxVertexBuffers: UInt = 0u
    override val maxBufferSize: ULong = 0u
    override val maxVertexAttributes: UInt = 0u
    override val maxVertexBufferArrayStride: UInt = 0u
    override val maxInterStageShaderVariables: UInt = 0u
    override val maxColorAttachments: UInt = 0u
    override val maxColorAttachmentBytesPerSample: UInt = 0u
    override val maxComputeWorkgroupStorageSize: UInt = 0u
    override val maxComputeInvocationsPerWorkgroup: UInt = 0u
    override val maxComputeWorkgroupSizeX: UInt = 0u
    override val maxComputeWorkgroupSizeY: UInt = 0u
    override val maxComputeWorkgroupSizeZ: UInt = 0u
    override val maxComputeWorkgroupsPerDimension: UInt = 0u
}

/** Empty adapter info; every field is the "unknown" value of the interface. */
object SimulatedGpuAdapterInfo : GPUAdapterInfo {
    override val vendor: String = ""
    override val architecture: String = ""
    override val device: String = ""
    override val description: String = ""
    override val subgroupMinSize: UInt = 0u
    override val subgroupMaxSize: UInt = 0u
    override val isFallbackAdapter: Boolean = false
}

/** [GPUTextureView] fixture; no member carries behavior. */
class SimulatedTextureView : GPUTextureView {
    override var label: String = ""

    override fun close() = Unit
}

/** Placeholder body for toolkit-untouched [GPUDevice] members. */
private fun <T> unused(): T = throw UnsupportedOperationException("simulated GPU device member is not scripted")
