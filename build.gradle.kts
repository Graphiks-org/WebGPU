plugins {
    generator
    id("org.graphiks.webgpu-specification-fetcher")
}

allprojects {
    group = "org.graphiks"
    version = (findProperty("releaseVersion") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "0.1.0-SNAPSHOT"
}
