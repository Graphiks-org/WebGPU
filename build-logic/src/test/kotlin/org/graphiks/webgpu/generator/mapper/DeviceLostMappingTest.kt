package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.parser.WebIdlParser
import org.graphiks.webgpu.generator.domain.MapperContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeviceLostMappingTest {

    @Test
    fun `device model exposes exactly one suspend awaitLost and no lost property`() {
        val context = contextForRealIdl()

        context.loadTypeDef()
        context.loadInterfaces()
        context.loadDictionaries()
        context.loadEnums()
        context.loadDescriptors()
        context.adaptToGuidelines()

        val device = context.interfaces.single { it.name == "GPUDevice" }

        assertTrue(
            device.attributes.none { it.name == "lost" },
            "The JavaScript lost promise must not leak into the common API",
        )

        val awaitLost = device.methods.filter { it.name == "awaitLost" }
        assertEquals(1, awaitLost.size, "GPUDevice must expose exactly one awaitLost")
        assertTrue(awaitLost[0].isSuspend, "awaitLost must be suspend")
        assertEquals("Result<GPUDeviceLostInfo>", awaitLost[0].returnType)
        assertEquals(emptyList(), awaitLost[0].parameters)
        assertNotNull(awaitLost[0].kDoc, "awaitLost must document loss, repetition and cancellation")
        val doc = awaitLost[0].kDoc!!.description
        assertTrue(doc.contains("lost", ignoreCase = true))
        assertTrue(doc.contains("cancel", ignoreCase = true))
    }

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
