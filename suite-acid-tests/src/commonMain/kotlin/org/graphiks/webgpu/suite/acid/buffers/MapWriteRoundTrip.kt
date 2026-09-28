package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.readBufferBytes
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertContentEquals

/**
 * A `MapWrite | CopySrc` buffer accepts CPU writes while mapped: writing `[5, 7, 11, 13]`, unmapping
 * and copying through a staging buffer yields exactly those four words.
 */
@AcidTest(
    id = AcidCaseId.BuffersMapWriteRoundTrip,
    family = AcidFamily.BuffersMapping,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBuffer_mapAsync,
        ApiSymbols.GPUBuffer_getMappedRange,
        ApiSymbols.ArrayBuffer_setUInts,
        ApiSymbols.GPUBuffer_unmap,
        ApiSymbols.GPUCommandEncoder_copyBufferToBuffer,
    ],
)
suspend fun mapWriteRoundTrip(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(
        BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc),
    ).use { buffer ->
        buffer.mapAsync(GPUMapMode.Write).getOrThrow()
        buffer.getMappedRange().setUInts(0uL, uintArrayOf(5u, 7u, 11u, 13u))
        buffer.unmap()

        val words = ArrayBuffer.of(readBufferBytes(device, buffer, 16uL)).toUIntArray()
        assertContentEquals(uintArrayOf(5u, 7u, 11u, 13u), words, "Mapped writes survive unmapping")
    }
}
