plugins {
    generator
    id("io.ygdrasil.webgpu-specification-fetcher")
}

allprojects {
    group = "io.ygdrasil"
    version = (findProperty("releaseVersion") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "0.0.10-SNAPSHOT"
}
