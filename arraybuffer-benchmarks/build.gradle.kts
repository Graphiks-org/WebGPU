@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kmp
    id("kmp.android")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(25)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "arraybuffer-benchmarks.js"
            }
        }
        nodejs()
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "arraybuffer-benchmarks.js"
            }
        }
        nodejs()
        binaries.executable()
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    linuxX64 {
        binaries {
            executable {
                entryPoint = "org.graphiks.webgpu.benchmarks.main"
            }
        }
    }

    macosArm64 {
        binaries {
            executable {
                entryPoint = "org.graphiks.webgpu.benchmarks.main"
            }
        }
    }

    android {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }

        namespace = "org.graphiks.webgpu.benchmarks"
        compileSdk = 36
        minSdk = 28
    }

    applyDefaultHierarchyTemplate()

    compilerOptions {
        optIn.add("kotlin.ExperimentalUnsignedTypes")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":webgpu-api"))
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    filter {
        failOnNoDiscoveredTests = false
    }
}

// Runs the JVM campaign without a GPU device. Options come from the command line.
tasks.register<JavaExec>("runJvmBenchmarks") {
    group = "verification"
    description = "Runs the JVM ArrayBuffer CPU benchmark campaign."
    mainClass.set("org.graphiks.webgpu.benchmarks.Main_jvmKt")
    val jvmMain = kotlin.jvm().compilations.getByName("main")
    classpath = files(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    // Keep report paths (build/reports/arraybuffer/...) rooted at the repository, like the other
    // runners, instead of the module directory.
    workingDir = rootProject.projectDir
}
