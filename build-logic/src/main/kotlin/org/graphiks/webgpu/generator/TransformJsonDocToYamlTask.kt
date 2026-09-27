package org.graphiks.webgpu.generator

import com.charleskorn.kaml.Yaml
import org.graphiks.webgpu.generator.files.SpecificationResources
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

open class TransformJsonDocToYamlTask : DefaultTask() {

    init {
        group = "generator"
    }

    @TaskAction
    fun launch() {
        val specificationResources = SpecificationResources(project.projectDir.toPath())

        val jsonFile = specificationResources.specificationsSourcePath.resolve(SpecificationResources.Files.documentationJson).toFile()
        val yamlFile = specificationResources.specificationsSourcePath.resolve(SpecificationResources.Files.documentationYaml).toFile()

        // Read the JSON file
        val jsonContent = jsonFile.readText()

        // Parse the JSON content
        val jsonObject = Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), jsonContent)

        yamlFile.delete()
        yamlFile.createNewFile()
        jsonObject.forEach { (key, value) ->
            yamlFile.appendText("\"$key\": |\n")
            value.split("\n").forEach { line ->
                yamlFile.appendText(if (line.isEmpty()) "\n" else "  $line\n")
            }
        }

        logger.lifecycle("Transformed documentation.json to documentation.yaml")
    }
}
