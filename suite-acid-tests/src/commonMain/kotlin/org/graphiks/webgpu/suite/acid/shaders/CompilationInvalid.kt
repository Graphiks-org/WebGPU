package org.graphiks.webgpu.suite.acid.shaders

import org.graphiks.webgpu.GPUCompilationMessageType
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val INVALID_WGSL = """
@compute @workgroup_size(1)
fn main() {
    let value: u32 = ;
}
"""

/**
 * A shader with a syntax error yields at least one Error-severity compilation message and a
 * validation error on the device. Only the categories are asserted: the message text, count, order
 * and positions are implementation-dependent and are kept for diagnosis, not compared.
 */
@AcidTest(
    id = AcidCaseId.ShadersCompilationInvalid,
    family = AcidFamily.ShadersCompilation,
    contract = [
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPUShaderModule_getCompilationInfo,
        ApiSymbols.GPUCompilationInfo,
        ApiSymbols.GPUCompilationInfo_messages,
        ApiSymbols.GPUCompilationMessage,
        ApiSymbols.GPUCompilationMessage_type,
        ApiSymbols.GPUCompilationMessage_message,
        ApiSymbols.GPUCompilationMessage_lineNum,
        ApiSymbols.GPUCompilationMessage_linePos,
        ApiSymbols.GPUCompilationMessage_offset,
        ApiSymbols.GPUCompilationMessage_length,
        ApiSymbols.GPUCompilationMessageType,
        ApiSymbols.GPUCompilationMessageType_Error,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
    ],
)
suspend fun compilationInvalid(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    var popped = false
    try {
        device.createShaderModule(ShaderModuleDescriptor(code = INVALID_WGSL)).use { shader ->
            val messages = shader.getCompilationInfo().getOrThrow().messages
            assertTrue(
                messages.any { it.type == GPUCompilationMessageType.Error },
                "An invalid shader must report at least one error message, observed $messages",
            )
        }
        val error = device.popErrorScope().getOrThrow()
        popped = true
        assertIs<GPUValidationError>(error, "An invalid shader module must raise a validation error")
    } finally {
        // The scope is still balanced even if an assertion failed before the pop above.
        if (!popped) runCatching { device.popErrorScope() }
    }
}
