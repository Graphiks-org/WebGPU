# WebGPU to Kotlin Type Mapping

This document outlines the comprehensive strategy for mapping WebGPU types to Kotlin Multiplatform Project implementations. 
It serves as a definitive guide for maintaining consistency across different platforms while ensuring type safety and optimal performance. 
The mapping strategy detailed herein focuses on creating platform-agnostic representations of WebGPU types, facilitating seamless integration across various target platforms supported by Kotlin. 
This documentation is essential for developers working on WebGPU implementations in Kotlin, particularly those dealing with cross-platform compatibility challenges.

## Project Structure

The WebGPU project is organized into several modules:

1. **webgpu-api**: Core module containing platform-agnostic interfaces and type definitions
2. **webgpu-descriptors**: Module containing descriptor implementations
3. **webgpu-web-bindings**: Module containing the generated JavaScript bindings and Kotlin JS/Wasm interop
4. **webgpu-browser**: Module containing the browser implementation: resource wrappers, descriptor converters, and canvas surfaces

## Mapping Input

The WebGPU specifications referenced in this document are derived from the official website
at [https://www.w3.org/TR/webgpu/](https://www.w3.org/TR/webgpu/), particularly the WebIDL definitions provided at the
end of the webpage. These WebIDL definitions serve as the foundation for creating platform-agnostic type mappings and
ensuring compliance with the WebGPU standard. By leveraging these authoritative specifications, the implementation
remains consistent with the official WebGPU guidelines.

The project also references the WebGPU C header specifications available at [https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml) for enumeration values and naming conventions.

## Buffer Types

### ArrayBuffer

`ArrayBuffer` is mapped differently depending on the target platform:
- In browser environments (JavaScript and WebAssembly), it's mapped to the JavaScript `ArrayBuffer` type
- In other platforms (native), it's mapped to a raw pointer

### AllowSharedBufferSource

`AllowSharedBufferSource` is mapped to the `ArrayBuffer` type across all platforms, which simplifies the API while maintaining compatibility with the WebGPU standard.

## Primitive Type Mapping

Primitives in WebGPU are mapped to Kotlin primitive types directly, ensuring type safety and performance efficiency.
The mapping follows a platform-agnostic strategy to maintain consistency across all target platforms in a Kotlin
Multiplatform Project. 

### Basic Type Mappings

| WebGPU Type | Kotlin Type | Example |
|-------------|-------------|---------|
| `unsigned long` | `UInt` | `GPUSize32`, `GPUIndex32` |
| `long` | `Int` | `GPUSignedOffset32`, `GPUDepthBias` |
| `unsigned long long` | `ULong` | `GPUSize64` |
| `float` | `Float` | Parameters in `setViewport` |
| `double` | `Double` | `GPUColor` properties, `GPUPipelineConstantValue` |
| `boolean` | `Boolean` | Various boolean properties |
| `DOMString` | `String` | Labels, shader code |

### Examples from the Codebase

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

In the web-specific implementation, these types are mapped to JavaScript types:

```kotlin
// From types.kt in webgpu-web-bindings
external interface WGPUColor : JsAny {
    var r: JsNumber /* double */
    var g: JsNumber /* double */
    var b: JsNumber /* double */
    var a: JsNumber /* double */
}
```

## Excluded Types

Certain WebGPU types are excluded from the core implementation because they are either browser-specific, redundant, or relate to specialized web contexts. This selective approach helps maintain a clean, platform-agnostic API.

### Browser-specific Types

| Excluded Type | Reason |
|---------------|--------|
| `NavigatorGPU`, `Navigator`, `WorkerNavigator` | These types belong to browser APIs and are not part of the core WebGPU functionality. Including them would unnecessarily couple the code to a browser environment, limiting portability. |

### Web Canvas-related Types

| Excluded Type | Reason |
|---------------|--------|
| `GPUCanvasContext`, `GPUCanvasConfiguration`, `GPUCanvasAlphaMode`, `GPUCanvasToneMappingMode`, `GPUCanvasToneMapping` | These types are primarily used for web-based canvas operations. They are omitted from the core implementation but may be included in web-specific modules if needed. |

### Redundant Dictionary Types

| Excluded Type | Reason |
|---------------|--------|
| `GPUColorDict`, `GPUOrigin2DDict`, `GPUOrigin3DDict`, `GPUExtent3DDict` | These dictionary types are repetitive in structure and purpose. They are consolidated into more streamlined interfaces in the implementation. |

### Web Event Types

| Excluded Type | Reason |
|---------------|--------|
| `GPUUncapturedErrorEvent`, `GPUUncapturedErrorEventInit` | These specialized event types are tied to WebGPU error handling in browsers. They are not essential for the core functionality. |

### Web Worker-related Types

| Excluded Type | Reason |
|---------------|--------|
| `GPUExternalTexture`, `GPUExternalTextureDescriptor`, `GPUExternalTextureBindingLayout`, `GPUCopyExternalImageSource`, `GPUCopyExternalImageDestInfo`, `GPUCopyExternalImageSourceInfo` | These types focus on Web Workers or advanced external texture handling. They are excluded to reduce complexity in the core implementation. |

By excluding these types, the implementation remains focused and avoids unnecessary dependencies on web-specific details. However, some of these types may be implemented in the web-specific modules (`webgpu-web-bindings`) when needed for browser compatibility.

## Union Type Handling

### "Dict" Type Treatment

WebGPU uses union types in some cases, particularly with sequences and dictionary types. To simplify the implementation and ensure type safety, these union types are transformed into single dictionary types.

#### Example: GPUColor

In the WebIDL specification, `GPUColor` is defined as a union type:

```webidl
typedef (sequence<double> or GPUColorDict) GPUColor;
```

In our implementation, this is transformed into a single interface:

```kotlin
// From interfaces.kt
interface GPUColor {
    val r: Double
    val g: Double
    val b: Double
    val a: Double
}
```

This transformation offers several benefits:
1. **Type safety**: The properties are explicitly defined with their types
2. **Clarity**: The interface clearly communicates the expected structure
3. **Consistency**: All implementations must provide the same properties
4. **Platform agnosticism**: The interface can be implemented on any platform

#### Implementation in Different Modules

In the core module, the interface is defined with Kotlin types:

```kotlin
// From interfaces.kt in webgpu-api
interface GPUColor {
    val r: Double
    val g: Double
    val b: Double
    val a: Double
}
```

In the web module, the interface is defined with JavaScript types:

```kotlin
// From types.kt in webgpu-web-bindings
external interface WGPUColor : JsAny {
    var r: JsNumber /* double */
    var g: JsNumber /* double */
    var b: JsNumber /* double */
    var a: JsNumber /* double */
}
```

This approach allows for platform-specific implementations while maintaining a consistent API across all platforms.

### WebIDL Records and Nullable Values

A WebIDL `record<K, V>` is not a Kotlin `Map`. The generated binding represents it as
`WebGpuRecord`, an external `JsAny` created with `createWebGpuRecord()` (a `null`-prototype object)
and filled with `setRecordValue()`. This keeps the dictionary a plain JavaScript object with
enumerable own properties, as the browser APIs require, instead of a `Map` instance. It is used for
pipeline constants and requested/limit dictionaries.

Nullable WebIDL members are preserved through the JavaScript boundary. A nullable binding member is
generated as a nullable parameter or result (`WGPUBindGroup?`, `WGPUBuffer?`), and a `Promise` whose
value is nullable becomes `Promise<JsAny?>`. Browser wrappers therefore pass `null` through rather
than coercing it, and the asynchronous entry points return `Result` values that distinguish an
absent value, a rejected promise, and a cancelled coroutine.

## Enumerations and Constants

### Enumeration Implementation

Enumerations in WebGPU are implemented as Kotlin enum classes that implement the `FlagEnumeration` interface. The values are derived from the WebGPU C header specifications available at [https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml).

The naming conventions follow the C specifications rather than the IDL values, as they provide a more natural and intuitive representation for developers.

#### Example: GPUBufferUsage

In the WebGPU specification, buffer usage flags are defined as constants:

```webidl
namespace GPUBufferUsage {
    const GPUFlagsConstant MAP_READ = 0x0001;
    const GPUFlagsConstant MAP_WRITE = 0x0002;
    const GPUFlagsConstant COPY_SRC = 0x0004;
    // ...
};
```

In our implementation, this is transformed into an enum class:

```kotlin
// From bitflags.kt
enum class GPUBufferUsage(override val value: ULong): FlagEnumeration {
    None(0uL),
    MapRead(1uL),
    MapWrite(2uL),
    CopySrc(4uL),
    CopyDst(8uL),
    Index(16uL),
    Vertex(32uL),
    Uniform(64uL),
    Storage(128uL),
    Indirect(256uL),
    QueryResolve(512uL);
}
```

### Flag Types

Flag types (bit flags) are implemented as Kotlin `Set<EnumType>` to provide type-safe operations:

```kotlin
// From typealiases.kt
typealias GPUBufferUsageFlags = Set<GPUBufferUsage>
typealias GPUMapModeFlags = Set<GPUMapMode>
typealias GPUTextureUsageFlags = Set<GPUTextureUsage>
typealias GPUShaderStageFlags = Set<GPUShaderStage>
```

### Constants

Constants in WebGPU are transformed into enum values with explicit `ULong` values. This approach ensures type safety while maintaining compatibility with the native WebGPU headers.

The use of `ULong` as the underlying type aligns with the data type used for constants in the native WebGPU implementation, ensuring consistent behavior across platforms.

## Dictionary and Interface Types

### Interface-based Implementation

Dictionary types and interfaces in WebGPU are transformed into Kotlin `interface` constructs. This design choice provides several advantages:

1. **Platform agnosticism**: Interfaces can be implemented differently on each platform while maintaining a consistent API
2. **Extensibility**: New functionality can be added through extension functions or interface inheritance
3. **Type safety**: The compiler ensures that all required properties and methods are implemented
4. **Flexibility**: Implementations can be tailored to specific platform requirements

### Core Implementation

The core module (`webgpu-api`) defines platform-agnostic interfaces with Kotlin types:

```kotlin
// From interfaces.kt
interface GPUBufferDescriptor {
    val size: GPUSize64
    val usage: GPUBufferUsageFlags
    val mappedAtCreation: Boolean
}

interface GPUDevice : GPUObjectBase, AutoCloseable {
    val features: GPUSupportedFeatures
    val limits: GPUSupportedLimits
    val adapterInfo: GPUAdapterInfo
    val queue: GPUQueue
    fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer
    // Other methods...
}
```

### Web-specific Implementation

The web module (`webgpu-web-bindings`) defines JavaScript-specific interfaces with the `WGPU` prefix:

```kotlin
// From types.kt
external interface WGPUBufferDescriptor : JsAny, WGPUObjectDescriptorBase {
    var size: JsNumber  /* GPUSize64 */
    var usage: JsNumber  /* GPUBufferUsageFlags */
    var mappedAtCreation: Boolean
}

external interface WGPUDevice : JsAny, WGPUObjectBase {
    var features: JsAny /* GPUSupportedFeatures */
    var limits: WGPUSupportedLimits  /* GPUSupportedLimits */
    var adapterInfo: WGPUAdapterInfo  /* GPUAdapterInfo */
    var queue: WGPUQueue  /* GPUQueue */
    fun createBuffer(descriptor: WGPUBufferDescriptor /* GPUBufferDescriptor */): WGPUBuffer
    // Other methods...
}
```

### Extensibility

This interface-based approach allows for different implementations and extensions:

1. **Basic implementations**: The initial target (e.g., `wgpu4k` or `dawn4k`) provides straightforward implementations of these interfaces.
2. **Advanced implementations**: Other projects can extend these interfaces to provide domain-specific languages (DSLs) or rich utility methods.
3. **Custom extensions**: Developers can add extension functions to enhance functionality without modifying the core interfaces.

### Default Values

Each implementation must respect the default values specified in the WebIDL definitions to ensure consistency with the WebGPU standard. This approach maintains compliance and avoids discrepancies across different platforms.

For example, if a WebIDL property has a default value:

```webidl
dictionary GPUSamplerDescriptor {
    GPUAddressMode addressModeU = "clamp-to-edge";
    // Other properties...
};
```

The Kotlin implementation should respect this default:

```kotlin
interface GPUSamplerDescriptor {
    val addressModeU: GPUAddressMode get() = GPUAddressMode.ClampToEdge
    // Other properties...
}
```

## Conclusion

This document outlines the comprehensive strategy for mapping WebGPU types to Kotlin Multiplatform Project implementations. The key principles of this mapping strategy are:

1. **Platform agnosticism**: Core types are defined in a platform-agnostic way to ensure consistency across different platforms
2. **Type safety**: Kotlin's type system is leveraged to provide compile-time safety for WebGPU operations
3. **Performance**: Direct mappings to native types are used where possible to minimize overhead
4. **Extensibility**: Interface-based design allows for different implementations and extensions
5. **Consistency**: Naming conventions and values are aligned with the official WebGPU specifications

By following these principles, the WebGPU project provides a solid foundation for WebGPU development in Kotlin, enabling developers to create high-performance graphics applications that work across multiple platforms.

### Implementation Guidelines

When implementing or extending the WebGPU types in this project, developers should:

1. Maintain consistency with the WebIDL specifications
2. Follow the naming conventions established in this document
3. Respect the default values specified in the WebIDL definitions
4. Leverage Kotlin's type system to provide compile-time safety
5. Consider platform-specific optimizations where appropriate

By adhering to these guidelines, developers can ensure that their implementations remain compatible with the WebGPU standard while taking advantage of Kotlin's features and the platform-specific optimizations available in this project.
