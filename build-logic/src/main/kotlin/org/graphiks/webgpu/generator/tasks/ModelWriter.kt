package org.graphiks.webgpu.generator.tasks

import org.graphiks.webgpu.generator.domain.MapperContext
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

object ModelWriter {
    fun write(context: MapperContext, root: Path = Paths.get(".")) {
        val descriptorCommonSourcePath = root.resolve("webgpu-descriptors").resolve("src").resolve("commonMain").resolve("kotlin")
        val commonSourcePath = root.resolve("webgpu-api").resolve("src").resolve("commonMain").resolve("kotlin")
        val commonWebSourcePath = root.resolve("webgpu-api").resolve("src").resolve("webMain").resolve("kotlin")
        val commonNativeSourcePath = root.resolve("webgpu-api").resolve("src").resolve("commonNativeMain").resolve("kotlin")
        val webSourcePath = root.resolve("webgpu-web-bindings").resolve("src").resolve("commonMain").resolve("kotlin")

        commonSourcePath.createSourceFile("bitflags.kt") {
            appendText(context.bitflagEnumerations.joinToString("\n"))
        }

        commonSourcePath.createSourceFile("enumerations.kt") {
            appendText(context.commonEnumerations.joinToString("\n"))
        }

        commonWebSourcePath.createSourceFile("enumerations.kt") {
            appendText(context.commonWebEnumerations.joinToString("\n"))
        }

        commonNativeSourcePath.createSourceFile("enumerations.kt") {
            appendText(context.commonNativeEnumerations.joinToString("\n"))
        }

        commonSourcePath.createSourceFile("typealiases.kt") {
            appendText(context.typeAliases.joinToString("\n"))
        }

        commonSourcePath.createSourceFile("interfaces.kt") {
            appendText(context.interfaces.joinToString("\n"))
        }

        descriptorCommonSourcePath.createSourceFile(
            "descriptor.kt",
            packageName = "org.graphiks.webgpu.descriptors",
            imports = listOf("org.graphiks.webgpu.*"),
        ) {
            appendText(context.descriptors.joinToString("\n"))
        }

        webSourcePath.createWebSourceFile("types.kt") {
            appendText("import kotlin.js.ExperimentalWasmJsInterop\n")
            appendText("import kotlin.js.JsAny\n")
            appendText("import kotlin.js.JsNumber\n")
            appendText("import kotlin.js.JsArray\n")
            appendText("import js.promise.Promise\n")
            appendText("import js.collections.JsSet\n")
            appendText("\n")
            appendText(context.webTypeAlias.joinToString("\n"))
            appendText("\n")
            appendText(context.webInterfaces.joinToString("\n"))
        }
    }

    private fun Path.createSourceFile(
        fileName: String,
        packageName: String = "org.graphiks.webgpu",
        imports: List<String> = emptyList(),
        block: File.() -> Unit,
    ) {
        createDirectories()
        resolve(fileName).toFile().apply {
            writeText("@file:Suppress(\"unused\")\n// This file has been generated DO NO EDIT\n")
            appendText("package $packageName\n\n")
            if (imports.isNotEmpty()) {
                imports.forEach { appendText("import $it\n") }
                appendText("\n")
            }
            block()
        }
    }

    private fun Path.createWebSourceFile(fileName: String, block: File.() -> Unit) {
        createDirectories()
        resolve(fileName).toFile().apply {
            writeText("@file:Suppress(\"unused\")\n")
            appendText("@file:OptIn(ExperimentalWasmJsInterop::class)\n")
            appendText("// This file has been generated DO NO EDIT\n")
            appendText("package org.graphiks.webgpu.bindings\n\n")
            block()
        }
    }
}
