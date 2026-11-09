import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUAdapterInfo
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
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.GPUQueue
import org.graphiks.webgpu.GPURenderBundleEncoder
import org.graphiks.webgpu.GPURenderBundleEncoderDescriptor
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUSampler
import org.graphiks.webgpu.GPUSamplerDescriptor
import org.graphiks.webgpu.GPUSupportedFeatures
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureDescriptor
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBindGroupDescriptor
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPUBindGroupLayoutDescriptor
import org.graphiks.webgpu.GPUPipelineLayout
import org.graphiks.webgpu.GPUPipelineLayoutDescriptor
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.ToolkitAlphaMode
import org.graphiks.webgpu.toolkit.ToolkitConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.ToolkitProgression
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUExtent3D
import org.graphiks.webgpu.GPUTexelCopyBufferInfo
import org.graphiks.webgpu.GPUTexelCopyBufferLayout
import org.graphiks.webgpu.GPUTexelCopyTextureInfo
import org.graphiks.webgpu.GPUTextureFormat

/**
 * Test fake of [WebGpuToolkitExtension] with scriptable startup, shutdown, device-loss and
 * pump behavior. Shared fixture for the session, surface and protocol tasks.
 */
class FakeExtension : WebGpuToolkitExtension {
    override val id: String = "fake"

    /** Configs sessions passed to [open], one entry per open call. */
    val openedConfigs = mutableListOf<ToolkitConfig>()

    /** When set, adapter requests fail with this error and startup maps to `Failed`. */
    var failStartup: IllegalStateException? = null

    /** When `true`, the device defers its close (loss) notification until [releaseShutdown]. */
    var hangShutdown: Boolean
        get() = device.hangShutdown
        set(value) {
            device.hangShutdown = value
        }

    /** Progression reported by instances this extension opens. */
    var progression: ToolkitProgression = ToolkitProgression.Pumped

    /**
     * When set, invoked at the top of every [FakeInstance.pumpEvents] call; a JVM test blocks
     * here to model a native pump call that never returns.
     */
    var onPump: (() -> Unit)? = null

    /** Device every adapter request hands out; lose it with [loseDevice]. */
    val device = FakeGpuDevice()

    /** Times the opened instance entered `close`. */
    var shutdownCalls: Int = 0
        private set

    /** Times the opened instance pumped events. */
    var pumpCount: Int = 0
        private set

    /** Whether the opened instance finished closing. */
    var instanceClosed: Boolean = false
        private set

    /** Releases a shutdown hanging because of [hangShutdown]. */
    fun releaseShutdown() {
        device.releaseShutdown()
    }

    /** Completes the fake device's loss with the scripted reason, `Unknown` by default. */
    fun loseDevice(
        reason: GPUDeviceLostReason = GPUDeviceLostReason.Unknown,
        message: String = "fake device lost",
    ) {
        device.lose(reason, message)
    }

    override fun open(config: ToolkitConfig): ToolkitInstance {
        openedConfigs += config
        return FakeInstance()
    }

    /** [ToolkitInstance] fake wired to the scripting hooks of [FakeExtension]. */
    inner class FakeInstance : ToolkitInstance {
        override val progression: ToolkitProgression
            get() = this@FakeExtension.progression

        override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> {
            val failure = failStartup
            return if (failure != null) Result.failure(failure) else Result.success(FakeGpuAdapter(device))
        }

        override suspend fun requestAdapter(forTarget: TargetLease, hints: AdapterHints): Result<GPUAdapter> =
            requestAdapter(hints)

        override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle =
            FakeSurfaceHandle()

        override fun pumpEvents() {
            onPump?.invoke()
            pumpCount++
        }

        override fun close() {
            shutdownCalls++
            instanceClosed = true
        }
    }
}

/**
 * [ToolkitSurfaceHandle] fake with a scriptable frame source and recording of every call.
 * `acquireFrame` suspends while [frames] is empty; feed the channel to script availability.
 */
