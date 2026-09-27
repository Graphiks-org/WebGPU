@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUPipelineLayout
import org.graphiks.webgpu.bindings.WGPUShaderModule
import org.graphiks.webgpu.bindings.createJsObject
import org.graphiks.webgpu.browser.mapper.map
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleCompilationHint
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.js.js
import kotlin.js.toJsString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun emptyShader(): WGPUShaderModule = js("({})")

class PipelineDescriptorTest {

    @Test
    fun computeAutoLayoutAndMutableConstantsAreForwarded() {
        val constants = mutableMapOf("scale" to 2.0)
        val descriptor = ComputePipelineDescriptor(
            compute = ProgrammableStage(ShaderModule(emptyShader()), constants = constants),
        )
        val first = map(descriptor)
        assertEquals("auto", propertyString(first, "layout"))
        assertEquals(2.0, propertyNumber(first.compute.constants, "scale"))
        constants["scale"] = 7.0
        val second = map(descriptor)
        assertEquals(7.0, propertyNumber(second.compute.constants, "scale"))
        assertEquals(2.0, propertyNumber(first.compute.constants, "scale"))
    }

    @Test
    fun renderStagesForwardConstantsAndLayouts() {
        val shader = ShaderModule(emptyShader())
        val layout = PipelineLayout(createJsObject<WGPUPipelineLayout>())
        val descriptor = RenderPipelineDescriptor(
            vertex = VertexState(shader, constants = mapOf("vertexScale" to 3.0)),
            fragment = FragmentState(
                targets = emptyList(),
                module = shader,
                constants = mapOf("fragmentScale" to 4.0),
            ),
            layout = layout,
        )
        val out = map(descriptor)
        assertTrue(sameJs(layout.handler, out.layout))
        assertEquals(3.0, propertyNumber(out.vertex.constants, "vertexScale"))
        assertEquals(4.0, propertyNumber(out.fragment.constants, "fragmentScale"))
        assertFalse(hasOwn(out.vertex, "entryPoint"))
    }

    @Test
    fun compilationHintsForwardEntryPointAndLayout() {
        val explicitLayout = PipelineLayout(createJsObject<WGPUPipelineLayout>())
        val descriptor = ShaderModuleDescriptor(
            code = "",
            compilationHints = listOf(
                ShaderModuleCompilationHint("main"),
                ShaderModuleCompilationHint("secondary", explicitLayout),
            ),
        )
        val out = map(descriptor)
        assertEquals("main", propertyString(out.compilationHints[0], "entryPoint"))
        assertTrue(sameJs("auto".toJsString(), propertyValue(out.compilationHints[0], "layout")))
        assertEquals("secondary", propertyString(out.compilationHints[1], "entryPoint"))
        assertTrue(sameJs(explicitLayout.handler, propertyValue(out.compilationHints[1], "layout")))
    }
}
