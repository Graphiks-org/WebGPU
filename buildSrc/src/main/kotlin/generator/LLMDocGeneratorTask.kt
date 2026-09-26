package generator

import generator.files.SpecificationResources
import generator.lm.DocumentGeneratorManager
import generator.tasks.ModelGenerator
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class LLMDocGeneratorTask : DefaultTask() {

    init {
        group = "generator"
    }

    @TaskAction
    fun launch() = runBlocking {
        val specificationResources = SpecificationResources(project.projectDir.toPath())
        val context = ModelGenerator(specificationResources).context
        val htmlDocumentation = specificationResources.findFilePath(SpecificationResources.Files.webgpuHtml)
            ?: error("Cannot find the html documentation")
         DocumentGeneratorManager(context, specificationResources, htmlDocumentation, logger)
            .also { it.inferHtmlDocumentation() }
    }
}
