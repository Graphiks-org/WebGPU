package org.graphiks.webgpu.suite.acid.compute

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName
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

private const val F16_SHADER = """
enable f16;

@group(0) @binding(0) var<storage, read_write> output: array<f32>;

@compute @workgroup_size(1)
fn main() {
    output[0] = f32(f16(1.5) + f16(2.0));
}
"""

/**
 * The f16 extension computes in half precision and widens the result: `f16(1.5) + f16(2.0)` is
 * exactly 3.5. Requires the optional `ShaderF16` feature; the runner reports it as `unsupported`
 * otherwise.
 */
@AcidTest(
    id = AcidCaseId.ComputeShaderF16,
    family = AcidFamily.Compute,
    requiredFeatures = [GPUFeatureName.ShaderF16],
    contract = [
        ApiSymbols.GPUFeatureName_ShaderF16,
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
    ],
)
suspend fun halfPrecision(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { output ->
        device.createShaderModule(ShaderModuleDescriptor(code = F16_SHADER)).use { shader ->
            device.createComputePipeline(
                ComputePipelineDescriptor(compute = ProgrammableStage(shader)),
            ).use { pipeline ->
                pipeline.getBindGroupLayout(0u).use { layout ->
                    device.createBindGroup(
                        BindGroupDescriptor(
                            layout = layout,
                            entries = listOf(BindGroupEntry(0u, BufferBinding(output))),
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
        }

        val value = org.graphiks.webgpu.ArrayBuffer.of(readBufferBytes(device, output, 4uL)).toFloatArray()[0]
        assertEquals(3.5f, value, 1e-6f, "f16(1.5) + f16(2.0) is exactly 3.5")
    }
}
