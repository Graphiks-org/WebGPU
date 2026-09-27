# Getting started

## Requirements

Use JDK 25 and the Gradle wrapper in this repository. A consumer needs Maven Central; snapshots
also require the Maven Central snapshots repository.

## Add a module

The published coordinates use the `org.graphiks` group. Choose the module your application needs:

```kotlin
dependencies {
    implementation("org.graphiks:webgpu-api:<version>")
    // Optional: descriptor implementations or browser interop.
    // implementation("org.graphiks:webgpu-descriptors:<version>")
    // implementation("org.graphiks:webgpu-web:<version>")
}
```

Replace `<version>` with an available release or snapshot version. The repository's development
default is `0.1.0-SNAPSHOT`; that value does not imply a release has already been published.

## Use a portable type

```kotlin
import org.graphiks.webgpu.GPUTextureSwizzle

val identity = GPUTextureSwizzle().toWebGpuString() // "rgba"
```

The same behavior is asserted by `GPUTextureSwizzleTest` in `webgpu-api`.

See [Architecture](architecture.md) for the module boundaries and [Type Mapping](generated/type-mapping.md)
for the WebGPU-to-Kotlin type contracts.
