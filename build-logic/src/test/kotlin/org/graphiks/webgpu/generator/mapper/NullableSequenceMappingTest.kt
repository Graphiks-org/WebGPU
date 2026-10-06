package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.parser.WebIdlParser
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.domain.YamlModel
import kotlin.test.Test
import kotlin.test.assertEquals

class NullableSequenceMappingTest {

    @Test
    fun `nullable sequence elements keep their nullability in interfaces and descriptors`() {
        val context = contextFor(
            """
                interface Item {};
                dictionary NullableLists {
                    required sequence<Item?> slots;
                    sequence<Item> dense = [];
                };
            """.trimIndent(),
        )

        context.loadInterfaces()
        context.loadDictionaries()
        context.loadDescriptors()

        val dictionary = context.interfaces.single { it.name == "NullableLists" }
        assertEquals("List<Item?>", dictionary.attributes.single { it.name == "slots" }.type)
        assertEquals("List<Item>", dictionary.attributes.single { it.name == "dense" }.type)

        val descriptor = context.descriptors.single { it.name == "NullableLists" }
        assertEquals("List<Item?>", descriptor.parameter.single { it.name == "slots" }.type)
        assertEquals("List<Item>", descriptor.parameter.single { it.name == "dense" }.type)
        assertEquals("emptyList()", descriptor.parameter.single { it.name == "dense" }.defaultValue)
    }

    @Test
    fun `versioned idl keeps the five nullable sequence slots nullable`() {
        val context = contextForRealIdl()

        context.loadTypeDef()
        context.loadInterfaces()
        context.loadDictionaries()
        context.loadEnums()
        context.loadDescriptors()

        val expected = mapOf(
            "GPUPipelineLayoutDescriptor" to "bindGroupLayouts",
            "GPUFragmentState" to "targets",
            "GPUVertexState" to "buffers",
            "GPURenderPassDescriptor" to "colorAttachments",
            "GPURenderPassLayout" to "colorFormats",
        )
        val elementTypes = mapOf(
            "GPUPipelineLayoutDescriptor" to "GPUBindGroupLayout",
            "GPUFragmentState" to "GPUColorTargetState",
            "GPUVertexState" to "GPUVertexBufferLayout",
            "GPURenderPassDescriptor" to "GPURenderPassColorAttachment",
            "GPURenderPassLayout" to "GPUTextureFormat",
        )

        expected.forEach { (dictionaryName, memberName) ->
            val elementType = elementTypes.getValue(dictionaryName)
            val dictionary = context.interfaces.single { it.name == dictionaryName }
            assertEquals(
                "List<$elementType?>",
                dictionary.attributes.single { it.name == memberName }.type,
                "interface $dictionaryName.$memberName",
            )
            val descriptor = context.descriptors.single { it.name == dictionaryName }
            assertEquals(
                "List<$elementType?>",
                descriptor.parameter.single { it.name == memberName }.type,
                "descriptor $dictionaryName.$memberName",
            )
        }
    }

    private fun contextFor(idl: String) = MapperContext(
        WebIdlParser.Companion.parseFromInputStream(idl.byteInputStream()),
        YamlModel(
            copyright = "",
            name = "",
            enum_prefix = "",
            constants = emptyList(),
            typedefs = emptyList(),
            bitflags = emptyList(),
            structs = emptyList(),
            functions = emptyList(),
            objects = emptyList(),
            enums = emptyList(),
        ),
    )

    private fun contextForRealIdl(): MapperContext {
        // Mirrors ModelGenerator: the parser needs the extra browser mixin types prepended.
        val idlExtraTypes = """
            interface mixin NavigatorGPU {
            };

            interface Navigator {
            };

            interface WorkerNavigator {
            };
        """.byteInputStream()
        val idlPath = java.nio.file.Paths.get("../webgpu-specifications/src/jvmMain/resources/webgpu.idl")
        val idlModel = WebIdlParser.Companion.parseFromInputStream(
            java.io.SequenceInputStream(idlExtraTypes, java.nio.file.Files.newInputStream(idlPath)),
        )
        return MapperContext(idlModel, loadWebGPUYaml())
    }
}
