package org.graphiks.webgpu.suite

import org.graphiks.webgpu.GPUFeatureName

/**
 * A single validation case for the public WebGPU contract.
 *
 * The runner provides an [AcidContext]; the case owns and closes every resource it creates beyond
 * the borrowed `context.device`. [requiredFeatures] only names optional features a case needs; a
 * capability required by the core contract is never hidden here.
 */
data class AcidCase(
    val id: AcidCaseId,
    val family: AcidFamily,
    val contract: List<String>,
    val requiredFeatures: Set<GPUFeatureName> = emptySet(),
    val run: suspend (AcidContext) -> Unit,
)
