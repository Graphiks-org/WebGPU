package org.graphiks.webgpu.suite.acid.errors

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidInput
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertEquals

/**
 * A dedicated device (never the shared one) is observed through `awaitLost` before and after an
 * explicit `close()`: the loss resolves with the destruction reason, and an observer that starts
 * after the loss resolves immediately with the same information. The waits are bounded by a real
 * timeout rather than a sleep.
 */
@AcidTest(
    id = AcidCaseId.ErrorsDeviceLost,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUAdapter_requestDevice,
        ApiSymbols.GPUDeviceDescriptor,
        ApiSymbols.GPUDevice_awaitLost,
        ApiSymbols.GPUDevice_close,
        ApiSymbols.GPUDeviceLostInfo,
        ApiSymbols.GPUDeviceLostInfo_reason,
        ApiSymbols.GPUDeviceLostReason_Destroyed,
    ],
    input = AcidInput.Context,
)
suspend fun deviceLost(context: AcidContext) {
    val adapter = context.requestAdapter(null).getOrThrow()
    try {
        val device = adapter.requestDevice(
            DeviceDescriptor(label = "acid-lost-device-λ"),
        ).getOrThrow()

        coroutineScope {
            val waiting = async { device.awaitLost() }
            device.close()

            val info = withTimeout(5.seconds) { waiting.await().getOrThrow() }
            assertEquals(
                GPUDeviceLostReason.Destroyed,
                info.reason,
                "An explicit close must notify the loss with the destruction reason",
            )

            val late = withTimeout(5.seconds) { device.awaitLost().getOrThrow() }
            assertEquals(
                GPUDeviceLostReason.Destroyed,
                late.reason,
                "An observer that starts after the loss must resolve immediately with the same reason",
            )
        }
    } finally {
        adapter.close()
    }
}
