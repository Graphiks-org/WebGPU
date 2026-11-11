plugins {
    id("org.graphiks.webgpu-generator")
    id("org.graphiks.webgpu-specification-fetcher")
}

allprojects {
    group = "org.graphiks.webgpu"
    version = (findProperty("releaseVersion") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "0.1.0-SNAPSHOT"
}
