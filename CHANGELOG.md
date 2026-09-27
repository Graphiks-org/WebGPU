# Changelog

Notable changes to WebGPU are recorded here. Entries follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and releases use [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Bilingual documentation, contributor guidance, and repository automation.
- `org.graphiks:webgpu-browser`, a browser implementation for Kotlin/JS and Kotlin/Wasm JS with adapter acquisition, resource wrappers, descriptor conversions, and canvas surfaces.
- `WebGpuRecord` and nullable browser binding values in `org.graphiks:webgpu-web-bindings`.

### Changed

- Require contribution pull requests from forks on `feat/`, `fix/`, or `chore/` branches based on the current `master` commit.
- Drop the deprecated macOS x64 and watchOS x64 native targets.
- Rename `webgpu-web` to `webgpu-web-bindings` and move generated descriptors and bindings to the `org.graphiks.webgpu.descriptors` and `org.graphiks.webgpu.bindings` packages.
