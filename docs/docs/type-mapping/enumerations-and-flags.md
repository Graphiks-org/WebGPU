# Enumerations and Flags

## Enumeration implementation

Enumerations in WebGPU are implemented as Kotlin enum classes that implement the `FlagEnumeration`
interface. The values are derived from the WebGPU C header specifications available at
[wgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml).

The naming conventions follow the C specifications rather than the IDL values, as they provide a
more natural and intuitive representation for developers.

### Example: GPUBufferUsage

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

## Flag types

Flag types (bit flags) are implemented as Kotlin `Set<EnumType>` to provide type-safe operations:

```kotlin
// From typealiases.kt
typealias GPUBufferUsageFlags = Set<GPUBufferUsage>
typealias GPUMapModeFlags = Set<GPUMapMode>
typealias GPUTextureUsageFlags = Set<GPUTextureUsage>
typealias GPUShaderStageFlags = Set<GPUShaderStage>
```

## Constants

Constants in WebGPU are transformed into enum values with explicit `ULong` values. This approach
ensures type safety while maintaining compatibility with the native WebGPU headers.

The use of `ULong` as the underlying type aligns with the data type used for constants in the native
WebGPU implementation, ensuring consistent behavior across platforms.
