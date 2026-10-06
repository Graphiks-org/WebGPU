package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `buffer.usage` is a typed mask: recreating a buffer with `BufferDescriptor(size = buffer.size,
 * usage = buffer.usage)` yields a buffer reporting the same mask, bit for bit.
 */
@AcidTest(
    id = AcidCaseId.BuffersUsageMaskRoundTrip,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBuffer_size,
        ApiSymbols.GPUBuffer_usage,
        ApiSymbols.GPUBufferUsage,
        ApiSymbols.GPUBufferUsage_CopySrc,
        ApiSymbols.GPUBufferUsage_CopyDst,
        ApiSymbols.GPUBufferUsage_Storage,
    ],
)
suspend fun usageMaskRoundTrip(device: GPUDevice) = withValidationScope(device) {
    val created = GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst or GPUBufferUsage.Storage
    device.createBuffer(
        BufferDescriptor(size = 32uL, usage = created),
    ).use { source ->
        assertEquals(created, source.usage)
        assertTrue(GPUBufferUsage.CopySrc in source.usage)
        assertTrue(GPUBufferUsage.Storage in source.usage)

        device.createBuffer(
            BufferDescriptor(size = source.size, usage = source.usage),
        ).use { copy ->
            assertEquals(source.size, copy.size)
            assertEquals(source.usage, copy.usage)
            assertEquals(created.value, copy.usage.value)
        }
    }
}
