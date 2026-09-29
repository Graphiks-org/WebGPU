package org.graphiks.webgpu.suite.acid.shaders

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCompilationMessageType
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val VALID_WRITER_SHADER = """
@group(0) @binding(0) var<storage, read_write> value: array<u32>;

@compute @workgroup_size(1)
fn main() {
    value[0] = 37u;
}
"""

/**
 * A valid shader module reports no Error-severity compilation message, and the module actually
 * builds a pipeline that runs: "compiled with no error" and "does nothing" are separated by reading
 * 37 back. Warnings and informational messages are allowed.
 */
@AcidTest(
    id = AcidCaseId.ShadersCompilationValid,
    family = AcidFamily.ShadersCompilation,
    contract = [
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPUShaderModule_getCompilationInfo,
        ApiSymbols.GPUCompilationInfo,
        ApiSymbols.GPUCompilationInfo_messages,
        ApiSymbols.GPUCompilationMessage,
        ApiSymbols.GPUCompilationMessage_type,
        ApiSymbols.GPUCompilationMessage_message,
        ApiSymbols.GPUCompilationMessageType,
        ApiSymbols.GPUCompilationMessageType_Error,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUBufferUsage_Storage,
        ApiSymbols.GPUBufferUsage_CopySrc,
    ],
)
suspend fun compilationValid(device: GPUDevice) = withValidationScope(device) {
    device.createShaderModule(ShaderModuleDescriptor(code = VALID_WRITER_SHADER)).use { shader ->
        val messages = shader.getCompilationInfo().getOrThrow().messages
        assertTrue(
            messages.none { it.type == GPUCompilationMessageType.Error },
            "A valid shader must report no error message, observed ${messages.map { it.type to it.message }}",
        )

        device.createBuffer(
            BufferDescriptor(4uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
        ).use { storage ->
            device.createComputePipeline(
                ComputePipelineDescriptor(compute = ProgrammableStage(shader)),
            ).use { pipeline ->
                pipeline.getBindGroupLayout(0u).use { layout ->
                    device.createBindGroup(
                        BindGroupDescriptor(
                            layout = layout,
                            entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
                        ),
                    ).use { group ->
                        device.createCommandEncoder().use { encoder ->
                            val pass = encoder.beginComputePass()
                            pass.setPipeline(pipeline)
                            pass.setBindGroup(0u, group)
                            pass.dispatchWorkgroups(1u)
                            pass.end()
                            encoder.finish().use { device.queue.submit(listOf(it)) }
                        }
                    }
                }
            }

            val bytes = readBufferBytes(device, storage, 4uL)
            assertEquals(37, bytes[0].toInt() and 255, "The valid shader must still run and write 37")
            assertEquals(0, bytes[1].toInt() and 255)
            assertEquals(0, bytes[2].toInt() and 255)
            assertEquals(0, bytes[3].toInt() and 255)
        }
    }
}
