plugins {
    kmp
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

    linuxX64()
    macosArm64()

    compilerOptions {
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }

    sourceSets.commonMain.dependencies {
        api(project(":suite-core"))
        implementation(kotlin("test"))
        implementation(project(":webgpu-descriptors"))
    }
}

val generateSuiteInventory = tasks.named(
    "generateSuiteInventory",
    org.graphiks.webgpu.inventory.GenerateSuiteInventoryTask::class.java,
)
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateSuiteInventory.flatMap { it.generatedSourceDir })
