# WebGPU

WebGPU provides Kotlin Multiplatform types and interfaces for the WebGPU API. The published
modules are `webgpu-api`, `webgpu-descriptors`, `webgpu-web-bindings`, and `webgpu-browser` under
the `org.graphiks` group. The `webgpu-specifications` module holds the versioned source material
used to generate bindings.

Browser applications start from `webgpu-browser`, which provides `requestAdapter`, the resource
wrappers, descriptor converters, and canvas surfaces. `webgpu-web-bindings` exposes the generated
JavaScript bindings for direct interop.

- [Get started](getting-started.md) with a dependency and a small API example.
- [Migrate the public API](public-api-migration.md) when upgrading across the contract rework.
- [Understand the modules](architecture.md) and their platform boundaries.
- [Run the business tests](testing.md) before submitting a change.
- [Maintain the specification](specification-maintenance.md) when upstream WebGPU changes.
- Consult the [type mapping](type-mapping/index.md) for WebGPU-to-Kotlin contracts.
