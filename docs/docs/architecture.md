# Architecture

| Module | Responsibility | Targets |
| --- | --- | --- |
| `webgpu-api` | Portable WebGPU interfaces, enums, aliases, and buffers | JVM, Android, JS, Wasm JS, Apple, Linux, Windows, and other configured Kotlin/Native targets |
| `webgpu-descriptors` | Descriptor implementations built on `webgpu-api` | JVM, Android, JS, Wasm JS, and configured Kotlin/Native targets |
| `webgpu-web-bindings` | Generated JavaScript bindings and Kotlin JS/Wasm interop | JS and Wasm JS |
| `webgpu-browser` | Browser implementation: resource wrappers, descriptor converters, and canvas surfaces | JS and Wasm JS |
| `webgpu-specifications` | Checked-in HTML, IDL, and documentation inputs for generation | JVM build tooling |

The four first modules are Maven publications. `webgpu-specifications` is a build-time source
module, not a consumer artifact. The shared API does not own a GPU device or a renderer. Browser
interop belongs in `webgpu-web-bindings`; `webgpu-browser` is the browser implementation that
consumes the portable contracts and the bindings; native implementations consume the portable
contracts.

The Kotlin binding generator reads the checked-in WebGPU IDL and documentation YAML and writes
source in the public modules. Edit the source specification or documentation data, then run the
manual flow in [Specification maintenance](specification-maintenance.md). See the canonical
[type mapping](generated/type-mapping.md) for individual type decisions.
