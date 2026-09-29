package org.graphiks.webgpu.suite

import org.graphiks.webgpu.GPUFeatureName

/**
 * Declares the inventory metadata of a validation case next to its implementation. The code is the
 * source of truth: the typed annotation feeds the generated inventory, while the texts live in the
 * localized resources keyed by [id].
 *
 * [contract] references the generated `ApiSymbols` constants, so an unknown contract member does
 * not compile. [input] selects the case function's argument: the default borrowed `GPUDevice`, or
 * the full [AcidContext] for a case that must request its own adapter or device.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class AcidTest(
    val id: AcidCaseId,
    val family: AcidFamily,
    val contract: Array<String>,
    val requiredFeatures: Array<GPUFeatureName> = [],
    val input: AcidInput = AcidInput.Device,
)
