package org.graphiks.webgpu.suite

import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPURequestAdapterOptions

/**
 * The execution context handed to a case.
 *
 * [device] is the runner-owned primary device: a case borrows it to exercise the ordinary contract
 * and never closes it. [requestAdapter] asks the runner's binding for an adapter and returns a
 * **fresh** adapter on every call, so a case that must observe adapter capabilities, a device
 * request or its own uncaptured-error callback owns that extra adapter and device and closes them
 * itself. The context is not a fixture service: it adds no resource a case does not ask for.
 */
class AcidContext(
    val device: GPUDevice,
    val requestAdapter: suspend (GPURequestAdapterOptions?) -> Result<GPUAdapter>,
)
