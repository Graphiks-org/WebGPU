plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
}

gradlePlugin {
    plugins {
        create("webGpuSpecificationFetcher") {
            id = "io.ygdrasil.webgpu-specification-fetcher"
            implementationClass = "io.ygdrasil.webgpu.fetcher.WebGpuSpecificationFetcherPlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}
