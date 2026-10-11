package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.parser.WebIdlParser
import org.graphiks.webgpu.generator.domain.MapperContext
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureLevelMappingTest {
    @Test
    fun `adapter feature level is typed and defaults to Core without changing raw WebIDL`() {
        val context = MapperContext(
            WebIdlParser.Companion.parseFromInputStream(
                """
                    dictionary GPURequestAdapterOptions {
                        DOMString featureLevel = "core";
                    };
                """.trimIndent().byteInputStream(),
            ),
            loadWebGPUYaml(),
        )
        context.loadDictionaries()
        context.loadDescriptors()
        context.loadWebInterfaces()

        assertEquals("GPUFeatureLevel", context.interfaces.single().attributes.single().type)
        val level = context.descriptors.single().parameter.single()
        assertEquals("GPUFeatureLevel", level.type)
        assertEquals("GPUFeatureLevel.Core", level.defaultValue)
        assertEquals("String /* DOMString */", context.webInterfaces.single().attributes.single().type)
    }

    @Test
    fun `feature levels map to WebGPU strings and native specification values`() {
        val context = MapperContext(
            WebIdlParser.Companion.parseFromInputStream("dictionary GPURequestAdapterOptions {};".byteInputStream()),
            loadWebGPUYaml(),
        )
        context.loadEnums()
        assertEquals(
            listOf("Compatibility", "Core"),
            context.commonEnumerations.single { it.name == "GPUFeatureLevel" }.values.map { it.name },
        )
        assertEquals(
            listOf("Compatibility(\"compatibility\")", "Core(\"core\")"),
            context.commonWebEnumerations.single { it.name == "GPUFeatureLevel" }.values.map { it.name },
        )
        assertEquals(
            listOf("Compatibility(1u)", "Core(2u)"),
            context.commonNativeEnumerations.single { it.name == "GPUFeatureLevel" }.values.map { it.name },
        )
    }
}
