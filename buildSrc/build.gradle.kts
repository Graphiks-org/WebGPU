plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven {
        name = "sonatypeSnapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
        content {
            includeGroup("io.ygdrasil")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("com.vanniktech:gradle-maven-publish-plugin:${libs.versions.maven.publish.get()}")
    implementation("io.kotest:kotest-framework-plugin-gradle:${libs.versions.kotest.get()}")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    implementation(libs.dokka)
    implementation("dev.opensavvy.dokka.mkdocs:dokka-mkdocs:0.6.3")

    implementation(libs.webidl.util)
    implementation(libs.kaml)
    implementation(libs.wgpu.specs)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.kotlinpoet)
    implementation(libs.coroutines)

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}
