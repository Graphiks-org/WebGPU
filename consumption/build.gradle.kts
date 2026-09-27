plugins {
    kotlin("multiplatform") version "2.4.20"
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

    linuxX64()
    macosArm64()

    sourceSets.commonMain.dependencies {
        implementation("org.graphiks:suite-acid-tests:${providers.gradleProperty("suiteVersion").get()}")
    }
}
