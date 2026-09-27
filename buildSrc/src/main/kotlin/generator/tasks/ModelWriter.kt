package generator.tasks

import generator.domain.MapperContext
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

object ModelWriter {
    private val descriptorCommonSourcePath = Paths.get("webgpu-descriptors").resolve("src").resolve("commonMain").resolve("kotlin")
    private val commonSourcePath = Paths.get("webgpu-api").resolve("src").resolve("commonMain").resolve("kotlin")
    private val commonWebSourcePath = Paths.get("webgpu-api").resolve("src").resolve("webMain").resolve("kotlin")
    private val commonNativeSourcePath = Paths.get("webgpu-api").resolve("src").resolve("commonNativeMain").resolve("kotlin")
    private val webSourcePath = Paths.get("webgpu-web").resolve("src").resolve("commonMain").resolve("kotlin")

    fun write(context: MapperContext) {

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

        descriptorCommonSourcePath.createSourceFile("descriptor.kt") {
            appendText(context.descriptors.joinToString("\n"))
        }

        webSourcePath.createWebSourceFile("types.kt") {
            appendText("import kotlin.js.ExperimentalWasmJsInterop\n")
            appendText("import kotlin.js.JsAny\n")
            appendText("import kotlin.js.JsNumber\n")
            appendText("import kotlin.js.JsArray\n")
            appendText("import js.promise.Promise\n")
            appendText("import js.collections.JsSet\n")
            appendText("import js.collections.JsMap\n")
            appendText("\n")
            appendText(context.webTypeAlias.joinToString("\n"))
            appendText("\n")
            appendText(context.webInterfaces.joinToString("\n"))
        }
    }

    private fun Path.createSourceFile(fileName: String, block: File.() -> Unit) {
        createDirectories()
        resolve(fileName).toFile().apply {
            delete()
            createNewFile()

            appendText("@file:Suppress(\"unused\")\n")
            appendText("// This file has been generated DO NO EDIT\n")
            appendText("package org.graphiks.webgpu\n\n")
            block()
        }
    }

    private fun Path.createWebSourceFile(fileName: String, block: File.() -> Unit) {
        createDirectories()
        resolve(fileName).toFile().apply {
            delete()
            createNewFile()

            appendText("@file:Suppress(\"unused\")\n")
            appendText("@file:OptIn(ExperimentalWasmJsInterop::class)")
            appendText("// This file has been generated DO NO EDIT\n")
            appendText("package org.graphiks.webgpu\n\n")
            block()
        }
    }
}
