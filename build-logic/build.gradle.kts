plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
}

gradlePlugin {
    plugins {
        create("webGpuSpecificationFetcher") {
            id = "org.graphiks.webgpu-specification-fetcher"
            implementationClass = "org.graphiks.webgpu.fetcher.WebGpuSpecificationFetcherPlugin"
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
