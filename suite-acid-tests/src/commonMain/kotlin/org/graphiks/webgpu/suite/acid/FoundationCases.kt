package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.suite.AcidCase

/**
 * The list of foundation cases the browser runner executes.
 *
 * The catalogue is explicit and ordered; every case here is also listed in
 * `inventory/foundation-case-ids.json`.
 */
fun foundationCases(): List<AcidCase> = listOf(
    AcidCase(
        id = "buffers.mapped-at-creation",
        title = "Write initial buffer contents",
        contract = listOf(
            "GPUDevice.createBuffer",
            "GPUBuffer.getMappedRange",
            "GPUBuffer.unmap",
            "GPUBuffer.size",
            "GPUBuffer.usage",
            "GPUBuffer.mapState",
        ),
        run = ::mappedAtCreation,
    ),
    AcidCase(
        id = "transfers.copy-offsets",
        title = "Copy a byte sub-range between buffers",
        contract = listOf(
            "GPUDevice.createBuffer",
            "GPUCommandEncoder.copyBufferToBuffer",
            "GPUCommandEncoder.finish",
            "GPUQueue.submit",
            "GPUBuffer.getMappedRange",
            "GPUBuffer.mapAsync",
            "GPUBuffer.unmap",
        ),
        run = ::bufferCopyOffsets,
    ),
    AcidCase(
        id = "transfers.write-offsets",
        title = "Write a data sub-range through the queue",
        contract = listOf(
            "GPUDevice.createBuffer",
            "GPUQueue.writeBuffer",
            "GPUCommandEncoder.copyBufferToBuffer",
            "GPUBuffer.mapAsync",
            "GPUBuffer.getMappedRange",
            "GPUBuffer.unmap",
        ),
        run = ::queueWriteOffsets,
    ),
    AcidCase(
        id = "transfers.write-remaining",
        title = "Write from a data offset to the end of the data",
        contract = listOf(
            "GPUDevice.createBuffer",
            "GPUQueue.writeBuffer",
            "GPUCommandEncoder.copyBufferToBuffer",
            "GPUBuffer.mapAsync",
            "GPUBuffer.getMappedRange",
            "GPUBuffer.unmap",
        ),
        run = ::queueWriteRemaining,
    ),
    AcidCase(
        id = "buffers.partial-map-remap",
        title = "Read a mapped sub-range then remap the whole buffer",
        contract = listOf(
            "GPUDevice.createBuffer",
            "GPUBuffer.mapAsync",
            "GPUBuffer.getMappedRange",
            "GPUBuffer.unmap",
            "GPUBuffer.mapState",
        ),
        run = ::partialMapping,
    ),
    AcidCase(
        id = "compute.auto-layout-constants",
        title = "Dispatch with an automatic layout and a specialization constant",
        contract = listOf(
            "GPUDevice.createShaderModule",
            "GPUDevice.createComputePipeline",
            "GPUComputePipeline.getBindGroupLayout",
            "GPUDevice.createBindGroup",
            "GPUComputePassEncoder.setPipeline",
            "GPUComputePassEncoder.setBindGroup",
            "GPUComputePassEncoder.dispatchWorkgroups",
            "GPUComputePassEncoder.end",
            "GPUProgrammableStage.constants",
        ),
        run = ::computeAutoLayout,
    ),
    AcidCase(
        id = "compute.explicit-layout-entrypoint",
        title = "Dispatch with an explicit layout and an inferred entry point",
        contract = listOf(
            "GPUDevice.createBindGroupLayout",
            "GPUDevice.createPipelineLayout",
            "GPUDevice.createComputePipeline",
            "GPUDevice.createBindGroup",
            "GPUComputePassEncoder.dispatchWorkgroups",
            "GPUBufferBindingLayout.minBindingSize",
        ),
        run = ::computeExplicitLayout,
    ),
    AcidCase(
        id = "errors.empty-scope",
        title = "An empty validation scope resolves to null",
        contract = listOf(
            "GPUDevice.pushErrorScope",
            "GPUDevice.popErrorScope",
        ),
        run = ::emptyErrorScope,
    ),
    AcidCase(
        id = "errors.invalid-buffer-usage",
        title = "A buffer with no usage reports a validation error",
        contract = listOf(
            "GPUDevice.createBuffer",
            "GPUDevice.pushErrorScope",
            "GPUDevice.popErrorScope",
        ),
        run = ::invalidBufferUsage,
    ),
    AcidCase(
        id = "errors.map-alignment",
        title = "An unaligned mapping offset fails validation",
        contract = listOf(
            "GPUBuffer.mapAsync",
            "GPUBuffer.mapState",
            "GPUDevice.pushErrorScope",
            "GPUDevice.popErrorScope",
        ),
        run = ::invalidMapAlignment,
    ),
    AcidCase(
        id = "buffers.map-destroyed",
        title = "Mapping a destroyed buffer reports a validation error",
        contract = listOf(
            "GPUDevice.createBuffer",
            "GPUBuffer.mapAsync",
            "GPUBuffer.close",
            "GPUDevice.pushErrorScope",
            "GPUDevice.popErrorScope",
        ),
        run = ::mappingDestroyedBuffer,
    ),
)
