package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * `createRenderPipelineAsync` resolves the full-screen red triangle; rendering it over a blue clear
 * leaves the interior red. A pipeline that resolves but does not draw would leave the centre blue,
 * so the result is judged on pixels, not on the Result alone.
 */
@AcidTest(
    id = AcidCaseId.RenderPipelineAsync,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createRenderPipelineAsync,
        ApiSymbols.GPUDevice_createShaderModule,
        ApiSymbols.GPURenderPipelineDescriptor,
        ApiSymbols.GPURenderPipelineDescriptor_vertex,
        ApiSymbols.GPURenderPipelineDescriptor_fragment,
        ApiSymbols.GPUFragmentState_targets,
        ApiSymbols.GPUColorTargetState,
        ApiSymbols.GPUColorTargetState_format,
        ApiSymbols.GPUTextureFormat_RGBA8Unorm,
        ApiSymbols.GPURenderPassColorAttachment_view,
    ],
)
suspend fun asyncRenderPipeline(device: GPUDevice) = withValidationScope(device) {
    device.createShaderModule(ShaderModuleDescriptor(code = FULLSCREEN_TRIANGLE_WGSL)).use { shader ->
        device.createRenderPipelineAsync(
            RenderPipelineDescriptor(
                vertex = VertexState(module = shader, entryPoint = "vertexMain"),
                fragment = FragmentState(
                    module = shader,
                    targets = listOf(ColorTargetState(GPUTextureFormat.RGBA8Unorm)),
                    entryPoint = "fragmentMain",
                ),
            ),
        ).getOrThrow().use { pipeline ->
            createColorTarget(device, 16, 16).use { target ->
                val pixels = renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 1.0, 1.0)) { pass ->
                    pass.setPipeline(pipeline)
                    pass.draw(3u)
                }
                assertPixel(pixels, 16, 8, 8, 255, 0, 0, 255)
            }
        }
    }
}
