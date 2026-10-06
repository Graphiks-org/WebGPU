package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.parser.WebIdlParser
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.domain.YamlModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequiredLimitsMappingTest {

    @Test
    fun `required limits mirror supported limits with nullable types and null defaults`() {
        val context = contextFor(
            """
                interface GPUSupportedLimits {
                    readonly attribute unsigned long maxBufferSize;
                    readonly attribute unsigned long long maxBigThing;
                };
                dictionary GPUDeviceDescriptor {
                    record<DOMString, unsigned long> requiredLimits = {};
                };
            """.trimIndent(),
        )

        context.loadInterfaces()
        context.loadDictionaries()
        context.loadDescriptors()
        context.adaptRequiredLimits()

        val requiredInterface = context.interfaces.single { it.name == "GPURequiredLimits" }
        assertEquals("UInt?", requiredInterface.attributes.single { it.name == "maxBufferSize" }.type)
        assertEquals("ULong?", requiredInterface.attributes.single { it.name == "maxBigThing" }.type)

        val requiredDescriptor = context.descriptors.single { it.name == "GPURequiredLimits" }
        val bufferSize = requiredDescriptor.parameter.single { it.name == "maxBufferSize" }
        assertEquals("UInt?", bufferSize.type)
        assertEquals("null", bufferSize.defaultValue)
        val bigThing = requiredDescriptor.parameter.single { it.name == "maxBigThing" }
        assertEquals("ULong?", bigThing.type)
        assertEquals("null", bigThing.defaultValue)

        val deviceDescriptor = context.descriptors.single { it.name == "GPUDeviceDescriptor" }
        val requiredLimits = deviceDescriptor.parameter.single { it.name == "requiredLimits" }
        assertEquals("GPURequiredLimits?", requiredLimits.type)
        assertEquals("null", requiredLimits.defaultValue)

        val deviceInterface = context.interfaces.single { it.name == "GPUDeviceDescriptor" }
        assertEquals(
            "GPURequiredLimits?",
            deviceInterface.attributes.single { it.name == "requiredLimits" }.type,
        )
    }

    @Test
    fun `versioned idl generates a required limits contract covering every supported limit`() {
        val context = contextForRealIdl()

        context.loadTypeDef()
        context.loadInterfaces()
        context.loadDictionaries()
        context.loadEnums()
        context.loadDescriptors()
        context.adaptRequiredLimits()

        val supported = context.interfaces.single { it.name == "GPUSupportedLimits" }
        assertTrue(supported.attributes.isNotEmpty())

        val requiredInterface = context.interfaces.single { it.name == "GPURequiredLimits" }
        supported.attributes.forEach { attribute ->
            val mirrored = requiredInterface.attributes.singleOrNull { it.name == attribute.name }
            assertNotNull(mirrored, "GPURequiredLimits is missing ${attribute.name}")
            assertEquals(
                "${attribute.type}?",
                mirrored.type,
                "GPURequiredLimits.${attribute.name} must be the nullable counterpart of GPUSupportedLimits.${attribute.name}",
            )
        }
        assertEquals(
            supported.attributes.size,
            requiredInterface.attributes.size,
            "GPURequiredLimits must not add or drop limits",
        )

        val requiredDescriptor = context.descriptors.single { it.name == "GPURequiredLimits" }
        supported.attributes.forEach { attribute ->
            val parameter = requiredDescriptor.parameter.singleOrNull { it.name == attribute.name }
            assertNotNull(parameter, "RequiredLimits is missing ${attribute.name}")
            assertEquals("${attribute.type}?", parameter.type)
            assertEquals("null", parameter.defaultValue)
        }
        assertEquals(supported.attributes.size, requiredDescriptor.parameter.size)

        val deviceDescriptor = context.descriptors.single { it.name == "GPUDeviceDescriptor" }
        assertEquals(
            "GPURequiredLimits?",
            deviceDescriptor.parameter.single { it.name == "requiredLimits" }.type,
        )
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
