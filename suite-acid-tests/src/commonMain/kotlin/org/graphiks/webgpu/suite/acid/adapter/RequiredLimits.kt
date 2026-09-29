package org.graphiks.webgpu.suite.acid.adapter

import kotlinx.coroutines.CompletableDeferred
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.QueueDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidInput
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val WRITE_37_SHADER = """
@group(0) @binding(0) var<storage, read_write> value: array<u32>;

@compute @workgroup_size(1)
fn main() {
    value[0] = 37u;
}
"""

/**
 * Requests a device whose `maxComputeWorkgroupSizeX` equals this adapter's own limit, the other
 * fields delegated to the same adapter. The request succeeds, the device reports at least the
 * requested bound, and a `workgroup_size(1)` kernel actually runs and writes 37. The sibling
 * `device.reject-excess-limit` proves the field is honoured rather than ignored.
 */
@AcidTest(
    id = AcidCaseId.AdapterRequiredLimits,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    contract = [
        ApiSymbols.GPUAdapter_requestDevice,
        ApiSymbols.GPUAdapter_limits,
        ApiSymbols.GPUSupportedLimits,
        ApiSymbols.GPUSupportedLimits_maxComputeWorkgroupSizeX,
        ApiSymbols.GPUDevice_limits,
        ApiSymbols.GPUDeviceDescriptor,
        ApiSymbols.GPUDeviceDescriptor_requiredLimits,
        ApiSymbols.GPUQueueDescriptor,
        ApiSymbols.GPUUncapturedErrorCallback,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPUDevice_createComputePipeline,
        ApiSymbols.GPUComputePipeline_getBindGroupLayout,
        ApiSymbols.GPUDevice_createBindGroup,
        ApiSymbols.GPUDevice_createCommandEncoder,
        ApiSymbols.GPUCommandEncoder_beginComputePass,
        ApiSymbols.GPUComputePassEncoder_setPipeline,
        ApiSymbols.GPUComputePassEncoder_setBindGroup,
        ApiSymbols.GPUComputePassEncoder_dispatchWorkgroups,
        ApiSymbols.GPUComputePassEncoder_end,
        ApiSymbols.GPUBufferUsage_Storage,
        ApiSymbols.GPUBufferUsage_CopySrc,
    ],
    input = AcidInput.Context,
)
suspend fun requiredLimits(context: AcidContext) {
    val adapter = context.requestAdapter(null).getOrThrow()
    try {
        val requested = object : GPUSupportedLimits by adapter.limits {
            override val maxComputeWorkgroupSizeX = adapter.limits.maxComputeWorkgroupSizeX
        }
        val unexpected = CompletableDeferred<GPUError>()
        adapter.requestDevice(
            DeviceDescriptor(
                requiredLimits = requested,
                label = "acid-device-λ",
                defaultQueue = QueueDescriptor(label = "acid-queue-λ"),
                onUncapturedError = GPUUncapturedErrorCallback { unexpected.complete(it) },
            ),
        ).getOrThrow().use { device ->
            assertTrue(
                device.limits.maxComputeWorkgroupSizeX >= requested.maxComputeWorkgroupSizeX,
                "Device maxComputeWorkgroupSizeX ${device.limits.maxComputeWorkgroupSizeX} is below the requested ${requested.maxComputeWorkgroupSizeX}",
            )

            device.createBuffer(
                BufferDescriptor(4uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
            ).use { storage ->
                device.createShaderModule(ShaderModuleDescriptor(code = WRITE_37_SHADER)).use { shader ->
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
                }

                val bytes = readBufferBytes(device, storage, 4uL)
                assertEquals(37, bytes[0].toInt() and 255, "The workgroup_size(1) kernel must write 37")
                assertEquals(0, bytes[1].toInt() and 255)
                assertEquals(0, bytes[2].toInt() and 255)
                assertEquals(0, bytes[3].toInt() and 255)
            }

            device.queue.onSubmittedWorkDone().getOrThrow()
            assertTrue(
                !unexpected.isCompleted,
                "Requesting a limit equal to the adapter's must not report an uncaptured error",
            )
        }
    } finally {
        adapter.close()
    }
}
