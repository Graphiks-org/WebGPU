# Type Mapping

This section describes how WebGPU types map to Kotlin Multiplatform implementations across the
project's modules. It is the reference for keeping the portable API, the descriptors, and the
JavaScript bindings consistent while remaining type-safe and portable.

## Modules

The mapping is spread over four published modules:

1. **`webgpu-api`** — portable interfaces and type definitions.
2. **`webgpu-descriptors`** — descriptor implementations built on the portable API.
3. **`webgpu-web-bindings`** — generated JavaScript bindings and Kotlin JS/Wasm interop.
4. **`webgpu-browser`** — browser implementation: resource wrappers, descriptor converters, and
   canvas surfaces.

## Mapping inputs

The WebIDL definitions at [w3.org/TR/webgpu](https://www.w3.org/TR/webgpu/) are the foundation for
the portable mappings. Enumeration values and naming conventions follow the WebGPU C headers
described by
[wgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml).

## Principles

1. **Platform agnosticism** — core types are defined in a platform-agnostic way so they behave the
   same on every target.
2. **Type safety** — Kotlin's type system provides compile-time safety for WebGPU operations.
3. **Performance** — direct mappings to native types are used where possible.
4. **Extensibility** — the interface-based design allows different implementations and extensions.
5. **Consistency** — naming conventions and values align with the official WebGPU specifications.

## In this section

- [Primitives and buffers](primitives-and-buffers.md)
- [Enumerations and flags](enumerations-and-flags.md)
- [Dictionaries and unions](dictionaries-and-unions.md)
- [Excluded types](excluded-types.md)