class FakeSurfaceHandle(
    /** Capabilities the surface reports; scriptable per test. */
    override var capabilities: SurfaceCapabilities = DEFAULT_CAPABILITIES,
) : ToolkitSurfaceHandle {
    /** Scriptable frame source; empty or closed means `acquireFrame` has nothing to hand out. */
    val frames = Channel<AcquiredFrame>(Channel.UNLIMITED)

    /** Configurations the surface was bound with, in order. */
    val recordedConfigures = mutableListOf<PresentationConfiguration>()

    /** Frames the surface was asked to show, in order. */
    val recordedPresents = mutableListOf<AcquiredFrame>()

    /** Frames handed back after presenting, in order. */
    val recordedReleases = mutableListOf<AcquiredFrame>()

    override fun configure(configuration: PresentationConfiguration) {
        recordedConfigures += configuration
    }

    override suspend fun acquireFrame(): AcquiredFrame = frames.receive()

    override fun present(frame: AcquiredFrame) {
        recordedPresents += frame
    }

    override fun releaseFrame(frame: AcquiredFrame) {
        recordedReleases += frame
    }

    override fun close() {
        frames.close()
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

/** Simple loss info the fakes complete [FakeGpuDevice.awaitLost] with. */
class FakeDeviceLostInfo(
    override val reason: GPUDeviceLostReason,
    override val message: String,
) : GPUDeviceLostInfo

/** [GPUDevice] fake: queue is stable, loss is scripted, `close` sets a flag and resolves loss. */
class FakeGpuDevice(
    /** Queue handed out by [queue]; the session must expose exactly this object. */
    val fakeQueue: FakeGpuQueue = FakeGpuQueue(),
) : GPUDevice {
    private val lost = CompletableDeferred<Result<GPUDeviceLostInfo>>()

    /** Whether the device was closed. */
    var closed: Boolean = false
        private set

    /**
     * When `true`, [close] records the close but defers the loss notification until
     * [releaseShutdown], modeling a backend whose teardown stalls.
     */
    var hangShutdown: Boolean = false

    /** Completes [awaitLost] successfully; defaults map to a real (non-`Destroyed`) loss. */
    fun lose(
        reason: GPUDeviceLostReason = GPUDeviceLostReason.Unknown,
        message: String = "fake device lost",
    ) {
        lost.complete(Result.success(FakeDeviceLostInfo(reason, message)))
    }

    /** Ends a [hangShutdown] stall; the pending close notification completes if it may. */
    fun releaseShutdown() {
        hangShutdown = false
        notifyLossIfClosed()
    }

    override var label: String = ""

    override val features: GPUSupportedFeatures = emptySet()

    override val limits: GPUSupportedLimits = FakeGpuLimits

    override val adapterInfo: GPUAdapterInfo = FakeGpuAdapterInfo

    override val queue: GPUQueue
        get() = fakeQueue

    override suspend fun awaitLost(): Result<GPUDeviceLostInfo> = lost.await()

    override fun close() {
        closed = true
        notifyLossIfClosed()
    }

    private fun notifyLossIfClosed() {
        if (closed && !hangShutdown) {
            lost.complete(Result.success(FakeDeviceLostInfo(GPUDeviceLostReason.Destroyed, "fake device closed")))
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

/** [GPUQueue] fake: every operation is a no-op. */
class FakeGpuQueue : GPUQueue {
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

/** [GPUAdapter] fake handing out the session's [FakeGpuDevice]. */
class FakeGpuAdapter(
    private val device: GPUDevice,
) : GPUAdapter {
    override val features: GPUSupportedFeatures = emptySet()

    override val limits: GPUSupportedLimits = FakeGpuLimits

    override val info: GPUAdapterInfo = FakeGpuAdapterInfo

    override suspend fun requestDevice(descriptor: GPUDeviceDescriptor?): Result<GPUDevice> =
        Result.success(device)

    override fun close() = Unit
}

/** Zeroed limits; no toolkit fake needs real limit values. */
object FakeGpuLimits : GPUSupportedLimits {
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
object FakeGpuAdapterInfo : GPUAdapterInfo {
    override val vendor: String = ""
    override val architecture: String = ""
    override val device: String = ""
    override val description: String = ""
    override val subgroupMinSize: UInt = 0u
    override val subgroupMaxSize: UInt = 0u
    override val isFallbackAdapter: Boolean = false
}

/** Placeholder body for toolkit-untouched [GPUDevice] members. */
private fun <T> unused(): T = throw UnsupportedOperationException("fake GPU device member is not scripted")
