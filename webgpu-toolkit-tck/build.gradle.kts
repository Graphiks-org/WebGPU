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
    tvosArm64()
    tvosSimulatorArm64()
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

        namespace = "org.graphiks.webgpu.toolkit.tck"
        compileSdk = 36
        minSdk = 28

        // Creates the androidUnitTest compilation (testDebugUnitTest /
        // testReleaseUnitTest) so the Android source sets are tested on the host JVM.
        withHostTest { }
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

        commonMain.dependencies {
            api(project(":webgpu-toolkit-surface"))
            implementation(libs.coroutines)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kotest)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }

        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }

    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


// AGP creates the host-test compilation lazily (withHostTest); its
// configurations only exist after evaluation, so the Kotest runner is added
// by configuration name, exactly like jvmTest.
project.afterEvaluate {
    dependencies {
        add("androidHostTestImplementation", libs.kotest.runner.junit5)
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

// AGP host tests run the Android source sets on the host JVM; the Kotest
// engine needs the JUnit 5 platform, exactly like jvmTest.
tasks.withType<Test>().matching { it.name == "testAndroidHostTest" || it.name == "testAndroid" }.configureEach {
    useJUnitPlatform()
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
