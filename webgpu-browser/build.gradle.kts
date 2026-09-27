plugins {
    kmp
    publish
}

kotlin {
    js {
        browser { testTask { useKarma { useChromeHeadless() } } }
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser { testTask { useKarma { useChromeHeadless() } } }
        nodejs()
    }
    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":webgpu-api"))
            api(project(":webgpu-web-bindings"))
            api(kotlinWrappers.browser)
            api(kotlinWrappers.web)
            implementation(libs.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":webgpu-descriptors"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
    }
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
