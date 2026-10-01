# Changelog

Notable changes to WebGPU are recorded here. Entries follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and releases use [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Bilingual documentation, contributor guidance, and repository automation.
- `org.graphiks:webgpu-browser`, a browser implementation for Kotlin/JS and Kotlin/Wasm JS with adapter acquisition, resource wrappers, descriptor conversions, and canvas surfaces.
- `WebGpuRecord` and nullable browser binding values in `org.graphiks:webgpu-web-bindings`.
- Graphiks WebGPU Suite foundation modules (`suite-core`, `suite-acid-tests`, `suite-browser`) with a portable `AcidCase` contract, eleven browser acid tests typed with `@AcidTest` annotations, a headless Chromium runner, a generated contract inventory, and a bilingual (English/French) Validation page published under the documentation site's `suite/` path.
- `org.graphiks:suite-demos`, a portable compute particle scene, plus a bilingual Demos gallery and GPU readback checks (`--demo-check`) published under the suite site's `demos/` path.
- `org.graphiks:suite-benchmarks`, two portable measurement workloads (`transfer.write-buffer`, `compute.encode-submit`) with the explicit `foundations-v1` protocol and GPU readback checks, a `--benchmark` collector mode, and a bilingual Benchmarks page published under the suite site's `benchmarks/` path.
- Extend the browser acid tests: targeted runs (`--cases=id1,id2` / `?cases=id1,id2` into `selected-<target>.json`), declared optional-feature reporting (`CaseResult.missingFeatures`, accepted as `unsupported` only for declared features), and new buffer, binding, transfer, texture-copy, texture-view, format, storage, sampling, render-command, blending, depth/stencil, advanced-command, error and optional-feature cases (eighty-three in total).
- Finalize the browser acid tests at 123 cases (118 mandatory, five optional) covering adapter/device requests and error delivery, shader compilation and asynchronous pipelines, texture aspects, views and comparison sampling, primitive/face/depth-stencil state, submission and mapped-range lifetime, render timestamps, draw limits and alpha-to-coverage. The coverage balance, optional-feature status and remaining contract gaps are published in `docs/acid-coverage.md`.
- `arraybuffer-benchmarks`, an unpublished CPU measurement harness for `webgpu-api` memory operations: the `arraybuffer-cpu-v1` and `arraybuffer-cpu-writers-v1` protocols with JVM, JS, Wasm, Native and Android runners, report validators, a before/after comparator, an Android instrumentation host, and the `docs/arraybuffer-bounds.md` / `docs/arraybuffer-performance.md` guides.

### Fixed

- `webgpu-browser` no longer throws on Kotlin/Wasm when an implementation omits a `GPUSupportedLimits` property; an absent limit is read as zero.
- `webgpu-browser` omits zero-valued maximum limits from a `requiredLimits` record, so a device request never sends a limit the implementation does not expose (older Chromium rejects an unknown `maxImmediateSize` key). The two alignment limits are always sent: zero is an invalid alignment, not an unexposed limit, so a bad request is rejected rather than silently replaced by the default.
- The browser suite runner pins Playwright 1.63.0 / Chromium 153, which implements the contract's four-character `DOMString` texture-view swizzle; `texture.view-swizzle` is executed instead of reported as an environment gap.
- `webgpu-browser` omits an identity `GPUTextureViewDescriptor.swizzle` when creating a view, so a view built from a descriptor no longer sends a swizzle value in the form rejected by Chromium 140's pre-release dictionary encoding.
- `webgpu-api` validates `ArrayBuffer` operations before touching memory: an out-of-range access throws `IndexOutOfBoundsException`, an unaligned offset, a non-divisible typed conversion or an unrepresentable allocation/wrapping size throws `IllegalArgumentException`, sizes are computed in a wide type before multiplication so nothing is silently truncated, a zero-length operation at `offset == size` is valid and never dereferences memory or builds a typed view, and a write rejected by these preconditions leaves the buffer unchanged. Capacities stay `Int.MAX_VALUE` bytes on Web, Android and Native and `Long.MAX_VALUE` bytes on the JVM.

### Changed

- The suite execution contract passes an `AcidContext` (the borrowed device plus a binding-supplied adapter factory) to `AcidCase.run`, so a case can request its own adapter and device; the individual case functions still take a `GPUDevice`.
- Require contribution pull requests from forks on `feat/`, `fix/`, or `chore/` branches based on the current `master` commit.
- Drop the deprecated macOS x64 and watchOS x64 native targets.
- Rename `webgpu-web` to `webgpu-web-bindings` and move generated descriptors and bindings to the `org.graphiks.webgpu.descriptors` and `org.graphiks.webgpu.bindings` packages.
- Move the type mapping into the documentation site as a dedicated bilingual section (`docs/docs/type-mapping/`) and remove the root `TYPE_MAPPING.md`.

### Removed

- Remove the deprecated descriptor type aliases (`Size3D`, `ImageCopyTexture`, `ImageCopyBuffer`, `ColorAttachment`, `DepthStencilAttachment`).
- Remove the redundant browser-target and public-ABI CI steps; `./gradlew check` already runs those tasks on every platform.
