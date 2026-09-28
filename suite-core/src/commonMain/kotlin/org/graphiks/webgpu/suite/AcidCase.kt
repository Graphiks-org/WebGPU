package org.graphiks.webgpu.suite

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName

/**
 * A single validation case for the public WebGPU contract.
 *
 * The runner provides the [GPUDevice]; the case owns and closes every resource it creates.
 * [requiredFeatures] only names optional features a case needs; a capability required by the
 * core contract is never hidden here.
 */
data class AcidCase(
    val id: AcidCaseId,
    val family: AcidFamily,
    val contract: List<String>,
    val requiredFeatures: Set<GPUFeatureName> = emptySet(),
    val run: suspend (GPUDevice) -> Unit,
)
