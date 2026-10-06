# Getting started

## Requirements

Use JDK 25 and the Gradle wrapper in this repository. A consumer needs Maven Central; snapshots
also require the Maven Central snapshots repository.

## Add a module

The published coordinates use the `org.graphiks` group. Choose the module your application needs:

```kotlin
dependencies {
    implementation("org.graphiks:webgpu-api:<version>")
    // Descriptor implementations (needed by the browser implementation).
    // implementation("org.graphiks:webgpu-descriptors:<version>")
    // Browser implementation.
    // implementation("org.graphiks:webgpu-browser:<version>")
    // Direct JavaScript interop.
    // implementation("org.graphiks:webgpu-web-bindings:<version>")
}
```

Replace `<version>` with an available release or snapshot version. The repository's development
default is `0.1.0-SNAPSHOT`; that value does not imply a release has already been published.

## Use the browser implementation

Add `webgpu-browser` and `webgpu-descriptors` to the shared JS/Wasm source set. The browser
implementation exposes the `org.graphiks.webgpu.browser` package; the descriptor classes used below
belong to `org.graphiks.webgpu.descriptors`. WebGPU access requires a compatible browser and a
secure context.

```kotlin
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.BufferDescriptor

suspend fun createExampleBuffer() {
    val adapter = requestAdapter().getOrThrow()
    val device = adapter.requestDevice().getOrThrow()
    try {
        val buffer = device.createBuffer(
            BufferDescriptor(
                size = 16uL,
                usage = GPUBufferUsage.CopyDst or GPUBufferUsage.Storage,
            ),
        )
        buffer.close()
    } finally {
        device.close()
        adapter.close()
    }
}
```

The canvas helpers (`getCanvasSurface`, `SurfaceConfiguration`) also live in
`org.graphiks.webgpu.browser`. `getCanvasSurface()` is an extension on the module's lightweight
`org.graphiks.webgpu.browser.HTMLCanvasElement`; cast your DOM canvas to that type (for example
`(canvas as HTMLCanvasElement).getCanvasSurface()`). `webgpu-browser` does not bring a DOM wrapper
library transitively, so add one (for example `kotlin-browser`) when your code needs DOM types.

## Use a portable type

```kotlin
import org.graphiks.webgpu.GPUTextureSwizzle

val identity = GPUTextureSwizzle().toWebGpuString() // "rgba"
```

The same behavior is asserted by `GPUTextureSwizzleTest` in `webgpu-api`.

See [Architecture](architecture.md) for the module boundaries and [Type Mapping](type-mapping/index.md)
for the WebGPU-to-Kotlin type contracts.
