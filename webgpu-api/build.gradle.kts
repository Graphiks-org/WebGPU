@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kmp
    id("kmp.android")
    publish
    id("io.kotest")
    id("com.google.devtools.ksp")
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }

    js {
        browser()
        nodejs()
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()
    macosArm64()
    macosX64()
    linuxArm64()
    linuxX64()
    mingwX64()
    androidNativeArm64()
    androidNativeX64()

    androidLibrary {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }

        namespace = "org.graphiks.webgpu.ktypes"
        compileSdk = 36
        minSdk = 28
    }


    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    applyDefaultHierarchyTemplate()

    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {

        webMain {
            dependencies {
                api(kotlinWrappers.js)
            }
        }


        val commonNativeMain by creating {
            dependsOn(commonMain.get())
        }

        nativeMain.get().dependsOn(commonNativeMain)
        jvmMain.get().dependsOn(commonNativeMain)
        androidMain.get().dependsOn(commonNativeMain)

        commonTest {
            dependencies {
                implementation(libs.bundles.kotest)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


tasks.withType<Test>().configureEach {
    filter {
        failOnNoDiscoveredTests = false
    }
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
