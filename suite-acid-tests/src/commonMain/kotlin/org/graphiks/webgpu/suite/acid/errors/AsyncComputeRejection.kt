package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val VALID_EMPTY_COMPUTE = """
@compute @workgroup_size(1)
fn main() {}
"""

/**
 * a valid module but an entry point that does not exist makes `createComputePipelineAsync` return a
 * failure, not a validation error on the scope: the rejection is reported on the Result, and the
 * Validation scope stays empty. The contract publishes no `GPUPipelineError` type, so no cast to one
 * and no message comparison is attempted.
 */
@AcidTest(
    id = AcidCaseId.ErrorsAsyncComputeRejection,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_createComputePipelineAsync,
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPUComputePipelineDescriptor,
        ApiSymbols.GPUComputePipelineDescriptor_compute,
        ApiSymbols.GPUProgrammableStage,
        ApiSymbols.GPUProgrammableStage_entryPoint,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
    ],
)
suspend fun asyncComputeRejection(device: GPUDevice) {
    device.createShaderModule(ShaderModuleDescriptor(code = VALID_EMPTY_COMPUTE)).use { module ->
        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            val result = device.createComputePipelineAsync(
                ComputePipelineDescriptor(
                    compute = ProgrammableStage(module = module, entryPoint = "missing"),
                ),
            )
            assertTrue(result.isFailure, "A missing entry point must reject async creation")
        } finally {
            assertNull(device.popErrorScope().getOrThrow(), "Async pipeline failure is not a scope error")
        }
    }
}
