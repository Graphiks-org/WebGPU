import org.gradle.api.tasks.Sync

plugins {
    id("dev.opensavvy.dokka-mkdocs")
}

val apiModules = listOf(
    ":webgpu-api",
    ":webgpu-descriptors",
    ":webgpu-web-bindings",
    ":webgpu-browser",
).map { project(it) }

val copyWebGpuDokkaIntoMkDocs = tasks.register<Sync>("copyWebGpuDokkaIntoMkDocs") {
    dependsOn(apiModules.map { it.tasks.named("dokkaGenerateModuleMkdocs") })
    dependsOn(tasks.named("dokkaCopyIntoMkDocs"))

    into(layout.projectDirectory.dir("docs/api"))
    apiModules.forEach { module ->
        from(module.layout.buildDirectory.dir("dokka-module/mkdocs/module")) {
            into(module.name)
        }
    }
}

val copyTypeMappingIntoMkDocs = tasks.register<Sync>("copyTypeMappingIntoMkDocs") {
    from(rootProject.layout.projectDirectory.file("TYPE_MAPPING.md"))
    into(layout.projectDirectory.dir("docs/generated"))
    rename { "type-mapping.md" }
}

tasks.named("generateMkDocsNavigation") {
    dependsOn(copyWebGpuDokkaIntoMkDocs, copyTypeMappingIntoMkDocs)
}

val compactMkDocsNavigation = tasks.register("compactMkDocsNavigation") {
    description = "Keep only the module-level API links in the tracked MkDocs navigation."
    doLast {
        val config = layout.projectDirectory.file("mkdocs.yml").asFile
        val source = config.readText()
        val startMarker = "# !!! EMBEDDED DOKKA START, DO NOT COMMIT !!! #"
        val endMarker = "# !!! EMBEDDED DOKKA END, DO NOT COMMIT !!! #"
        val start = source.indexOf(startMarker)
        val end = source.indexOf(endMarker)
        require(start >= 0 && end > start) { "Dokka navigation markers are missing from mkdocs.yml" }
        val compact = source.substring(0, start + startMarker.length) + "\n" + source.substring(end)
        if (compact != source) config.writeText(compact)
    }
}

tasks.named("embedDokkaIntoMkDocs") {
    finalizedBy(compactMkDocsNavigation)
}
