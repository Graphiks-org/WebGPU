plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
    id("dev.opensavvy.dokka-mkdocs")
}

val libraryDescription = when (project.name) {
    "webgpu-api" -> "Shared WebGPU API for Kotlin Multiplatform"
    "webgpu-descriptors" -> "Descriptor implementations for the Graphiks WebGPU API"
    "webgpu-web-bindings" -> "WebGPU JavaScript bindings and Kotlin JS/Wasm interop"
    "webgpu-browser" -> "Browser implementation of the Graphiks WebGPU API for Kotlin JS and Wasm"
    else -> "Graphiks WebGPU for Kotlin Multiplatform"
}

val isPublishing = project.findProperty("signingInMemoryKey")?.toString()?.isNotBlank() == true ||
    project.findProperty("signing.keyId")?.toString()?.isNotBlank() == true

mavenPublishing {
    if (isPublishing) {
        publishToMavenCentral()
        signAllPublications()
    }

    coordinates(group.toString(), project.name, version.toString())

    pom {
        name.set(project.name)
        description.set(libraryDescription)
        url.set("https://github.com/Graphiks-org/WebGPU")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/license/MIT")
            }
        }

        developers {
            developer {
                id.set("amommers")
                name.set("Alexandre Mommers")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/Graphiks-org/WebGPU.git")
            developerConnection.set("scm:git:https://github.com/Graphiks-org/WebGPU.git")
            url.set("https://github.com/Graphiks-org/WebGPU")
        }
    }
}
