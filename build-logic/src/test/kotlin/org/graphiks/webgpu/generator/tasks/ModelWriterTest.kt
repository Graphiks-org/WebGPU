package org.graphiks.webgpu.generator.tasks

import org.graphiks.webgpu.generator.mapper.loadDescriptors
import org.graphiks.webgpu.generator.mapper.loadDictionaries
import org.graphiks.webgpu.generator.mapper.loadInterfaces
import org.graphiks.webgpu.generator.mapper.loadWebInterfaces
import org.graphiks.webgpu.generator.testContext
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelWriterTest {

    @Test
    fun `writes descriptor and binding packages under an injected root`() {
        val idl = """
            interface GPUBuffer {};
            dictionary GPUBufferDescriptor { required unsigned long size; };
        """.trimIndent()
        val context = testContext(idl).apply {
            loadInterfaces()
            loadDictionaries()
            loadDescriptors()
            loadWebInterfaces()
        }
        val root = Files.createTempDirectory("webgpu-writer-test")
        try {
            ModelWriter.write(context, root)
            val descriptors = root.resolve("webgpu-descriptors/src/commonMain/kotlin/descriptor.kt").readText()
            val bindings = root.resolve("webgpu-web-bindings/src/commonMain/kotlin/types.kt").readText()
            val api = root.resolve("webgpu-api/src/commonMain/kotlin/interfaces.kt").readText()
            assertTrue(descriptors.contains("package org.graphiks.webgpu.descriptors"))
            assertTrue(descriptors.contains("import org.graphiks.webgpu.*"))
            assertTrue(bindings.contains("package org.graphiks.webgpu.bindings"))
            assertTrue(api.contains("package org.graphiks.webgpu\n"))
            assertFalse(root.resolve("webgpu-web").toFile().exists())
            ModelWriter.write(context, root)
            assertEquals(descriptors, root.resolve("webgpu-descriptors/src/commonMain/kotlin/descriptor.kt").readText())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
