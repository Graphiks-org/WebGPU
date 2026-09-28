package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.FULLSCREEN_TRIANGLE_WGSL
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

/**
 * `drawIndirect` takes its arguments from a buffer: `[3, 1, 0, 0]` draws the red triangle, while a
 * second argument set `[0, 1, 0, 0]` draws nothing and leaves the target black.
 */
@AcidTest(
    id = AcidCaseId.RenderDrawIndirect,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUDevice_createBuffer,
        ApiSymbols.GPUBufferUsage_Indirect,
        ApiSymbols.GPURenderCommandsMixin_drawIndirect,
        ApiSymbols.GPUDevice_createRenderPipeline,
    ],
)
suspend fun drawIndirect(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(32uL, GPUBufferUsage.Indirect or GPUBufferUsage.CopyDst)).use { args ->
        device.queue.writeBuffer(
            args,
            0uL,
            ArrayBuffer.of(uintArrayOf(3u, 1u, 0u, 0u, 0u, 1u, 0u, 0u)),
        )
        createRenderPipeline(device, FULLSCREEN_TRIANGLE_WGSL).use { pipeline ->
            createColorTarget(device, 16, 16).use { drawn ->
                createColorTarget(device, 16, 16).use { skipped ->
                    val drawnPixels = renderAndRead(device, drawn, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                        pass.setPipeline(pipeline)
                        pass.drawIndirect(args, 0uL)
                    }
                    val skippedPixels = renderAndRead(device, skipped, 16, 16, Color(0.0, 0.0, 0.0, 1.0)) { pass ->
                        pass.setPipeline(pipeline)
                        pass.drawIndirect(args, 16uL)
                    }
                    assertPixel(drawnPixels, 16, 8, 8, 255, 0, 0, 255)
                    assertPixel(skippedPixels, 16, 8, 8, 0, 0, 0, 255)
                }
            }
        }
    }
}
