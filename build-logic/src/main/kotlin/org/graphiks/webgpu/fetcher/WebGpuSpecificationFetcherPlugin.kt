package org.graphiks.webgpu.fetcher

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class WebGpuSpecificationFetcherExtension @Inject constructor(objects: ObjectFactory) {
    val resourceDirectory: DirectoryProperty = objects.directoryProperty()
    val htmlUrl: Property<String> = objects.property(String::class.java)
    val idlUrl: Property<String> = objects.property(String::class.java)
}

abstract class CheckCacheTask : DefaultTask() {
    @get:OutputDirectory
    abstract val resourceDirectory: DirectoryProperty

    @get:Input
    abstract val htmlUrl: Property<String>

    @get:Input
    abstract val idlUrl: Property<String>

    @TaskAction
    fun refreshSpecifications() {
        val sources = listOf(
            SpecificationSource("webgpu.html", java.net.URI(htmlUrl.get())),
            SpecificationSource("webgpu.idl", java.net.URI(idlUrl.get())),
        )
        val result = SpecificationRefreshService().refresh(resourceDirectory.get().asFile.toPath(), sources)

        for (source in sources) {
            val state = if (source.fileName in result.changedFiles) "changed" else "unchanged"
            logger.lifecycle("${source.fileName}: $state (${source.url})")
        }
    }
}

class WebGpuSpecificationFetcherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "webGpuSpecificationFetcher",
            WebGpuSpecificationFetcherExtension::class.java,
            project.objects,
        )
        extension.resourceDirectory.convention(
            project.layout.projectDirectory.dir("webgpu-specifications/src/jvmMain/resources"),
        )
        extension.htmlUrl.convention(DEFAULT_HTML_URL)
        extension.idlUrl.convention(DEFAULT_IDL_URL)

        project.tasks.register("check-cache", CheckCacheTask::class.java).configure {
            group = "verification"
            description = "Fetch and cache the WebGPU specification HTML and IDL."
            resourceDirectory.set(extension.resourceDirectory)
            htmlUrl.set(extension.htmlUrl)
            idlUrl.set(extension.idlUrl)
            outputs.upToDateWhen { false }
            outputs.cacheIf { false }
        }
    }

    private companion object {
        const val DEFAULT_HTML_URL = "https://www.w3.org/TR/webgpu/"
        const val DEFAULT_IDL_URL = "https://gpuweb.github.io/gpuweb/webgpu.idl"
    }
}
