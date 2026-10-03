package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.MultisampleState
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Valid modules but an invalid multisample count (3) make `createRenderPipelineAsync` return a
 * failure: the rejection is delivered on the Result and the Validation scope stays empty.
 */
@AcidTest(
    id = AcidCaseId.ErrorsAsyncRenderRejection,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipelineAsync,
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPURenderPipelineDescriptor,
        ApiSymbols.GPURenderPipelineDescriptor_vertex,
        ApiSymbols.GPURenderPipelineDescriptor_fragment,
        ApiSymbols.GPURenderPipelineDescriptor_multisample,
        ApiSymbols.GPUMultisampleState,
        ApiSymbols.GPUMultisampleState_count,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUErrorFilter_Validation,
    ],
)
suspend fun asyncRenderRejection(device: GPUDevice) {
    device.createShaderModule(ShaderModuleDescriptor(code = FULLSCREEN_TRIANGLE_WGSL)).use { shader ->
        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            val result = device.createRenderPipelineAsync(
                RenderPipelineDescriptor(
                    vertex = VertexState(module = shader, entryPoint = "vertexMain"),
                    fragment = FragmentState(
                        module = shader,
                        targets = listOf(ColorTargetState(GPUTextureFormat.RGBA8Unorm)),
                        entryPoint = "fragmentMain",
                    ),
                    multisample = MultisampleState(count = 3u),
                ),
            )
            // A conforming request fails; a pipeline that slips through is closed before the assertion.
            result.getOrNull()?.close()
            assertTrue(result.isFailure, "A multisample count of 3 must reject async creation")
        } finally {
            assertNull(device.popErrorScope().getOrThrow(), "Async pipeline failure is not a scope error")
        }
    }
}
