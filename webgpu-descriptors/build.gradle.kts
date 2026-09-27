@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kmp
    id("kmp.android")
    publish
}

kotlin {
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
    macosArm64()
    linuxArm64()
    linuxX64()
    mingwX64()
    androidNativeArm64()
    androidNativeX64()

    android {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }

        namespace = "org.graphiks.webgpu.ktypes.descriptors"
        compileSdk = 36
        minSdk = 28
    }


    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    compilerOptions {
        allWarningsAsErrors = true
    }

    sourceSets.commonMain.dependencies {
        implementation(project(":webgpu-api"))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
