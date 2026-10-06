# Enumerations and Flags

## Enumeration implementation

Enumerations in WebGPU are implemented as Kotlin enum classes. The values are derived from the
WebGPU C header specifications available at
[wgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml).

The naming conventions follow the C specifications rather than the IDL values, as they provide a
more natural and intuitive representation for developers.

## Flag types

Flag types (bit flags) are implemented as Kotlin `value class` masks wrapping a `ULong`, so a mask
is a single integer in both directions: descriptors accept it and `GPUBuffer.usage` /
`GPUTexture.usage` return it.

```kotlin
// From bitflags.kt
@JvmInline
value class GPUBufferUsage private constructor(val value: ULong) {
    public infix fun or(other: GPUBufferUsage): GPUBufferUsage
    public infix fun of(values: Array<GPUBufferUsage>): GPUBufferUsage

    public companion object {
        public val None: GPUBufferUsage
        public val MapRead: GPUBufferUsage
        public val MapWrite: GPUBufferUsage
        public val CopySrc: GPUBufferUsage
        // ...

        /** `true` when every bit of [other] is present in this mask. */
        public operator fun GPUBufferUsage.contains(other: GPUBufferUsage): Boolean

        /** Rebuilds a mask from raw bits, preserving unknown bits. */
        public fun fromBits(value: ULong): GPUBufferUsage
    }
}
```

`a in mask` means "every bit of `a` is present in `mask`"; because `None` has no bits, `None in
mask` is true for every mask, including `None` itself.

`fromBits` preserves unknown bits: it performs no GPU validation, so a mask carrying bits the
current contract does not name is still a valid value to pass around, and the backend decides
whether a command accepts it.

## Constants

Constants in WebGPU are transformed into mask values with explicit `ULong` values. This approach
ensures type safety while maintaining compatibility with the native WebGPU headers.

The use of `ULong` as the underlying type aligns with the data type used for constants in the
native WebGPU implementation, ensuring consistent behavior across platforms.
