package org.graphiks.webgpu.browser.mapper

import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.bindings.WGPUDeviceLostInfo

/**
 * Converts the browser loss notification into the portable info object.
 *
 * A reason the portable enumeration does not know maps to [GPUDeviceLostReason.Unknown]: a
 * non-portable backend value must not be confused with a voluntary destruction.
 */
internal fun mapDeviceLostInfo(input: WGPUDeviceLostInfo): GPUDeviceLostInfo = DeviceLostInfo(
    reason = GPUDeviceLostReason.of(input.reason) ?: GPUDeviceLostReason.Unknown,
    message = input.message,
)

private data class DeviceLostInfo(
    override val reason: GPUDeviceLostReason,
    override val message: String,
) : GPUDeviceLostInfo
