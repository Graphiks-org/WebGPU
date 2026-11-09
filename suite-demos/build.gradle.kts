plugins {
    kmp
    id("kmp.android")
    publish
}

kotlin {
    jvmToolchain(25)

    jvm()

    js {
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    macosArm64()
    linuxArm64()
    linuxX64()
    mingwX64()
    androidNativeArm64()
    androidNativeX64()

    android {
        namespace = "org.graphiks.webgpu.suite.demos"
        compileSdk = 36
        minSdk = 28
    }

    compilerOptions {
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }

    sourceSets.commonMain.dependencies {
        api(project(":suite-core"))
        api(project(":webgpu-toolkit-surface"))
        implementation(project(":webgpu-descriptors"))
        implementation(libs.coroutines)
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(project(":webgpu-toolkit-tck"))
        implementation(libs.coroutines)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    }
}
