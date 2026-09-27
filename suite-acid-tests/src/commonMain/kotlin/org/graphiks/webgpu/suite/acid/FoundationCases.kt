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
)
