package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.StorageTextureBindingLayout
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val STORAGE_WRITE_SHADER = """
@group(0) @binding(0) var image: texture_storage_2d<rgba8unorm, write>;

@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    let colors = array<vec4f,4>(
        vec4f(1, 0, 0, 1),
        vec4f(0, 1, 0, 1),
        vec4f(0, 0, 1, 1),
        vec4f(1, 1, 1, 1),
    );
    textureStore(image, vec2i(id.xy), colors[id.y * 2u + id.x]);
}
"""

/**
 * A storage texture is written by a compute dispatch: a 2×2 dispatch with `workgroup_size(1)` stores
 * red, green, blue and white by coordinate, and the readback returns exactly those four texels.
 */
@AcidTest(
    id = AcidCaseId.TexturesStorageWrite,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureUsage_StorageBinding,
        ApiSymbols.GPUStorageTextureBindingLayout,
        ApiSymbols.GPUStorageTextureBindingLayout_access,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
    ],
)
suspend fun storageWrite(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.StorageBinding or GPUTextureUsage.CopySrc,
        ),
    ).use { texture ->
        device.createShaderModule(ShaderModuleDescriptor(code = STORAGE_WRITE_SHADER)).use { shader ->
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    entries = listOf(
                        BindGroupLayoutEntry(
                            binding = 0u,
                            visibility = GPUShaderStage.Compute,
                            storageTexture = StorageTextureBindingLayout(
                                format = GPUTextureFormat.RGBA8Unorm,
                                viewDimension = GPUTextureViewDimension.TwoD,
                            ),
                        ),
                    ),
                ),
            ).use { layout ->
                device.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                    device.createComputePipeline(
                        ComputePipelineDescriptor(compute = ProgrammableStage(shader), layout = pipelineLayout),
                    ).use { pipeline ->
                        texture.createView().use { view ->
                            device.createBindGroup(
                                BindGroupDescriptor(layout = layout, entries = listOf(BindGroupEntry(0u, view))),
                            ).use { group ->
                                device.createCommandEncoder().use { encoder ->
                                    val pass = encoder.beginComputePass()
                                    pass.setPipeline(pipeline)
                                    pass.setBindGroup(0u, group)
                                    pass.dispatchWorkgroups(2u, 2u)
                                    pass.end()
                                    encoder.finish().use { device.queue.submit(listOf(it)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        val pixels = readRgba8(device, texture, 2, 2)
        assertPixel(pixels, 2, 0, 0, 255, 0, 0, 255)
        assertPixel(pixels, 2, 1, 0, 0, 255, 0, 255)
        assertPixel(pixels, 2, 0, 1, 0, 0, 255, 255)
        assertPixel(pixels, 2, 1, 1, 255, 255, 255, 255)
    }
}
