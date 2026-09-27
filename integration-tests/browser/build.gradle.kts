plugins { kotlin("multiplatform") version "2.4.20" }

val testedVersion = providers.gradleProperty("testedVersion").getOrElse("0.1.0-SNAPSHOT")

kotlin {
    js { browser { testTask { useKarma { useChromeHeadless() } } } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser { testTask { useKarma { useChromeHeadless() } } } }

    compilerOptions {
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        implementation("org.graphiks:webgpu-browser:$testedVersion")
        implementation("org.graphiks:webgpu-descriptors:$testedVersion")
    }
}
