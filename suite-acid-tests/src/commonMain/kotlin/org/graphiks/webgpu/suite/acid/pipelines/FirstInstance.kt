package org.graphiks.webgpu.suite.acid.pipelines

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.assertPixel
import org.graphiks.webgpu.suite.acid.createColorTarget
import org.graphiks.webgpu.suite.acid.createRenderPipeline
import org.graphiks.webgpu.suite.acid.renderAndRead
import org.graphiks.webgpu.suite.acid.withValidationScope

private const val INSTANCE_COLOR_SHADER = """
struct VOut {
    @builtin(position) position: vec4f,
    @location(0) @interpolate(flat) isInstanceTwo: f32,
};

@vertex fn vertexMain(
    @builtin(vertex_index) i: u32,
    @builtin(instance_index) instance: u32,
) -> VOut {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    var out: VOut;
    out.position = vec4f(points[i], 0.5, 1.0);
    out.isInstanceTwo = select(0.0, 1.0, instance == 2u);
    return out;
}

@fragment fn fragmentMain(in: VOut) -> @location(0) vec4f {
    return select(vec4f(1,0,0,1), vec4f(0,1,0,1), in.isInstanceTwo > 0.5);
}
"""

/**
 * A direct `draw(vertexCount = 3, instanceCount = 1, firstInstance = 2)` runs with
 * `instance_index = 2`, so the fragment is green. The control with `firstInstance = 0` runs
 * `instance_index = 0` and is red; a non-zero `firstInstance` on a direct draw does not need the
 * `IndirectFirstInstance` feature.
 */
@AcidTest(
    id = AcidCaseId.RenderFirstInstance,
    family = AcidFamily.PipelinesRenderState,
    contract = [
        ApiSymbols.GPURenderCommandsMixin_draw,
        ApiSymbols.GPUDevice_createRenderPipeline,
        ApiSymbols.GPUCommandEncoder_beginRenderPass,
    ],
)
suspend fun firstInstance(device: GPUDevice) = withValidationScope(device) {
    createRenderPipeline(device, INSTANCE_COLOR_SHADER).use { pipeline ->
        val overridden = createColorTarget(device, 16, 16).use { target ->
            renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 1.0, 1.0)) { pass ->
                pass.setPipeline(pipeline)
                pass.draw(3u, instanceCount = 1u, firstInstance = 2u)
            }
        }
        val control = createColorTarget(device, 16, 16).use { target ->
            renderAndRead(device, target, 16, 16, Color(0.0, 0.0, 1.0, 1.0)) { pass ->
                pass.setPipeline(pipeline)
                pass.draw(3u, instanceCount = 1u, firstInstance = 0u)
            }
        }
        assertPixel(overridden, 16, 8, 8, 0, 255, 0, 255)
        assertPixel(control, 16, 8, 8, 255, 0, 0, 255)
    }
}
