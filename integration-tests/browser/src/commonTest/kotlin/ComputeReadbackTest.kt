@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.integration

import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

private val computeShaderCode = """
    override scale: u32 = 1u;
    @group(0) @binding(0) var<storage, read_write> output: array<u32>;
    @compute @workgroup_size(1)
    fn main(@builtin(global_invocation_id) id: vec3<u32>) {
        output[id.x] = (id.x + 1u) * scale;
    }
""".trimIndent()

class ComputeReadbackTest {

    @Test
    fun computesAndReadsBack() = runTest(timeout = 60.seconds) {
        val adapter = requestAdapter().getOrThrow()
        val device = adapter.requestDevice().getOrThrow()
        val storage = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc))
        val staging = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
        try {
            device.pushErrorScope(GPUErrorFilter.Validation)
            val shader = device.createShaderModule(ShaderModuleDescriptor(code = computeShaderCode))
            val pipeline = device.createComputePipeline(
                ComputePipelineDescriptor(
                    compute = ProgrammableStage(shader, entryPoint = "main", constants = mapOf("scale" to 7.0)),
                ),
            )
            val group = device.createBindGroup(
                BindGroupDescriptor(
                    layout = pipeline.getBindGroupLayout(0u),
                    entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
                ),
            )
            val encoder = device.createCommandEncoder()
            val pass = encoder.beginComputePass()
            pass.setPipeline(pipeline)
            pass.setBindGroup(0u, group)
            pass.dispatchWorkgroups(4u)
            pass.end()
            encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
            device.queue.submit(listOf(encoder.finish()))
            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            val actual = staging.getMappedRange().toUIntArray()
            assertContentEquals(uintArrayOf(7u, 14u, 21u, 28u), actual)
            staging.unmap()
            assertNull(device.popErrorScope().getOrThrow())
        } finally {
            staging.close()
            storage.close()
            device.close()
            adapter.close()
        }
    }
}
