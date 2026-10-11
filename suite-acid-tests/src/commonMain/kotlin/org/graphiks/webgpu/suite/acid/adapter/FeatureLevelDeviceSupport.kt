package org.graphiks.webgpu.suite.acid.adapter

import kotlinx.coroutines.delay
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import kotlin.test.assertTrue

/** Owned devices must drain their own queue and error callbacks, not the runner's borrowed device. */
internal suspend fun withFeatureLevelDevice(adapter: GPUAdapter, block: suspend (GPUDevice) -> Unit) {
    val unexpected = mutableListOf<GPUError>()
    adapter.requestDevice(
        DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback { unexpected += it }),
    ).getOrThrow().use { device ->
        try {
            block(device)
        } finally {
            // This runs even before an unsupported Core fallback can escape to the runner.
            device.queue.onSubmittedWorkDone().getOrThrow()
            delay(50)
            assertTrue(unexpected.isEmpty(), "Unexpected uncaptured errors: ${unexpected.joinToString { it.message }}")
        }
    }
}
