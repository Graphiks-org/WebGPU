package org.graphiks.webgpu.generator.mapper

import org.graphiks.webgpu.generator.testContext
import kotlin.test.Test
import kotlin.test.assertTrue

class WebInteropMappingTest {

    @Test
    fun `nullable web values keep their nullability and records use WebGpuRecord`() {
        val idl = """
            interface GPUBindGroup {};
            interface GPUError {};
            interface GPUFixture {
                undefined setBindGroup(unsigned long index, GPUBindGroup? group);
                Promise<GPUError?> popErrorScope();
            };
            dictionary GPUFixtureDescriptor {
                record<USVString, double> constants;
            };
        """.trimIndent()
        val context = testContext(idl).apply { loadWebInterfaces() }

        val fixture = context.webInterfaces.single { it.name == "WGPUFixture" }
        val group = fixture.methods.single { it.name == "setBindGroup" }.parameters.last()
        assertTrue(group.type.substringBefore("/*").trim().endsWith("?"))
        assertTrue(fixture.methods.single { it.name == "popErrorScope" }.returnType.contains("JsAny?"))
        val descriptor = context.webInterfaces.single { it.name == "WGPUFixtureDescriptor" }
        assertTrue(descriptor.attributes.single { it.name == "constants" }.type.startsWith("WebGpuRecord"))
    }
}
