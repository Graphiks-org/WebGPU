rootProject.name = "webgpu-ktypes-root"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("kotlinWrappers") {
            val wrappersVersion = "2026.9.2"
            from("org.jetbrains.kotlin-wrappers:kotlin-wrappers-catalog:$wrappersVersion")
        }
    }
}

include("webgpu-ktypes")
include("webgpu-ktypes-descriptors")
include("webgpu-ktypes-web")
include("webgpu-ktypes-specifications")
