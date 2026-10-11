@file:OptIn(ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUBlendComponent
import org.graphiks.webgpu.GPUBlendState
import org.graphiks.webgpu.GPUColorTargetState
import org.graphiks.webgpu.GPUDepthStencilState
import org.graphiks.webgpu.GPUFragmentState
import org.graphiks.webgpu.GPUMultisampleState
import org.graphiks.webgpu.GPUPrimitiveState
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUStencilFaceState
import org.graphiks.webgpu.GPUVertexAttribute
import org.graphiks.webgpu.GPUVertexBufferLayout
import org.graphiks.webgpu.GPUVertexState
import org.graphiks.webgpu.browser.PipelineLayout
import org.graphiks.webgpu.browser.ShaderModule
import org.graphiks.webgpu.bindings.WGPUBlendComponent
import org.graphiks.webgpu.bindings.WGPUBlendState
import org.graphiks.webgpu.bindings.WGPUColorTargetState
import org.graphiks.webgpu.bindings.WGPUDepthStencilState
import org.graphiks.webgpu.bindings.WGPUFragmentState
import org.graphiks.webgpu.bindings.WGPUMultisampleState
import org.graphiks.webgpu.bindings.WGPUPrimitiveState
import org.graphiks.webgpu.bindings.WGPURenderPipelineDescriptor
import org.graphiks.webgpu.bindings.WGPUStencilFaceState
import org.graphiks.webgpu.bindings.WGPUVertexAttribute
import org.graphiks.webgpu.bindings.WGPUVertexBufferLayout
import org.graphiks.webgpu.bindings.WGPUVertexState
import org.graphiks.webgpu.bindings.asJsNumber
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.bindings.mapJsArray
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

internal fun map(input: GPURenderPipelineDescriptor): WGPURenderPipelineDescriptor =
    createJsObject<WGPURenderPipelineDescriptor>().apply {
        label = input.label
        vertex = map(input.vertex)
        layout = (input.layout as PipelineLayout?)?.handler ?: "auto".toJsString()
        primitive = map(input.primitive)
        input.depthStencil?.let { depthStencil = map(it) }
        input.fragment?.let { fragment = map(it) }
        multisample = map(input.multisample)
    }

internal fun map(input: GPUVertexState): WGPUVertexState = createJsObject<WGPUVertexState>().apply {
    module = (input.module as ShaderModule).handler
    input.entryPoint?.let { entryPoint = it }
    constants = mapConstants(input.constants)
    buffers = input.buffers.mapJsArray { buffer -> buffer?.let { map(it) } }
}

private fun map(input: GPUVertexBufferLayout): WGPUVertexBufferLayout =
    createJsObject<WGPUVertexBufferLayout>().apply {
        arrayStride = input.arrayStride.asJsNumber()
        attributes = input.attributes.mapJsArray { map(it) }
        stepMode = input.stepMode.value
    }

private fun map(input: GPUVertexAttribute): WGPUVertexAttribute =
    createJsObject<WGPUVertexAttribute>().apply {
        format = input.format.value
        offset = input.offset.asJsNumber()
        shaderLocation = input.shaderLocation.asJsNumber()
    }

private fun map(input: GPUPrimitiveState): WGPUPrimitiveState =
    createJsObject<WGPUPrimitiveState>().apply {
        topology = input.topology.value
        input.stripIndexFormat?.let { stripIndexFormat = it.value }
        frontFace = input.frontFace.value
        cullMode = input.cullMode.value
        unclippedDepth = input.unclippedDepth
    }

private fun map(input: GPUDepthStencilState): WGPUDepthStencilState =
    createJsObject<WGPUDepthStencilState>().apply {
        format = input.format.value
        input.depthWriteEnabled?.let { depthWriteEnabled = it }
        input.depthCompare?.let { depthCompare = it.value }
        stencilFront = map(input.stencilFront)
        stencilBack = map(input.stencilBack)
        stencilReadMask = input.stencilReadMask.asJsNumber()
        stencilWriteMask = input.stencilWriteMask.asJsNumber()
        depthBias = input.depthBias.asJsNumber()
        depthBiasSlopeScale = input.depthBiasSlopeScale.asJsNumber()
        depthBiasClamp = input.depthBiasClamp.asJsNumber()
    }

private fun map(input: GPUStencilFaceState): WGPUStencilFaceState =
    createJsObject<WGPUStencilFaceState>().apply {
        compare = input.compare.value
        failOp = input.failOp.value
        depthFailOp = input.depthFailOp.value
        passOp = input.passOp.value
    }

private fun map(input: GPUMultisampleState): WGPUMultisampleState =
    createJsObject<WGPUMultisampleState>().apply {
        count = input.count.asJsNumber()
        mask = input.mask.asJsNumber()
        alphaToCoverageEnabled = input.alphaToCoverageEnabled
    }

internal fun map(input: GPUFragmentState): WGPUFragmentState =
    createJsObject<WGPUFragmentState>().apply {
        targets = input.targets.mapJsArray { target -> target?.let { map(it) } }
        module = (input.module as ShaderModule).handler
        input.entryPoint?.let { entryPoint = it }
        constants = mapConstants(input.constants)
    }

private fun map(input: GPUColorTargetState): WGPUColorTargetState =
    createJsObject<WGPUColorTargetState>().apply {
        format = input.format.value
        input.blend?.let { blend = map(it) }
        writeMask = input.writeMask.value.asJsNumber()
    }

private fun map(input: GPUBlendState): WGPUBlendState =
    createJsObject<WGPUBlendState>().apply {
        color = map(input.color)
        alpha = map(input.alpha)
    }

private fun map(input: GPUBlendComponent): WGPUBlendComponent =
    createJsObject<WGPUBlendComponent>().apply {
        operation = input.operation.value
        srcFactor = input.srcFactor.value
        dstFactor = input.dstFactor.value
    }
