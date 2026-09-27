plugins {
    kmp
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

    linuxX64()
    macosArm64()

    compilerOptions {
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }

    sourceSets.commonMain.dependencies {
        api(project(":webgpu-api"))
    }
}
