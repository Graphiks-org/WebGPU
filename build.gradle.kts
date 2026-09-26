plugins {
    generator
    id("io.ygdrasil.webgpu-specification-fetcher")
}

allprojects {
    group = "io.ygdrasil"
    version = (findProperty("releaseVersion") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "0.1.0-SNAPSHOT"
}
