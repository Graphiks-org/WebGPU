# Changelog

Notable changes to WebGPU are recorded here. Entries follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and releases use [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Bilingual documentation, contributor guidance, and repository automation.
- `org.graphiks:webgpu-browser`, a browser implementation for Kotlin/JS and Kotlin/Wasm JS with adapter acquisition, resource wrappers, descriptor conversions, and canvas surfaces.
- `WebGpuRecord` and nullable browser binding values in `org.graphiks:webgpu-web-bindings`.
- Graphiks WebGPU Suite foundation modules (`suite-core`, `suite-acid-tests`, `suite-browser`) with a portable `AcidCase` contract, eleven browser acid tests typed with `@AcidTest` annotations, a headless Chromium runner, a generated contract inventory, and a bilingual (English/French) Validation page published under the documentation site's `suite/` path.
- `org.graphiks:suite-demos`, a portable compute particle scene, plus a bilingual Demos gallery and GPU readback checks (`--demo-check`) published under the suite site's `demos/` path.

### Fixed

- `webgpu-browser` no longer throws on Kotlin/Wasm when an implementation omits a `GPUSupportedLimits` property; an absent limit is read as zero.

### Changed

- Require contribution pull requests from forks on `feat/`, `fix/`, or `chore/` branches based on the current `master` commit.
- Drop the deprecated macOS x64 and watchOS x64 native targets.
- Rename `webgpu-web` to `webgpu-web-bindings` and move generated descriptors and bindings to the `org.graphiks.webgpu.descriptors` and `org.graphiks.webgpu.bindings` packages.
- Move the type mapping into the documentation site as a dedicated bilingual section (`docs/docs/type-mapping/`) and remove the root `TYPE_MAPPING.md`.

### Removed

- Remove the deprecated descriptor type aliases (`Size3D`, `ImageCopyTexture`, `ImageCopyBuffer`, `ColorAttachment`, `DepthStencilAttachment`).
- Remove the redundant browser-target and public-ABI CI steps; `./gradlew check` already runs those tasks on every platform.
