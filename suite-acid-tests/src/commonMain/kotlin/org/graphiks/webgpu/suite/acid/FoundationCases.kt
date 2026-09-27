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
)
