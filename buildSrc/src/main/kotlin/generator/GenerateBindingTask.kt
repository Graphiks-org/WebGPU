package generator

import generator.files.SpecificationResources
import generator.tasks.ModelGenerator
import generator.tasks.ModelWriter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateBindingTask : DefaultTask() {

    init {
        group = "generator"
    }

    @TaskAction
    fun launch() {
        val specificationResources = SpecificationResources(project.projectDir.toPath())
        val context = ModelGenerator(specificationResources)
            .also { it.injectDocumentation() }
            .context
        ModelWriter.write(context)
    }
}
