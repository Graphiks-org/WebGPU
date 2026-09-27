package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.parser.WebIdlParser
import org.graphiks.webgpu.generator.domain.Enumeration
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.domain.YamlModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryUnionMappingTest {
    @Test
    fun `render attachment unions are shared and preserve required and optional members`() {
        val context = contextFor(
            """
                interface GPUTexture {};
                interface GPUTextureView {};

                dictionary GPURenderPassColorAttachment {
                    required (GPUTexture or GPUTextureView) view;
                    (GPUTexture or GPUTextureView) resolveTarget;
                };

                dictionary GPURenderPassDepthStencilAttachment {
                    required (GPUTextureView or GPUTexture) view;
                };
            """.trimIndent(),
        )

        context.loadInterfaces()
        context.loadDictionaries()
        context.loadDescriptors()

        val union = context.interfaces.single { it.name == "GPUTextureOrGPUTextureView" }
        assertTrue(union.sealed)
        assertEquals(setOf(union.name), context.interfaces.single { it.name == "GPUTexture" }.extends)
        assertEquals(setOf(union.name), context.interfaces.single { it.name == "GPUTextureView" }.extends)

        val color = context.interfaces.single { it.name == "GPURenderPassColorAttachment" }
        assertEquals(union.name, color.attributes.single { it.name == "view" }.type)
        assertEquals("${union.name}?", color.attributes.single { it.name == "resolveTarget" }.type)

        val colorDescriptor = context.descriptors.single { it.name == "GPURenderPassColorAttachment" }
        val requiredView = colorDescriptor.parameter.single { it.name == "view" }
        assertEquals(union.name, requiredView.type)
        assertNull(requiredView.defaultValue)

        val optionalResolveTarget = colorDescriptor.parameter.single { it.name == "resolveTarget" }
        assertEquals("${union.name}?", optionalResolveTarget.type)
        assertEquals("null", optionalResolveTarget.defaultValue)

        val depthDescriptor = context.descriptors.single { it.name == "GPURenderPassDepthStencilAttachment" }
        assertEquals(union.name, depthDescriptor.parameter.single { it.name == "view" }.type)
    }

    @Test
    fun `layout union keeps the existing null representation for automatic layout`() {
        val context = contextFor(
            """
                interface GPUPipelineLayout {};
                enum GPUAutoLayoutMode { "auto" };
                dictionary GPUPipelineDescriptorBase {
                    required (GPUPipelineLayout or GPUAutoLayoutMode) layout;
                };
                dictionary GPURenderPipelineDescriptor {
                    required (GPUAutoLayoutMode or GPUPipelineLayout) layout;
                };
            """.trimIndent(),
        )

        context.loadInterfaces()
        context.loadDictionaries()
        context.loadDescriptors()

        assertTrue(context.interfaces.none { it.name == "GPUPipelineLayoutOrGPUAutoLayoutMode" })
        assertEquals(
            "GPUPipelineLayout?",
            context.interfaces.single { it.name == "GPUPipelineDescriptorBase" }
                .attributes.single { it.name == "layout" }.type,
        )
        val layout = context.descriptors.single { it.name == "GPUPipelineDescriptorBase" }
            .parameter.single { it.name == "layout" }
        assertEquals("GPUPipelineLayout?", layout.type)
        assertEquals("null", layout.defaultValue)

        assertEquals(
            "GPUPipelineLayout?",
            context.interfaces.single { it.name == "GPURenderPipelineDescriptor" }
                .attributes.single { it.name == "layout" }.type,
        )
        val reversedLayout = context.descriptors.single { it.name == "GPURenderPipelineDescriptor" }
            .parameter.single { it.name == "layout" }
        assertEquals("GPUPipelineLayout?", reversedLayout.type)
        assertEquals("null", reversedLayout.defaultValue)
    }

    @Test
    fun `unsupported common dictionary unions fail with member context`() {
        val context = contextFor(
            """
                interface GPUTexture {};
                dictionary UnsupportedDescriptor {
                    required (DOMString or GPUTexture) resource;
                };
            """.trimIndent(),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            context.loadDictionaries()
        }

        assertNotNull(error.message)
        assertTrue(error.message!!.contains("UnsupportedDescriptor.resource"))
        assertTrue(error.message!!.contains("DOMString"))
        assertTrue(error.message!!.contains("GPUTexture"))
    }

    @Test
    fun `unknown GPU union alternatives fail instead of generating placeholder interfaces`() {
        val context = contextFor(
            """
                interface GPUTexture {};
                dictionary UnsupportedDescriptor {
                    required (GPUTexture or GPUUnknownResource) resource;
                };
            """.trimIndent(),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            context.loadDictionaries()
        }

        assertNotNull(error.message)
        assertTrue(error.message!!.contains("UnsupportedDescriptor.resource"))
        assertTrue(error.message!!.contains("GPUUnknownResource"))
    }

    @Test
    fun `dictionary interface mapping does not resolve enum defaults before enums are loaded`() {
        val context = contextFor(
            """
                enum GPUTextureDimension { "1d", "2d", "3d" };
                dictionary GPUTextureDescriptor {
                    GPUTextureDimension dimension = "2d";
                };
            """.trimIndent(),
        )

        context.loadDictionaries()

        assertEquals(
            "GPUTextureDimension",
            context.interfaces.single { it.name == "GPUTextureDescriptor" }
                .attributes.single { it.name == "dimension" }.type,
        )

        context.commonEnumerations += Enumeration(
            "GPUTextureDimension",
            listOf(Enumeration.Value("OneD"), Enumeration.Value("TwoD"), Enumeration.Value("ThreeD")),
        )
        context.loadDescriptors()

        assertEquals(
            "GPUTextureDimension.TwoD",
            context.descriptors.single { it.name == "GPUTextureDescriptor" }
                .parameter.single { it.name == "dimension" }.defaultValue,
        )
    }

    @Test
    fun `texture view swizzle maps the WebIDL string to the typed public value`() {
        val context = contextFor(
            """
                dictionary GPUTextureViewDescriptor {
                    DOMString swizzle = "rgba";
                };
            """.trimIndent(),
        )

        context.loadDictionaries()
        context.loadDescriptors()

        assertEquals(
            "GPUTextureSwizzle",
            context.interfaces.single { it.name == "GPUTextureViewDescriptor" }
                .attributes.single { it.name == "swizzle" }.type,
        )
        val swizzle = context.descriptors.single { it.name == "GPUTextureViewDescriptor" }
            .parameter.single { it.name == "swizzle" }
        assertEquals("GPUTextureSwizzle", swizzle.type)
        assertEquals("GPUTextureSwizzle()", swizzle.defaultValue)
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
}
