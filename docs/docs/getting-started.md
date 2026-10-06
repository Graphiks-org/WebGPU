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

## Interop with the browser backend

Browser wrappers expose their raw handle as `handler` for interop. Calling operations on the
handle directly can invalidate the wrapper's contract (ownership and close-once semantics), so
prefer the wrapper's own API.

Texture ownership is explicit. `Texture.wrapOwned(handler)` takes over the destruction of the
handle; `Texture.wrapBorrowed(handler)` wraps a texture owned by someone else (for example the
canvas of a `CanvasSurface`) and never destroys it. Textures from `device.createTexture` are
owned; textures from `CanvasSurface.getCurrentTexture` are borrowed. `CanvasSurface` is
`AutoCloseable`: `close()` unconfigures the canvas context and does not close the device passed
to `configure`.

```kotlin
import org.graphiks.webgpu.browser.Texture

// Owned: closing the wrapper destroys the texture.
val owned = Texture.wrapOwned(device.createTexture(descriptor).let { it as org.graphiks.webgpu.browser.Texture }.handler)

// Borrowed: closing the wrapper leaves the canvas texture alone.
val borrowed = Texture.wrapBorrowed(surface.getCurrentTexture().texture.let { it as org.graphiks.webgpu.browser.Texture }.handler)
```

## Map a buffer with a scoped range

`GPUBuffer.withMappedRange` maps a range, runs a non-suspending block with the borrowed view and
unmaps in a `finally`, so the buffer is back to `Unmapped` even when the block throws:

```kotlin
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.withMappedRange

suspend fun writeExample(device: GPUDevice) {
    val buffer = device.createBuffer(
        BufferDescriptor(size = 16uL, usage = GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc),
    )
    buffer.use {
        it.withMappedRange(GPUMapMode.Write) { view ->
            view.setUInts(0uL, uintArrayOf(1u, 2u, 3u, 4u))
        }
    }
}
```

The view passed to the block is borrowed: it is invalidated by the `unmap()` the helper performs
and must not be retained or returned. Resources created by a device are owned by the caller:
`close()` destroys them (a native backend also releases its owned reference), and closing an
already closed resource does not release the same reference twice. A texture obtained from a
canvas context is borrowed: closing its wrapper does not destroy the canvas-owned texture.

## Use a portable type

```kotlin
import org.graphiks.webgpu.GPUTextureSwizzle

val identity = GPUTextureSwizzle().toWebGpuString() // "rgba"
```

The same behavior is asserted by `GPUTextureSwizzleTest` in `webgpu-api`.

See [Architecture](architecture.md) for the module boundaries and [Type Mapping](type-mapping/index.md)
for the WebGPU-to-Kotlin type contracts.
