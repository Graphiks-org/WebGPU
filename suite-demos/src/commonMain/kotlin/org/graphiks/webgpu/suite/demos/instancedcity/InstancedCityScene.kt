@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.demos.instancedcity

import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*

/** InstanceColors is a deterministic verification view, not a browser UI control. */
enum class CityDisplay { Neon, InstanceColors }

/**
 * Owns geometry, instance and frame resources; borrows the device and frame target.
 * Submit once after each encodeFrame before encoding another frame with new uniforms.
 * Two draws: one ground plane, one indexed instanced draw for all buildings.
 */
class InstancedCityScene private constructor(
    private val device: GPUDevice,
    val count: Int,
    private val vertices: GPUBuffer,
    private val indices: GPUBuffer,
    private val instances: GPUBuffer,
    private val frameBuffer: GPUBuffer,
    private val frameGroup: GPUBindGroup,
    private val buildingPipeline: GPURenderPipeline,
    private val groundPipeline: GPURenderPipeline,
    private val owned: List<AutoCloseable>,
) : AutoCloseable {
    private var depthTexture: GPUTexture?=null
    private var depthView: GPUTextureView?=null
    private var depthWidth=0
    private var depthHeight=0
    private var closed=false

    fun encodeFrame(encoder: GPUCommandEncoder,target: GPUTextureView,width: Int,height: Int,
        camera: CityCamera,heightScale: Float=1f,gridStep: Float=2f,display: CityDisplay=CityDisplay.Neon) {
        check(!closed) { "City scene is closed" }
        require(width>0 && height>0 && width.toUInt()<=device.limits.maxTextureDimension2D &&
            height.toUInt()<=device.limits.maxTextureDimension2D) { "Invalid city frame dimensions" }
        require(heightScale.isFinite() && heightScale in .5f..2f) { "Height scale must be in .5..2" }
        require(gridStep.isFinite() && gridStep in 2f..8f) { "Grid pitch must be in 2..8" }
        val matrix=cityViewProjection(camera,width,height)
        ensureDepth(width,height)
        device.queue.writeBuffer(frameBuffer,0uL,ArrayBuffer.of(matrix+floatArrayOf(heightScale,gridStep,display.ordinal.toFloat(),0f)))
        val pass=encoder.beginRenderPass(RenderPassDescriptor(
            colorAttachments=listOf(RenderPassColorAttachment(view=target,loadOp=GPULoadOp.Clear,
                storeOp=GPUStoreOp.Store,clearValue=Color(.005,.005,.025,1.0))),
            depthStencilAttachment=RenderPassDepthStencilAttachment(view=checkNotNull(depthView),
                depthClearValue=1f,depthLoadOp=GPULoadOp.Clear,depthStoreOp=GPUStoreOp.Store)))
        pass.setPipeline(groundPipeline); pass.setBindGroup(0u,frameGroup); pass.draw(6u)
        pass.setPipeline(buildingPipeline); pass.setBindGroup(0u,frameGroup)
        pass.setVertexBuffer(0u,vertices); pass.setVertexBuffer(1u,instances)
        pass.setIndexBuffer(indices,GPUIndexFormat.Uint16)
        pass.drawIndexed(36u,count.toUInt()); pass.end()
    }

    private fun ensureDepth(width: Int,height: Int) {
        if(depthWidth==width && depthHeight==height) return
        val texture=device.createTexture(TextureDescriptor(size=Extent3D(width.toUInt(),height.toUInt()),
            format=GPUTextureFormat.Depth24Plus,usage=GPUTextureUsage.RenderAttachment))
        val view=try { texture.createView() } catch(failure: Throwable) { texture.close(); throw failure }
        val old=listOfNotNull(depthTexture,depthView)
        depthTexture=texture; depthView=view; depthWidth=width; depthHeight=height
        closeCityResources(old)
    }

    override fun close() {
        if(closed) return
        closed=true
        val resources=owned+listOfNotNull(depthTexture,depthView)
        depthTexture=null; depthView=null
        closeCityResources(resources)
    }

    companion object {
        fun supportedDensities(limits: GPUSupportedLimits): List<CityDensity> =
            if (!cityLimitsSupported(limits)) emptyList() else CityDensity.entries.filter {
                it.count.toULong()*CityInstanceBytes.toULong()<=limits.maxBufferSize
            }

        fun create(device: GPUDevice,format: GPUTextureFormat,
            instances: FloatArray=initialCityInstances(CityDensity.Dense)): InstancedCityScene {
            validateCityInstances(instances)
            require(cityLimitsSupported(device.limits)) { "Device limits cannot render instanced city geometry" }
            require(instances.size.toULong()*4uL<=device.limits.maxBufferSize) { "City instance buffer exceeds device limit" }
            val owned=mutableListOf<AutoCloseable>()
            fun <T:AutoCloseable> own(resource: T): T=resource.also { owned.add(it) }
            try {
                fun upload(data: ArrayBuffer,usage: GPUBufferUsage): GPUBuffer {
                    val buffer=own(device.createBuffer(BufferDescriptor(size=data.size,
                        usage=usage or GPUBufferUsage.CopyDst)))
                    device.queue.writeBuffer(buffer,0uL,data)
                    return buffer
                }
                val vertices=upload(ArrayBuffer.of(cityCubeVertices()),GPUBufferUsage.Vertex)
                val indices=upload(ArrayBuffer.of(cityCubeIndices()),GPUBufferUsage.Index)
                val instanceBuffer=upload(ArrayBuffer.of(instances),GPUBufferUsage.Vertex)
                val frameBuffer=own(device.createBuffer(BufferDescriptor(size=80uL,usage=GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst)))
                val groupLayout=own(device.createBindGroupLayout(BindGroupLayoutDescriptor(entries=listOf(
                    BindGroupLayoutEntry(binding=0u,visibility=GPUShaderStage.Vertex or GPUShaderStage.Fragment,
                        buffer=BufferBindingLayout(minBindingSize=80uL))))))
                val pipelineLayout=own(device.createPipelineLayout(PipelineLayoutDescriptor(listOf(groupLayout))))
                val group=own(device.createBindGroup(BindGroupDescriptor(layout=groupLayout,
                    entries=listOf(BindGroupEntry(0u,BufferBinding(frameBuffer))))))
                val vertexLayout=VertexBufferLayout(arrayStride=32uL,stepMode=GPUVertexStepMode.Vertex,attributes=listOf(
                    VertexAttribute(GPUVertexFormat.Float32x3,0uL,0u),VertexAttribute(GPUVertexFormat.Float32x3,12uL,1u),
                    VertexAttribute(GPUVertexFormat.Float32x2,24uL,2u)))
                val instanceLayout=VertexBufferLayout(arrayStride=32uL,stepMode=GPUVertexStepMode.Instance,attributes=listOf(
                    VertexAttribute(GPUVertexFormat.Float32x4,0uL,3u),VertexAttribute(GPUVertexFormat.Float32x4,16uL,4u)))
                fun pipeline(code: String,vertex: String,fragment: String,buffers: List<VertexBufferLayout>,cull: GPUCullMode): GPURenderPipeline =
                    device.createShaderModule(ShaderModuleDescriptor(code=code)).use { shader ->
                        own(device.createRenderPipeline(RenderPipelineDescriptor(layout=pipelineLayout,
                            vertex=VertexState(module=shader,entryPoint=vertex,buffers=buffers),
                            fragment=FragmentState(module=shader,entryPoint=fragment,targets=listOf(ColorTargetState(format))),
                            primitive=PrimitiveState(cullMode=cull),
                            depthStencil=DepthStencilState(format=GPUTextureFormat.Depth24Plus,
                                depthWriteEnabled=true,depthCompare=GPUCompareFunction.Less))))
                    }
                val building=pipeline(CityBuildingShader,"buildingVertex","buildingFragment",listOf(vertexLayout,instanceLayout),GPUCullMode.Back)
                val ground=pipeline(CityGroundShader,"groundVertex","groundFragment",emptyList(),GPUCullMode.None)
                return InstancedCityScene(device,instances.size/8,vertices,indices,instanceBuffer,frameBuffer,group,building,ground,owned)
            } catch(failure: Throwable) { closeCityResources(owned,failure); throw failure }
        }
    }
}

private fun cityLimitsSupported(limits: GPUSupportedLimits): Boolean = limits.maxBufferSize>=768uL &&
    limits.maxUniformBufferBindingSize>=80uL && limits.maxVertexBuffers>=2u && limits.maxVertexAttributes>=5u &&
    limits.maxVertexBufferArrayStride>=32u && limits.maxBindingsPerBindGroup>=1u && limits.maxBindGroups>=1u &&
    limits.maxUniformBuffersPerShaderStage>=1u && limits.maxTextureDimension2D>=1u

private fun closeCityResources(resources: List<AutoCloseable>,original: Throwable?=null) {
    var failure=original
    for(resource in resources.asReversed()) try { resource.close() } catch(error: Throwable) {
        val first=failure; if(first==null) failure=error else first.addSuppressed(error)
    }
    if(original==null) failure?.let { throw it }
}
