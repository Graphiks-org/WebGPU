package org.graphiks.webgpu.generator.files

import java.nio.file.Path

class SpecificationResources(projectDirectory: Path) {

    object Files {
        const val webgpuHtml = "webgpu.html"
        const val webgpuIdl = "webgpu.idl"
        const val documentationYaml = "documentation.yaml"
        const val documentationJson = "documentation.json"
    }

    val specificationsSourcePath: Path = projectDirectory
        .resolve("webgpu-specifications")
        .resolve("src")
        .resolve("jvmMain")
        .resolve("resources")

    fun findFilePath(fileName: String): Path? = specificationsSourcePath
        .resolve(fileName)
        .takeIf { java.nio.file.Files.exists(it) }
}
