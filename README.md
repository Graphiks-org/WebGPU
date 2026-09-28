# WebGPU

[![Tests](https://github.com/Graphiks-org/WebGPU/actions/workflows/test.yml/badge.svg)](https://github.com/Graphiks-org/WebGPU/actions/workflows/test.yml)
[![Documentation](https://github.com/Graphiks-org/WebGPU/actions/workflows/docs.yml/badge.svg)](https://github.com/Graphiks-org/WebGPU/actions/workflows/docs.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

WebGPU provides Kotlin Multiplatform types and interfaces shared by browser and native WebGPU
implementations. The repository is **incubating**: the API and published coordinates may evolve.
Its Kotlin bindings are generated from versioned WebGPU specification inputs.

## Modules

| Module | Maven coordinate | Purpose |
| --- | --- | --- |
| `webgpu-api` | `org.graphiks:webgpu-api` | Portable interfaces, enums, aliases, and buffers |
| `webgpu-descriptors` | `org.graphiks:webgpu-descriptors` | Descriptor implementations on the portable API |
| `webgpu-web-bindings` | `org.graphiks:webgpu-web-bindings` | WebGPU JavaScript bindings and Kotlin JS/Wasm interop |
| `webgpu-browser` | `org.graphiks:webgpu-browser` | Browser implementation of the Graphiks WebGPU API |
| `suite-core` | `org.graphiks:suite-core` | Execution contract for portable validation cases |
| `suite-acid-tests` | `org.graphiks:suite-acid-tests` | Portable acid tests for the public API |
| `suite-demos` | `org.graphiks:suite-demos` | Portable particle demo scene and reproducible data |
| `suite-browser` | Not published | Browser runner that executes the suite and reports results |
| `webgpu-specifications` | Not published | Versioned specification and documentation inputs |

`webgpu-api` and `webgpu-descriptors` configure JVM, Android, JS, Wasm JS, and Kotlin/Native
targets; `webgpu-web-bindings` and `webgpu-browser` configure JS and Wasm JS. See the
[architecture guide](docs/docs/architecture.md) for boundaries and the [type mapping](docs/docs/type-mapping/index.md)
for WebGPU-to-Kotlin decisions.

## Use the API

Add the module your application needs. Substitute an available version for `<version>`; the
repository's development default is `0.1.0-SNAPSHOT`, which does not imply that a release is
already published.

```kotlin
dependencies {
    implementation("org.graphiks:webgpu-api:<version>")
    // Browser implementation: implementation("org.graphiks:webgpu-browser:<version>")
    // Optional: implementation("org.graphiks:webgpu-descriptors:<version>")
    // Direct interop: implementation("org.graphiks:webgpu-web-bindings:<version>")
}
```

`webgpu-browser` is the entry point for browser applications: it provides `requestAdapter`, the
resource wrappers, and the canvas surface helpers. WebGPU access requires a compatible browser and
a secure context. When using it, also add `webgpu-descriptors` for the Kotlin descriptor classes,
and reference the `org.graphiks.webgpu.browser` and `org.graphiks.webgpu.descriptors` packages.
`getCanvasSurface()` extends the module's lightweight `org.graphiks.webgpu.browser.HTMLCanvasElement`;
cast your DOM canvas to that type. The module does not bring a DOM wrapper library transitively.

For example, a portable texture swizzle encodes to the string required by WebGPU:

```kotlin
import org.graphiks.webgpu.GPUTextureSwizzle

val identity = GPUTextureSwizzle().toWebGpuString() // "rgba"
```

This behavior is covered by `GPUTextureSwizzleTest`. See [Getting started](docs/docs/getting-started.md)
for requirements and dependency notes.

## Validate the contract with the suite

Graphiks WebGPU Suite exercises the public contract with portable acid tests that run against a real
browser WebGPU implementation. `suite-core` holds the execution contract, `suite-acid-tests` the
cases, `suite-demos` a portable compute particle scene, and `suite-browser` runs them on Kotlin/JS
and Kotlin/Wasm JS and publishes a report. The site's Validation page presents the contract
inventory, the behaviour coverage, and the published results, and links to both local launches; its
Demos page presents the interactive particle demo.

Run the browser suite locally with JDK 25 and Node 22:

```sh
npm install --prefix tools
npm exec --prefix tools -- playwright install chromium
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
node tools/build-site.mjs
```

Each `run-browser.mjs` invocation writes a report under `build/reports/` and exits non-zero when the
run is incomplete or failed. See [docs/running.md](docs/running.md) for the published matrix, the
demo routes and consumption from a binding, and [docs/verification.md](docs/verification.md) for
recorded evidence. Results published on the site come from identified runs and are never conflated
with a local execution.

## Build, test, and read the docs

Use JDK 25 and the repository's Gradle wrapper:

```sh
./gradlew check
./gradlew :docs:embedDokkaIntoMkDocs
mkdocs build -f docs/mkdocs.yml
```

The site build needs MkDocs Material and `mkdocs-static-i18n`. The source guides are in English
and French under [`docs/docs`](docs/docs/index.md); the [testing guide](docs/docs/testing.md)
lists targeted business-test tasks. API reference is generated with Dokka and embedded into the
site. The [specification maintenance guide](docs/docs/specification-maintenance.md) contains the
manual download, documentation, and binding-generation commands. They are separate from `check`.

## Contribute

Read [Contributing](CONTRIBUTING.md), the [Code of Conduct](CODE_OF_CONDUCT.md), the
[Security policy](SECURITY.md), and [Support](SUPPORT.md) before submitting a change. Notable
changes are recorded in the [Changelog](CHANGELOG.md). French versions of the community policies
are linked from their English pages.

WebGPU is licensed under the [MIT License](LICENSE).
