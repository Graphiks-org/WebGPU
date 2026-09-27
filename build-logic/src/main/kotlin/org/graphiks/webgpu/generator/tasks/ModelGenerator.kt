package org.graphiks.webgpu.generator.tasks

import com.charleskorn.kaml.Yaml
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.files.SpecificationResources
import org.graphiks.webgpu.generator.mapper.injectDocumentation
import org.graphiks.webgpu.generator.mapper.loadDescriptors
import org.graphiks.webgpu.generator.mapper.loadDictionaries
import org.graphiks.webgpu.generator.mapper.loadEnums
import org.graphiks.webgpu.generator.mapper.loadInterfaces
import org.graphiks.webgpu.generator.mapper.loadTypeDef
import org.graphiks.webgpu.generator.mapper.loadWebGPUYaml
import org.graphiks.webgpu.generator.mapper.loadWebInterfaces
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.SequenceInputStream
import java.nio.file.Files

class ModelGenerator(
    val specificationResources: SpecificationResources,
) {

    private val idlExtraTyps = """
        interface mixin NavigatorGPU {
        };
    
        interface Navigator {
        };
    
        interface WorkerNavigator {
        };
    """.byteInputStream()

    val context: MapperContext by lazy {
        val ildPath = specificationResources.findFilePath(SpecificationResources.Files.webgpuIdl) ?: error("fail to get cached file")

        val idlModel = de.fabmax.webidl.parser.WebIdlParser.Companion.parseFromInputStream(
            SequenceInputStream(idlExtraTyps, Files.newInputStream(ildPath))
        )
        val yamlModel = loadWebGPUYaml()

        MapperContext(idlModel, yamlModel).apply {
            loadTypeDef()
            loadInterfaces()
            loadDictionaries()
            loadEnums()
            loadDescriptors()
            loadWebInterfaces()

            adaptToGuidelines()
        }
    }

    fun injectDocumentation() {
        val yamlFile = specificationResources.specificationsSourcePath.resolve(SpecificationResources.Files.documentationYaml).toFile()
        val yamlContent = yamlFile.readText()
        val yamlMap = Yaml.default.decodeFromString(MapSerializer(String.serializer(), String.serializer()), yamlContent)
        context.injectDocumentation(yamlMap)
    }

}
