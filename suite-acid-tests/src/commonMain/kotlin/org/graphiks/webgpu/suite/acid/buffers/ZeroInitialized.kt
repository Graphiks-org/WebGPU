package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

/**
 * A buffer is zero-initialised: a 64-byte `CopySrc` buffer read back through a staging copy holds
 * 64 zero bytes. Nothing writes to it before the read, and the staging buffer is prefilled with
 * `0xff`, so a copy that is silently dropped shows the sentinel instead of passing as zeros.
 */
@AcidTest(
    id = AcidCaseId.BuffersZeroInitialized,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_getMappedRange,
    ],
)
suspend fun zeroInitialized(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(64uL, GPUBufferUsage.CopySrc)).use { buffer ->
        val bytes = readBufferBytes(device, buffer, 64uL)
        assertContentEquals(ByteArray(64), bytes, "A freshly created buffer must read back as zeros")
    }
}
