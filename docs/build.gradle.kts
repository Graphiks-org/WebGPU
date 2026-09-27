import org.gradle.api.tasks.Sync

plugins {
    id("dev.opensavvy.dokka-mkdocs")
}

val apiModules = listOf(
    ":webgpu-api",
    ":webgpu-descriptors",
    ":webgpu-web",
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
