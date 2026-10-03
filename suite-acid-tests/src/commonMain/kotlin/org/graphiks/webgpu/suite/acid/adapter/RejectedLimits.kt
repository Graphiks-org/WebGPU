package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidInput
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertTrue

/**
 * A device request whose `maxComputeWorkgroupSizeX` is one above the adapter's own limit must fail
 * instead of silently returning a device that ignores the requested bound. No large resource is
 * allocated and the adapter is not reused for a second request.
 */
@AcidTest(
    id = AcidCaseId.AdapterRejectedLimits,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    contract = [
        ApiSymbols.GPUAdapter_requestDevice,
        ApiSymbols.GPUAdapter_limits,
        ApiSymbols.GPUSupportedLimits,
        ApiSymbols.GPUSupportedLimits_maxComputeWorkgroupSizeX,
        ApiSymbols.GPUDeviceDescriptor,
        ApiSymbols.GPUDeviceDescriptor_requiredLimits,
    ],
    input = AcidInput.Context,
)
suspend fun rejectedLimits(context: AcidContext) {
    val adapter = context.requestAdapter(null).getOrThrow()
    try {
        val base = adapter.limits.maxComputeWorkgroupSizeX
        assertTrue(
            base < UInt.MAX_VALUE,
            "maxComputeWorkgroupSizeX cannot be the maximum UInt for an excess request to exist",
        )
        val excess = base + 1u
        val requested = object : GPUSupportedLimits by adapter.limits {
            override val maxComputeWorkgroupSizeX = excess
        }
        val result = adapter.requestDevice(
            DeviceDescriptor(requiredLimits = requested),
        )
        // A conforming request fails; a device that slips through is closed before the assertion.
        result.getOrNull()?.close()
        assertTrue(
            result.isFailure,
            "Requesting maxComputeWorkgroupSizeX=$excess above the adapter limit $base must fail",
        )
    } finally {
        adapter.close()
    }
}
