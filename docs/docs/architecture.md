# Architecture

| Module | Responsibility | Targets |
| --- | --- | --- |
| `webgpu-api` | Portable WebGPU interfaces, enums, aliases, and buffers | JVM, Android, JS, Wasm JS, Apple, Linux, Windows, and other configured Kotlin/Native targets |
| `webgpu-descriptors` | Descriptor implementations built on `webgpu-api` | JVM, Android, JS, Wasm JS, and configured Kotlin/Native targets |
| `webgpu-web-bindings` | Generated JavaScript bindings and Kotlin JS/Wasm interop | JS and Wasm JS |
| `webgpu-browser` | Browser implementation: resource wrappers, descriptor converters, and canvas surfaces | JS and Wasm JS |
| `webgpu-specifications` | Checked-in HTML, IDL, and documentation inputs for generation | JVM build tooling |
| `suite-core` | Typed identities, annotations and execution contract for acid cases | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-acid-tests` | Portable annotated cases and generated catalogue | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-demos` | Portable compute particle scene and reproducible data | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-benchmarks` | Portable transfer and compute workloads and measurements | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-browser` | Real browser execution and JSON reports | JS and Wasm JS |

The four first modules are Maven publications. `webgpu-specifications` is a build-time source
module, not a consumer artifact. The shared API does not own a GPU device or a renderer. Browser
interop belongs in `webgpu-web-bindings`; `webgpu-browser` is the browser implementation that
consumes the portable contracts and the bindings; native implementations consume the portable
contracts.

`suite-core`, `suite-acid-tests`, `suite-demos` and `suite-benchmarks` also use the Maven publication
convention; `suite-browser` is an application. The suite uses local project dependencies and the
repository's Gradle conventions. Each case lives in its own file, grouped by family, and exercises
the public API. Native bindings consume the shared artifacts and run them in their own repositories.

The `org.graphiks.webgpu-suite-inventory` build-logic plugin generates symbol constants, the case
catalogue and manifests from the API sources and `@AcidTest` annotations. The site inventory combines
these outputs with authored uncovered behaviours and EN/FR resources. Generated outputs are not
versioned. The [Validation site](suite.md) is deployed with the documentation under `suite/`.

The Kotlin binding generator reads the checked-in WebGPU IDL and documentation YAML and writes
source in the public modules. Edit the source specification or documentation data, then run the
manual flow in [Specification maintenance](specification-maintenance.md). See the canonical
[type mapping](type-mapping/index.md) for individual type decisions.
