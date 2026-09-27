package org.graphiks.webgpu.generator

import org.gradle.api.Plugin
import org.gradle.api.Project

class GeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val tasks = project.tasks
        tasks.register("generate-binding", GenerateBindingTask::class.java)
        tasks.register("generate-doc-from-llm", LLMDocGeneratorTask::class.java)
        tasks.register("refresh-documentation-from-spec", RefreshDocumentationFromSpecTask::class.java)
        tasks.register("check-missing-doc", CheckMissingDocumentationTask::class.java)
        tasks.register("tranform-json-doc-to-yaml", TransformJsonDocToYamlTask::class.java)
    }
}
