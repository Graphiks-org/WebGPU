plugins {
    kmp
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(25)

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "suite.js"
            }
        }
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "suite.js"
            }
        }
        binaries.executable()
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    sourceSets.commonMain.dependencies {
        implementation(project(":suite-acid-tests"))
        implementation(project(":webgpu-browser"))
        implementation(project(":webgpu-descriptors"))
        implementation(libs.coroutines)
        implementation(libs.kotlinx.serialization.json)
    }
}
