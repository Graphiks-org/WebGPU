@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.*
import org.graphiks.webgpu.bindings.*

import org.graphiks.webgpu.browser.mapper.errorOf
import org.graphiks.webgpu.browser.mapper.map
import org.graphiks.webgpu.browser.mapper.mapDeviceLostInfo
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString
import kotlin.js.unsafeCast

class Device(val handler: WGPUDevice, onUncapturedError: GPUUncapturedErrorCallback? = null) : GPUDevice {

    init {
        onUncapturedError?.let { callback ->
            configureUncapturedError(handler, callback)
        }
    }

    override var label: String
        get() = handler.label
        set(value) {
            handler.label = value
        }

    override val queue: GPUQueue by lazy { Queue(handler.queue) }

    override val features: Set<GPUFeatureName> by lazy {
        GPUFeatureName.entries
            .filter { handler.features.has(it.value.toJsString()) }
            .toSet()
    }

    override val limits: GPUSupportedLimits by lazy { map(handler.limits) }
    override val adapterInfo: GPUAdapterInfo
        get() = map(handler.adapterInfo)

    override fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor?): GPUCommandEncoder {
        return CommandEncoder(
            when (descriptor) {
                null -> handler.createCommandEncoder()
                else -> handler.createCommandEncoder(map(descriptor))
            }
        )
    }

    override fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule {
        return map(descriptor)
            .let { handler.createShaderModule(it) }
            .let(::ShaderModule)
    }

    override fun createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): GPUPipelineLayout = handler
        .createPipelineLayout(map(descriptor))
        .let(::PipelineLayout)

    override fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): GPURenderPipeline =
        map(descriptor)
            .let { handler.createRenderPipeline(it) }
            .let(::RenderPipeline)

    override suspend fun createComputePipelineAsync(descriptor: GPUComputePipelineDescriptor): Result<GPUComputePipeline> =
        browserResult {
            map(descriptor)
                .let { handler.createComputePipelineAsync(it) }
                .await()
                .unsafeCast<WGPUComputePipeline>()
                .let { ComputePipeline(it) }
        }

    override suspend fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Result<GPURenderPipeline> =
        browserResult {
            map(descriptor)
                .let { handler.createRenderPipelineAsync(it) }
                .await()
                .unsafeCast<WGPURenderPipeline>()
                .let { RenderPipeline(it) }
        }

    override fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer = map(descriptor)
        .let { handler.createBuffer(it) }
        .let(::Buffer)

    override fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture = map(descriptor)
        .let { handler.createTexture(it) }
        .let(::Texture)

    override fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup = map(descriptor)
        .let { handler.createBindGroup(it) }
        .let(::BindGroup)

    override fun createSampler(descriptor: GPUSamplerDescriptor?): GPUSampler =
        when (descriptor) {
            null -> handler.createSampler()
            else -> map(descriptor)
                .let { handler.createSampler(it) }
        }.let(::Sampler)

    override fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): GPUComputePipeline =
        map(descriptor)
            .let { handler.createComputePipeline(it) }
            .let(::ComputePipeline)

    override fun createBindGroupLayout(descriptor: GPUBindGroupLayoutDescriptor): GPUBindGroupLayout =
        map(descriptor)
            .let { handler.createBindGroupLayout(it) }
            .let(::BindGroupLayout)

    override fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): GPURenderBundleEncoder =
        map(descriptor)
            .let { handler.createRenderBundleEncoder(it) }
            .let(::RenderBundleEncoder)

    override fun createQuerySet(descriptor: GPUQuerySetDescriptor): GPUQuerySet =
        map(descriptor)
            .let { handler.createQuerySet(it) }
            .let(::QuerySet)

    override fun pushErrorScope(filter: GPUErrorFilter) {
        handler.pushErrorScope(filter.value)
    }

    override suspend fun popErrorScope(): Result<GPUError?> = browserResult {
        handler.popErrorScope().await()?.let { errorOf(it.unsafeCast<WGPUError>()) }
    }

    override suspend fun awaitLost(): Result<GPUDeviceLostInfo> = browserResult {
        mapDeviceLostInfo(handler.lost.await().unsafeCast<WGPUDeviceLostInfo>())
    }

    private var closed = false

    override fun close() {
        // A repeated close must not release the same owned reference twice.
        if (closed) return
        closed = true
        handler.destroy()
    }

}

internal expect fun configureUncapturedError(handler: WGPUDevice, callback: GPUUncapturedErrorCallback)






