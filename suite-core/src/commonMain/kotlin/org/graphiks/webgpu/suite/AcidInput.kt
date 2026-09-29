package org.graphiks.webgpu.suite

/**
 * Selects what a case function declares as its argument.
 *
 * `Device` cases take the runner's borrowed `GPUDevice`, the common shape for the catalogue.
 * `Context` cases take the full [AcidContext] because they must ask for their own adapter or
 * device to observe capabilities, a device request's result or an uncaptured-error callback.
 */
enum class AcidInput { Device, Context }
