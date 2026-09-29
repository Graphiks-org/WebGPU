package org.graphiks.webgpu.suite.acid.adapter

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUPowerPreference
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.QueueDescriptor
import org.graphiks.webgpu.descriptors.RequestAdapterOptions
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidInput
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Requests a low-power adapter, reads its capabilities and requests a device from it without any
 * optional feature. The device's features cannot exceed the adapter's, the limits the case pins are
 * positive and bounded by the adapter's, and the device's alignment limits are at least as strict as
 * the adapter's. The adapter's stable identifying fields survive onto the device, and a cleared
 * buffer really reads back zero from a falsely non-zero state.
 *
 * A power hint is only a hint: the case never requires a particular model or a hardware selection.
 */
@AcidTest(
    id = AcidCaseId.AdapterCapabilities,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    contract = [
        ApiSymbols.GPUAdapter,
        ApiSymbols.GPUAdapter_features,
        ApiSymbols.GPUAdapter_limits,
        ApiSymbols.GPUAdapter_info,
        ApiSymbols.GPUAdapter_requestDevice,
        ApiSymbols.GPUAdapterInfo,
        ApiSymbols.GPUAdapterInfo_vendor,
        ApiSymbols.GPUAdapterInfo_architecture,
        ApiSymbols.GPUAdapterInfo_device,
        ApiSymbols.GPUAdapterInfo_description,
        ApiSymbols.GPURequestAdapterOptions,
        ApiSymbols.GPUPowerPreference,
        ApiSymbols.GPUPowerPreference_LowPower,
        ApiSymbols.GPUSupportedFeatures,
        ApiSymbols.GPUDevice_features,
        ApiSymbols.GPUDevice_limits,
        ApiSymbols.GPUDevice_adapterInfo,
        ApiSymbols.GPUDeviceDescriptor,
        ApiSymbols.GPUQueueDescriptor,
        ApiSymbols.GPUUncapturedErrorCallback,
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUCommandEncoder_clearBuffer,
        ApiSymbols.GPUBufferUsage_CopySrc,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUQueue_writeBuffer,
        ApiSymbols.GPUQueue_onSubmittedWorkDone,
    ],
    input = AcidInput.Context,
)
suspend fun capabilities(context: AcidContext) {
    val adapter = context.requestAdapter(
        RequestAdapterOptions(powerPreference = GPUPowerPreference.LowPower),
    ).getOrThrow()
    try {
        val adapterFeatures = adapter.features
        val adapterLimits = adapter.limits
        val adapterInfo = adapter.info

        val unexpected = CompletableDeferred<GPUError>()
        val device = adapter.requestDevice(
            DeviceDescriptor(
                label = "acid-device-λ",
                defaultQueue = QueueDescriptor(label = "acid-queue-λ"),
                onUncapturedError = GPUUncapturedErrorCallback { unexpected.complete(it) },
            ),
        ).getOrThrow()
        try {
            assertEquals("acid-device-λ", device.label, "The device keeps its descriptor label")
            assertEquals("acid-queue-λ", device.queue.label, "The default queue keeps its descriptor label")

            // Requested without optional features, the device may enable nothing the adapter lacks.
            assertTrue(
                device.features.all { it in adapterFeatures },
                "Device features ${device.features} must be a subset of adapter features $adapterFeatures",
            )

            val limits = device.limits
            assertTrue(limits.maxTextureDimension2D > 0u, "maxTextureDimension2D must be positive")
            assertTrue(
                limits.maxTextureDimension2D <= adapterLimits.maxTextureDimension2D,
                "Device maxTextureDimension2D ${limits.maxTextureDimension2D} exceeds the adapter's ${adapterLimits.maxTextureDimension2D}",
            )
            assertTrue(limits.maxBindGroups > 0u, "maxBindGroups must be positive")
            assertTrue(
                limits.maxBindGroups <= adapterLimits.maxBindGroups,
                "Device maxBindGroups ${limits.maxBindGroups} exceeds the adapter's ${adapterLimits.maxBindGroups}",
            )
            assertTrue(limits.maxBufferSize > 0uL, "maxBufferSize must be positive")
            assertTrue(
                limits.maxBufferSize <= adapterLimits.maxBufferSize,
                "Device maxBufferSize ${limits.maxBufferSize} exceeds the adapter's ${adapterLimits.maxBufferSize}",
            )
            assertTrue(
                limits.minUniformBufferOffsetAlignment >= adapterLimits.minUniformBufferOffsetAlignment,
                "Device uniform alignment ${limits.minUniformBufferOffsetAlignment} must be at least the adapter's ${adapterLimits.minUniformBufferOffsetAlignment}",
            )
            assertTrue(
                limits.minStorageBufferOffsetAlignment >= adapterLimits.minStorageBufferOffsetAlignment,
                "Device storage alignment ${limits.minStorageBufferOffsetAlignment} must be at least the adapter's ${adapterLimits.minStorageBufferOffsetAlignment}",
            )

            val info = device.adapterInfo
            assertEquals(adapterInfo.vendor, info.vendor, "adapterInfo.vendor is stable across the device")
            assertEquals(adapterInfo.architecture, info.architecture, "adapterInfo.architecture is stable across the device")
            assertEquals(adapterInfo.device, info.device, "adapterInfo.device is stable across the device")
            assertEquals(adapterInfo.description, info.description, "adapterInfo.description is stable across the device")

            // Fill the buffer with 0xff first: a clear that is silently ignored would then leave the
            // sentinel visible instead of coinciding with the zero-initialised contents.
            device.createBuffer(
                BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.CopySrc),
            ).use { buffer ->
                device.queue.writeBuffer(buffer, 0uL, ArrayBuffer.of(ByteArray(16) { 0xFF.toByte() }))
                device.createCommandEncoder().use { encoder ->
                    encoder.clearBuffer(buffer, 0uL, 16uL)
                    encoder.finish().use { device.queue.submit(listOf(it)) }
                }
                val bytes = readBufferBytes(device, buffer, 16uL)
                assertTrue(
                    bytes.all { it == 0.toByte() },
                    "clearBuffer must zero every byte, observed ${bytes.toList()}",
                )
            }

            device.queue.onSubmittedWorkDone().getOrThrow()
        } finally {
            device.close()
        }

        // The runner drains 50 ms for the borrowed device; this dedicated device must stay quiet too.
        delay(50)
        assertFalse(
            unexpected.isCompleted,
            "A valid device request and a cleared buffer must not report an uncaptured error",
        )
    } finally {
        adapter.close()
    }
}
