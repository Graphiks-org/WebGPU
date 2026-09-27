plugins {
    kmp
}

kotlin {
    jvm()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
