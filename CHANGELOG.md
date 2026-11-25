# Changelog

Notable changes to WebGPU are recorded here. Entries follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and releases use [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- The WebGPU toolkit (`toolkit-core`, `toolkit-surface`, `toolkit-web`, `toolkit-tck`): opt-in, extension-driven sessions (`withSession`/`openSession`, `executionOwner`, startup hooks, `Pumped`/`EventLoop`/`Manual` progression), presentation (`withSurface`/`withFrame`, manual frame acquisition, present modes), windowing-neutral windows with input events and target swap, and a protocol TCK to validate extensions. Getting-started guide: `docs/docs/toolkit.md` (French twin included).
- `browser`, a browser implementation for Kotlin/JS and Kotlin/Wasm JS; `web-bindings` gains `WebGpuRecord` and nullable binding values.
- The Graphiks WebGPU Suite (`suite-core`, `suite-acid-tests`, `suite-browser`, `suite-demos`, `suite-benchmarks`): a portable acid suite at 147 cases (138 mandatory) run headless on Chromium with bilingual validation pages, four demos — particle scene, image filter lab, cyberpunk city, Gray–Scott reaction-diffusion — with GPU readback checks, and two benchmark workloads with a collector mode.
- Public API contract rework (breaking; details in `docs/docs/public-api-contract.md`): typed usage masks, `GPURequiredLimits?` descriptors, `GPUDevice.awaitLost()`, `GPUBuffer.withMappedRange`, explicit browser texture ownership.
- Android `ArrayBuffer.wrap(address, size)`, a bounds-checked borrowed view over native memory.
- `arraybuffer-benchmarks`, a CPU measurement harness for `ArrayBuffer` operations, with bilingual guides.
- Publish the portable modules across the full KMP target matrix, including Android, Windows and ARM64 tvOS.

### Changed

- Maven coordinates follow the Graphiks naming norm: group `org.graphiks.webgpu`, artifactIds drop the `webgpu-` prefix — `org.graphiks.webgpu:api`, `:descriptors`, `:web-bindings`, `:browser`, `:toolkit-*`, `:suite-*` (previously `org.graphiks:webgpu-*` / `org.graphiks:suite-*`).
- `GPURequestAdapterOptions.featureLevel` now takes the `GPUFeatureLevel` enum (`Core` by default, or `Compatibility`) instead of `String`.
- The suite execution contract passes an `AcidContext` to `AcidCase.run`, so a case can request its own adapter and device.
- Suite reports carry the exact commit and version they were built from; the runner fails closed on a mismatch.
- Rename `webgpu-web` to `webgpu-web-bindings`, with generated descriptors and bindings in `org.graphiks.webgpu.descriptors` / `org.graphiks.webgpu.bindings`.
- Drop the deprecated macOS x64 and watchOS x64 native targets.

### Removed

- Remove the deprecated descriptor type aliases (`Size3D`, `ImageCopyTexture`, `ImageCopyBuffer`, `ColorAttachment`, `DepthStencilAttachment`).

### Fixed

- `webgpu-api` validates `ArrayBuffer` operations before touching memory: bounds, alignment and size preconditions, wide-type size computation, failed writes leave the buffer unchanged.
- Android `ArrayBuffer` typed factories and views use native byte order, without HotSpot-only `Unsafe` access (ART-safe).
- `webgpu-browser` Chromium compatibility: forwards `textureBindingViewDimension`, omits zero-valued limits and identity swizzles, reads absent limits as zero on Wasm.
- Pin `wgpu4k-native` to `v27.0.4` and the suite runner to Playwright 1.63.0 / Chromium 153, so `texture.view-swizzle` executes instead of reporting an environment gap.
