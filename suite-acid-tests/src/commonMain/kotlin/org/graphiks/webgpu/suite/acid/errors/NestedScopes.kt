package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Two nested validation scopes each capture their own operations: the invalid buffer creation is
 * reported by the inner scope, and the outer scope resolves to null.
 */
@AcidTest(
    id = AcidCaseId.ErrorsNestedScopes,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUDevice_createBuffer,
    ],
)
suspend fun nestedScopes(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
        } finally {
            val inner = device.popErrorScope()
            assertTrue(inner.isSuccess)
            assertIs<GPUValidationError>(inner.getOrThrow())
        }
    } finally {
        val outer = device.popErrorScope()
        assertTrue(outer.isSuccess)
        assertNull(outer.getOrThrow())
    }
}
