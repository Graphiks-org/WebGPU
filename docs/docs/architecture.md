# Architecture

| Module | Responsibility | Targets |
| --- | --- | --- |
| `webgpu-api` | Portable WebGPU interfaces, enums, aliases, and buffers | JVM, Android, JS, Wasm JS, Apple, Linux, Windows, and other configured Kotlin/Native targets |
| `webgpu-descriptors` | Descriptor implementations built on `webgpu-api` | JVM, Android, JS, Wasm JS, and configured Kotlin/Native targets |
| `webgpu-web` | JavaScript and Wasm JS interop with browser WebGPU values | JS and Wasm JS |
| `webgpu-specifications` | Checked-in HTML, IDL, and documentation inputs for generation | JVM build tooling |

The three first modules are Maven publications. `webgpu-specifications` is a build-time source
module, not a consumer artifact. The shared API does not own a GPU device or a renderer. Browser
interop belongs in `webgpu-web`; native implementations consume the portable contracts.

The Kotlin binding generator reads the checked-in WebGPU IDL and documentation YAML and writes
source in the public modules. Edit the source specification or documentation data, then run the
manual flow in [Specification maintenance](specification-maintenance.md). See the canonical
[type mapping](generated/type-mapping.md) for individual type decisions.
