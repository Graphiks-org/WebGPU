package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUPrimitiveTopology
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.PrimitiveState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.readRgba8
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val POINT_LINE_SHADER = """
@vertex fn pointMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,1>(vec2f(-0.375, 0.375));
    return vec4f(points[i], 0.5, 1.0);
}

@vertex fn lineMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,2>(vec2f(-0.875, 0.875), vec2f(-0.125, 0.875));
    return vec4f(points[i], 0.5, 1.0);
}

@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
"""

/**
 * A point-list pipeline draws one point at the centre of pixel (2, 2) of an 8x8 target, and a
 * line-list pipeline draws a horizontal line through the centres of the row-0 pixels 0 to 3. Every
 * pixel of the target is compared against exactly the covered set: one pixel for the point, the
 * line pixels its segment enters and exits, and nothing else. The line's endpoint sits at the
 * centre of pixel (3, 0): the diamond-exit rule covers a pixel only when the segment leaves its
 * diamond, so that final pixel stays black — the endpoint rule is observed, not assumed.
 */
@AcidTest(
    id = AcidCaseId.RenderPointLineTopology,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPUPrimitiveState_topology,
        ApiSymbols.GPUPrimitiveTopology_PointList,
        ApiSymbols.GPUPrimitiveTopology_LineList,
        ApiSymbols.GPURenderPassEncoder_draw,
    ],
)
suspend fun pointLineTopology(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(
        device,
        POINT_LINE_SHADER,
        primitive = PrimitiveState(topology = GPUPrimitiveTopology.PointList),
        vertexEntryPoint = "pointMain",
    ).use { points ->
        createRenderPipeline(
            device,
            POINT_LINE_SHADER,
            primitive = PrimitiveState(topology = GPUPrimitiveTopology.LineList),
            vertexEntryPoint = "lineMain",
        ).use { lines ->
            createColorTarget(device, 8, 8).use { target ->
                target.createView().use { view ->
                    device.createCommandEncoder().use { encoder ->
                        val pointPass = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = view,
                                        loadOp = GPULoadOp.Clear,
                                        storeOp = GPUStoreOp.Store,
                                        clearValue = Color(0.0, 0.0, 0.0, 1.0),
                                    ),
                                ),
                            ),
                        )
                        pointPass.setPipeline(points)
                        pointPass.draw(1u)
                        pointPass.end()

                        val linePass = encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = view,
                                        loadOp = GPULoadOp.Load,
                                        storeOp = GPUStoreOp.Store,
                                    ),
                                ),
                            ),
                        )
                        linePass.setPipeline(lines)
                        linePass.draw(2u)
                        linePass.end()

                        encoder.finish().use { device.queue.submit(listOf(it)) }
                    }
                }

                val pixels = readRgba8(device, target, 8, 8)
                // Every pixel of the target is compared against exactly the covered set: the
                // point's own pixel, the line's exited pixels and nothing else.
                for (y in 0 until 8) {
                    for (x in 0 until 8) {
                        val covered = (x == 2 && y == 2) || (y == 0 && x <= 2)
                        if (covered) {
                            assertPixel(pixels, 8, x, y, 255, 0, 0, 255)
                        } else {
                            assertPixel(pixels, 8, x, y, 0, 0, 0, 255)
                        }
                    }
                }
            }
        }
    }
}
