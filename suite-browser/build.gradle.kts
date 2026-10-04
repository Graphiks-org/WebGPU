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
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()

        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "suite.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()

        binaries.executable()
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    sourceSets.commonMain.dependencies {
        implementation(project(":suite-acid-tests"))
        implementation(project(":suite-demos"))
        implementation(project(":suite-benchmarks"))
        implementation(project(":webgpu-browser"))
        implementation(project(":webgpu-descriptors"))
        implementation(libs.coroutines)
        implementation(libs.kotlinx.serialization.json)
        implementation(kotlin("test"))
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jsTest {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        wasmJsTest {
            dependencies {
                implementation(kotlin("test-wasm-js"))
            }
        }
    }
}
