# Dictionaries and Unions

## Union type handling

### "Dict" type treatment

WebGPU uses union types in some cases, particularly with sequences and dictionary types. To simplify
the implementation and ensure type safety, these union types are transformed into single dictionary
types.

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

1. **Type safety** — the properties are explicitly defined with their types.
2. **Clarity** — the interface clearly communicates the expected structure.
3. **Consistency** — all implementations must provide the same properties.
4. **Platform agnosticism** — the interface can be implemented on any platform.

#### Implementation in different modules

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

In the binding module, the interface is defined with JavaScript types:

```kotlin
// From types.kt in webgpu-web-bindings
external interface WGPUColor : JsAny {
    var r: JsNumber /* double */
    var g: JsNumber /* double */
    var b: JsNumber /* double */
    var a: JsNumber /* double */
}
```

This approach allows for platform-specific implementations while maintaining a consistent API across
all platforms.

### WebIDL records and nullable values

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

## Dictionary and interface types

### Interface-based implementation

Dictionary types and interfaces in WebGPU are transformed into Kotlin `interface` constructs. This
design choice provides several advantages:

1. **Platform agnosticism** — interfaces can be implemented differently on each platform while
   maintaining a consistent API.
2. **Extensibility** — new functionality can be added through extension functions or interface
   inheritance.
3. **Type safety** — the compiler ensures that all required properties and methods are implemented.
4. **Flexibility** — implementations can be tailored to specific platform requirements.

### Core implementation

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

### Web-specific implementation

The binding module (`webgpu-web-bindings`) defines JavaScript-specific interfaces with the `WGPU`
prefix:

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

1. **Basic implementations** — an implementation provides straightforward versions of these
   interfaces.
2. **Advanced implementations** — other projects can extend these interfaces to provide
   domain-specific languages (DSLs) or rich utility methods.
3. **Custom extensions** — developers can add extension functions without modifying the core
   interfaces.

### Default values

Each implementation must respect the default values specified in the WebIDL definitions to ensure
consistency with the WebGPU standard. This approach maintains compliance and avoids discrepancies
across different platforms.

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
