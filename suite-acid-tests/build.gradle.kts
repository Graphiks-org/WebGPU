plugins {
    kmp
    id("kmp.android")
    publish
    id("org.graphiks.webgpu-suite-inventory")
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
        namespace = "org.graphiks.webgpu.suite.acid.tests"
        compileSdk = 36
        minSdk = 28
    }

    compilerOptions {
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }

    sourceSets.commonMain.dependencies {
        api(project(":suite-core"))
        implementation(kotlin("test"))
        implementation(project(":webgpu-descriptors"))
        // Context cases await their own uncaptured-error callback and bound that wait with a
        // timeout instead of sleeping; the portable contract exposes suspend results only.
        implementation(libs.coroutines)
    }
}

val generateSuiteInventory = tasks.named(
    "generateSuiteInventory",
    org.graphiks.webgpu.inventory.GenerateSuiteInventoryTask::class.java,
)
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateSuiteInventory.flatMap { it.generatedSourceDir })
