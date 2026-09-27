pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
rootProject.name = "webgpu-browser-integration"

val publicationRepository = providers.gradleProperty("publicationRepository").orNull
dependencyResolutionManagement {
    repositories {
        if (publicationRepository != null) {
            exclusiveContent {
                forRepository { maven { url = uri(publicationRepository) } }
                filter { includeGroup("org.graphiks") }
            }
        }
        mavenCentral()
        google()
    }
}
if (publicationRepository == null) {
    includeBuild("../..") {
        dependencySubstitution {
            substitute(module("org.graphiks:webgpu-browser")).using(project(":webgpu-browser"))
            substitute(module("org.graphiks:webgpu-descriptors")).using(project(":webgpu-descriptors"))
            substitute(module("org.graphiks:webgpu-api")).using(project(":webgpu-api"))
            substitute(module("org.graphiks:webgpu-web-bindings")).using(project(":webgpu-web-bindings"))
        }
    }
}
