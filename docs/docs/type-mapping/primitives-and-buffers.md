# Primitives and Buffers

## Primitive type mapping

Primitives in WebGPU are mapped to Kotlin primitive types directly, ensuring type safety and
performance efficiency. The mapping follows a platform-agnostic strategy to maintain consistency
across all target platforms.

### Basic type mappings

| WebGPU Type | Kotlin Type | Example |
|-------------|-------------|---------|
| `unsigned long` | `UInt` | `GPUSize32`, `GPUIndex32` |
| `long` | `Int` | `GPUSignedOffset32`, `GPUDepthBias` |
| `unsigned long long` | `ULong` | `GPUSize64` |
| `float` | `Float` | Parameters in `setViewport` |
| `double` | `Double` | `GPUColor` properties, `GPUPipelineConstantValue` |
| `boolean` | `Boolean` | Various boolean properties |
| `DOMString` | `String` | Labels, shader code |

### Examples from the codebase

The core module defines type aliases for WebGPU-specific types:

```kotlin
// From typealiases.kt
typealias GPUBufferDynamicOffset = UInt
typealias GPUStencilValue = UInt
typealias GPUSampleMask = UInt
typealias GPUDepthBias = Int
typealias GPUSize64 = ULong
typealias GPUIntegerCoordinate = UInt
typealias GPUIndex32 = UInt
typealias GPUSize32 = UInt
typealias GPUSignedOffset32 = Int
```

In the web-specific implementation, the same values are exposed to JavaScript through the generated
bindings:

```kotlin
// From types.kt in webgpu-web-bindings
external interface WGPUColor : JsAny {
    var r: JsNumber /* double */
    var g: JsNumber /* double */
    var b: JsNumber /* double */
    var a: JsNumber /* double */
}
```

## Buffer types

### ArrayBuffer

`ArrayBuffer` is mapped differently depending on the target platform:

- In browser environments (JavaScript and WebAssembly), it is mapped to the JavaScript
  `ArrayBuffer` type.
- On the JVM, it wraps a `MemorySegment` through `ArrayBuffer.wrap(segment)`.
- On Kotlin/Native targets, it wraps a raw pointer through
  `ArrayBuffer.wrap(pointer, size)`.
- On Android, it wraps a direct `ByteBuffer` through `ArrayBuffer.wrap(buffer)`, or —
  for memory a native library lends the caller, such as a GPU-mapped range between
  map and unmap — the borrowed range itself through `ArrayBuffer.wrap(address, size)`.
  The borrowed view reads and writes that memory in place, in the platform's native
  byte order, and never frees it: the caller keeps owning the memory and must keep it
  valid for the view's whole lifetime.

### AllowSharedBufferSource

`AllowSharedBufferSource` is mapped to the `ArrayBuffer` type across all platforms, which simplifies
the API while maintaining compatibility with the WebGPU standard.
