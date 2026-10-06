# Changelog

Notable changes to WebGPU are recorded here. Entries follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and releases use [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Android `ArrayBuffer.wrap(address, size)`: a borrowed, non-owning view over native memory (native byte order, bounds-checked) so a backend can expose a range a native library lends the caller — such as a GPU-mapped buffer between map and unmap — without copying it.
- Publish the portable suite modules across the `webgpu-api` KMP target matrix, including Android, Windows, and ARM64 tvOS targets.
- Bilingual documentation, contributor guidance, and repository automation.
- `org.graphiks:webgpu-browser`, a browser implementation for Kotlin/JS and Kotlin/Wasm JS with adapter acquisition, resource wrappers, descriptor conversions, and canvas surfaces.
- `WebGpuRecord` and nullable browser binding values in `org.graphiks:webgpu-web-bindings`.
- Graphiks WebGPU Suite foundation modules (`suite-core`, `suite-acid-tests`, `suite-browser`) with a portable `AcidCase` contract, eleven browser acid tests typed with `@AcidTest` annotations, a headless Chromium runner, a generated contract inventory, and a bilingual (English/French) Validation page published under the documentation site's `suite/` path.
- `org.graphiks:suite-demos`, a portable compute particle scene, plus a bilingual Demos gallery and GPU readback checks (`--demo-check`) published under the suite site's `demos/` path.
- `org.graphiks:suite-benchmarks`, two portable measurement workloads (`transfer.write-buffer`, `compute.encode-submit`) with the explicit `foundations-v1` protocol and GPU readback checks, a `--benchmark` collector mode, and a bilingual Benchmarks page published under the suite site's `benchmarks/` path.
- Extend the browser acid tests: targeted runs (`--cases=id1,id2` / `?cases=id1,id2` into `selected-<target>.json`), declared optional-feature reporting (`CaseResult.missingFeatures`, accepted as `unsupported` only for declared features), and new buffer, binding, transfer, texture-copy, texture-view, format, storage, sampling, render-command, blending, depth/stencil, advanced-command, error and optional-feature cases (eighty-three in total).
- Finalize the browser acid tests at 123 cases (118 mandatory, five optional) covering adapter/device requests and error delivery, shader compilation and asynchronous pipelines, texture aspects, views and comparison sampling, primitive/face/depth-stencil state, submission and mapped-range lifetime, render timestamps, draw limits and alpha-to-coverage. The coverage balance, optional-feature status and remaining contract gaps are published in `docs/acid-coverage.md`.
- Extend the browser acid tests to 132 cases (125 mandatory, seven optional) with a follow-up increment: storage-binding range length, `minBindingSize` rejection, post-bundle pass-state reset, partial texture initialization, discard re-initialization, min/mag filter selection, timestamp write-index validation for both pass types, and the unenabled `TimestampQuery` refusal.
- French twins of the repo-level acid records: `docs/acid-coverage.fr.md` and `docs/verification.fr.md`, with the French site pages linking to them.
- Readable bilingual (EN/FR) runner pages: the validation and demo-verification routes of `run/js|wasm/` show a localized progress line, a summary line, a case table with colored statuses and diagnostics, and the pretty-printed report folded below. The published `graphiksSuiteReport`/`graphiksDemoReport` strings are compact JSON for the collector, the Validation page's local-run links pass the site's locale, and a raw-JSON display with an explicit diagnostic covers a text-resource failure.
- Extend the browser acid tests to 147 cases (138 mandatory, nine optional) with residual-behaviour coverage: a `StencilOnly` view and its copy aspect, `Depth16Unorm` and packed `RGB10A2Uint` targets read back exactly, anisotropic sampler filter validation, percentage-closer comparison filtering, point and line topologies with the diamond-exit endpoint, a partial 4x multisample mask, view-dimension creation constraints, storage-usage and transient-attachment refusals, negative render-bundle validation, full-range 32-bit draw indices (the data family's first cases), the adapter's full limit space requested and observed field by field, and two present optional features exercised end to end (`TextureCompressionBC` block decode, `Subgroups` sum within the reported bounds). The seven fully covered residuals move from the residual inventory into the catalogue; the partially covered behaviours keep distinct case ids with re-scoped residual texts, and the balance goes from fifteen residuals to eight.

### Fixed

- `webgpu-browser` no longer throws on Kotlin/Wasm when an implementation omits a `GPUSupportedLimits` property; an absent limit is read as zero.
- `webgpu-browser` omits zero-valued maximum limits from a `requiredLimits` record, so a device request never sends a limit the implementation does not expose (older Chromium rejects an unknown `maxImmediateSize` key). The two alignment limits are always sent: zero is an invalid alignment, not an unexposed limit, so a bad request is rejected rather than silently replaced by the default.
- The browser suite runner pins Playwright 1.63.0 / Chromium 153, which implements the contract's four-character `DOMString` texture-view swizzle; `texture.view-swizzle` is executed instead of reported as an environment gap.
- `webgpu-browser` omits an identity `GPUTextureViewDescriptor.swizzle` when creating a view, so a view built from a descriptor no longer sends a swizzle value in the form rejected by Chromium 140's pre-release dictionary encoding.

### Changed

- The suite execution contract passes an `AcidContext` (the borrowed device plus a binding-supplied adapter factory) to `AcidCase.run`, so a case can request its own adapter and device; the individual case functions still take a `GPUDevice`.
- Require contribution pull requests from forks on `feat/`, `fix/`, or `chore/` branches based on the current `master` commit.
- Drop the deprecated macOS x64 and watchOS x64 native targets.
- Rename `webgpu-web` to `webgpu-web-bindings` and move generated descriptors and bindings to the `org.graphiks.webgpu.descriptors` and `org.graphiks.webgpu.bindings` packages.
- Move the type mapping into the documentation site as a dedicated bilingual section (`docs/docs/type-mapping/`) and remove the root `TYPE_MAPPING.md`.
- Suite reports (acid, demo, benchmark) carry the exact commit and version they were built from, and the runner fails closed on a missing or mismatched identity instead of attributing a report to the current checkout; inventory generation links every case entry to its source file on the Validation page.
- Accept automated dependency updates in the PR policy: a pull request authored by `dependabot[bot]` on a `dependabot/` branch passes a reduced bot lane — Conventional Commit subjects with the reserved `deps` scope and the current `master` commit — instead of failing the fork, branch-prefix, template, documentation, and changelog rules; the validator now ships unit tests that run on every pull request.

### Removed

- Remove the deprecated descriptor type aliases (`Size3D`, `ImageCopyTexture`, `ImageCopyBuffer`, `ColorAttachment`, `DepthStencilAttachment`).
- Remove the four never-testable acid residuals (`errors.device-lost`, `immediates.set-immediates`, `features.not-exposed-by-reference-browser`, `async.oom-and-internal-results`) from the residual inventory: they have no portable case in this contract and stay documented as known limits in `docs/acid-coverage.md`.
- Remove the redundant browser-target and public-ABI CI steps; `./gradlew check` already runs those tasks on every platform.
