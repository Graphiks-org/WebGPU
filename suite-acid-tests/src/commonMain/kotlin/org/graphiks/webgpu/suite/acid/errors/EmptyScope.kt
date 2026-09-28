package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A validation scope with no operation inside resolves to `null` and does not fail.
 */
@AcidTest(
    id = AcidCaseId.ErrorsEmptyScope,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun emptyErrorScope(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    val result = device.popErrorScope()
    assertTrue(result.isSuccess)
    assertNull(result.getOrThrow())
}
