pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val isolatedRepository = providers.gradleProperty("publicationRepository").orNull
            ?: error("Set -PpublicationRepository to the isolated Maven repository")
        exclusiveContent {
            forRepository {
                maven {
                    name = "suitePublication"
                    url = uri(isolatedRepository)
                }
            }
            filter {
                includeGroup("org.graphiks")
            }
        }
        mavenCentral()
        google()
    }
}

rootProject.name = "suite-consumption"
