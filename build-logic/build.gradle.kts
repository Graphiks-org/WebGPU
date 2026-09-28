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
        create("webGpuGenerator") {
            id = "org.graphiks.webgpu-generator"
            implementationClass = "org.graphiks.webgpu.generator.GeneratorPlugin"
        }
        create("webGpuSuiteInventory") {
            id = "org.graphiks.webgpu-suite-inventory"
            implementationClass = "org.graphiks.webgpu.inventory.SuiteInventoryPlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.webidl.util)
    implementation(libs.kaml)
    implementation(libs.kotlinpoet)
    implementation(libs.wgpu.specs)
    implementation(libs.jsoup)
    implementation(libs.coroutines)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}
