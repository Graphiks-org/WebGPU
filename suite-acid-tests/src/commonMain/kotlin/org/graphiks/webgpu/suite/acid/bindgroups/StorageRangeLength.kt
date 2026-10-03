package org.graphiks.webgpu.suite.acid.bindgroups

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferBindingLayout
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

private const val RANGE_LENGTH_SHADER = """
@group(0) @binding(0) var<storage, read> a: array<u32>;
@group(0) @binding(1) var<storage, read> b: array<u32>;
@group(0) @binding(2) var<storage, read_write> out: array<u32>;

@compute @workgroup_size(1)
fn main() {
    out[0] = arrayLength(&a);
    out[1] = a[0];
    out[2] = arrayLength(&b);
    out[3] = b[0];
}
"""

/**
 * A buffer binding's explicit size bounds its runtime array length, and its offset selects the first
 * word, even when the backing buffer is larger than either bound range: with a backing buffer of
 * `alignment + 48` bytes, binding a (offset 0, size 16) reports `arrayLength` 4 and binding b (offset
 * at the storage alignment, size 32) reports 8 with its first word taken from that offset. Omitting
 * the explicit size would report the remaining buffer words instead, and ignoring the offset would
 * read the wrong first word.
 */
@AcidTest(
    id = AcidCaseId.BindingsStorageRangeLength,
    family = AcidFamily.BindGroupsLayouts,
    contract = [
        ApiSymbols.GPUDevice_createBindGroupLayout,
        ApiSymbols.GPUDevice_createPipelineLayout,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUBufferBindingLayout_minBindingSize,
        ApiSymbols.GPUBufferBinding_offset,
        ApiSymbols.GPUBufferBinding_size,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
    ],
)
suspend fun storageRangeLength(device: GPUDevice) = withValidationScope(device) {
    val alignment = device.limits.minStorageBufferOffsetAlignment.toULong()
    val bufferBytes = alignment + 48uL
    val words = UIntArray((bufferBytes / 4uL).toInt()) { index -> 100u + index.toUInt() }
    words[0] = 7u
    words[(alignment / 4uL).toInt()] = 29u

    device.createBuffer(
        BufferDescriptor(bufferBytes, GPUBufferUsage.Storage or GPUBufferUsage.CopyDst),
    ).use { source ->
        device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { output ->
            device.queue.writeBuffer(source, 0uL, ArrayBuffer.of(words))
            device.createShaderModule(ShaderModuleDescriptor(code = RANGE_LENGTH_SHADER)).use { shader ->
                device.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.ReadOnlyStorage, minBindingSize = 16uL),
                            ),
                            BindGroupLayoutEntry(
                                binding = 1u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.ReadOnlyStorage, minBindingSize = 32uL),
                            ),
                            BindGroupLayoutEntry(
                                binding = 2u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, minBindingSize = 16uL),
                            ),
                        ),
                    ),
                ).use { layout ->
                    device.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                        device.createComputePipeline(
                            ComputePipelineDescriptor(compute = ProgrammableStage(shader), layout = pipelineLayout),
                        ).use { pipeline ->
                            device.createBindGroup(
                                BindGroupDescriptor(
                                    layout = layout,
                                    entries = listOf(
                                        BindGroupEntry(0u, BufferBinding(source, offset = 0uL, size = 16uL)),
                                        BindGroupEntry(1u, BufferBinding(source, offset = alignment, size = 32uL)),
                                        BindGroupEntry(2u, BufferBinding(output)),
                                    ),
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
            }
            assertContentEquals(
                uintArrayOf(4u, 7u, 8u, 29u),
                ArrayBuffer.of(readBufferBytes(device, output, 16uL)).toUIntArray(),
                "Explicit binding sizes must bound arrayLength and the offset must select the first word",
            )
        }
    }
}
