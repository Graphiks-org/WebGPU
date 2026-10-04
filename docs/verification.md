# Verification

This document records the reference contract, the generated inventory and the verification evidence
of the Graphiks WebGPU Suite: what is verified, how it is verified, and the observed results. It is
not a conformance certificate.

## Reference contract

- API and suite version: `0.1.0-SNAPSHOT`.
- Reference commit and source hashes: generated at build time (published at
  `suite/inventory/baseline.json`), from the commit and the SHA-256 of the seven API source files.
- The inventory is extracted from the seven `commonMain` files of `webgpu-api` (not the JVM ABI
  snapshot alone), so it represents the shared signature surface.

## Inventory

The inventory is **generated at build time** from the case annotations and the API sources; it is not
versioned. The generated `symbols.tsv` lists **884 declarations** across 14 families, and the
localized behaviour files describe **147 behaviours per locale**: **132 executable cases** and
**15 residuals**. The number of symbols is a measure of surface area only: it is not a conformance
percentage, and a symbol being listed never means it is tested.

| Family | Declarations |
| --- | ---: |
| textures/views/samplers | 223 |
| pipelines/render state | 185 |
| types de données/descripteurs/flags/swizzle | 115 |
| adapter/device/features/limits | 88 |
| rendu/passes/attachments | 55 |
| shaders/compilation | 36 |
| bind groups/layouts | 35 |
| buffers/mapping | 34 |
| compute | 24 |
| erreurs/asynchronisme | 22 |
| render bundles | 21 |
| transferts buffers/textures | 18 |
| queue/commandes | 16 |
| queries/timestamps | 12 |
| **Total** | **884** |

The coverage balance, the residuals and the known limits of the evidence are in
[acid-coverage.md](acid-coverage.md).

## Runner semantics

- Without an adapter the runner reports every case as `failed` with `No WebGPU adapter is
  available.` — never `passed` and never `unsupported`.
- The collector exits non-zero when a case id is missing, duplicated or not passed.
- Case isolation holds: `errors.uncaptured-error` on its dedicated device followed by
  `errors.empty-scope` in the same campaign each reported their own result, with no error leaking
  into the next case.

## Acid campaign

- Commands:

  ```sh
  ./gradlew :suite-acid-tests:generateSuiteInventory \
    :suite-core:compileKotlinJvm :suite-acid-tests:compileKotlinJvm :suite-benchmarks:compileKotlinJvm \
    :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/build-site.mjs
  ./gradlew check
  ```

- Environment: `Chromium 153.0.8010.12` (Playwright 1.63.0) on `darwin`, headless with
  `--enable-unsafe-webgpu --enable-unsafe-swiftshader --use-angle=swiftshader`. These are functional
  software-backend results, not physical-GPU results.
- Result: **132 cases (125 mandatory, 7 optional)**.

  | Target | Passed | Unsupported | Failed | Total |
  | --- | ---: | ---: | ---: | ---: |
  | JS | 131 | 1 | 0 | 132 |
  | Wasm JS | 131 | 1 | 0 | 132 |

  All **125 mandatory cases pass** on both targets. The only non-passing case is optional
  `compute.shader-f16`, `unsupported` because the environment lacks `ShaderF16`.
- Demo checks: **2/2 on both targets**. Benchmark scenarios: **10/10 on both targets** (`ci`
  profile, 5 retained samples per scenario).
- Report attribution: each envelope's `buildCommit` equals `git rev-parse HEAD` at build time and
  matches the inventory `baseline.commit`, and `buildVersion` matches `baseline.suiteVersion`; the
  collector fails closed on a missing or mismatched identity.
- Site: assembled with **884 symbols and 147 behaviours per locale**; each of the 132 case entries
  links to its source file.
- Unit evidence: `./gradlew check` passes.
- Observed 2026-10-02.

## Oracle mutation evidence

Each row below removes or breaks the behaviour an oracle must observe; the case then reports
`failed`. No mutation is part of the catalogue.

| Mutation | Case(s) that fail |
| --- | --- |
| `readRgba8`: the `copyTextureToBuffer` call removed | `render.clear-only` (observes the `0xa5` prefill instead of 255) |
| `requiredLimits = null` in the device request | `device.reject-excess-limit` |
| Two command buffers submitted in reverse order | `command.ordered-command-buffers` |
| Slope-clamp bias set to 0 | `depth.bias-slope-clamp` |
| Mapper drops the forwarded binding `size` (`mapper/BindGroupDescriptor.kt`) | `bindings.storage-range-length` |
| `minBindingSize` lowered so the 12-byte probe binds | `errors.min-binding-size` |
| `setViewport` replaced by `setScissorRect(4,4,8,8)` | `render.viewport` |
| Bundle draws one triangle (`draw(3)`) | `bundles.draw`, `bundles.reuse`, `command.debug-markers` |
| Out-of-range timestamp descriptor omitted in the probe | `errors.compute-timestamp-indices` |
| Out-of-range timestamp descriptor omitted in the probe | `errors.render-timestamp-indices` |
| Pass state rebound before the post-bundle draw (state-retention surrogate) | `errors.bundle-post-execute-state` |
| Depth readback mapping shortened to 520 bytes | `transfers.texture-copy-aspect` |

Accepted survivors — mutations the catalogue deliberately does not catch, with the narrowed claim
that covers them: omitting `size` only in `bindings.buffer-range`, dropping `timestampWrites` only
from the positive timestamp cases (covered by the deterministic index cases), and an immediate
`onSubmittedWorkDone`. The timestamp observability limit is documented in
[acid-coverage.md](acid-coverage.md).

## Demo evidence (2026-09-28)

- Commands:

  ```sh
  ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
  node tools/build-site.mjs
  ```

- Demo results: **2/2 passed on JS and 2/2 passed on Wasm**.
  `particles.compute-render-readback` reads back the computed position `x=0.02` with the velocity
  unchanged, and reads the rendered disk pixels (`r>40`, `g>150`, `b>200`, `a=255`) over a black
  background, from the same submission as the compute pass.
  `particles.bounds-pause-reset` exercises 65 particles (a partial last workgroup), the `±0.95`
  bounce clamps, an unchanged zero-delta frame and a reset back to the initial data. The demo checks
  run on their own adapter and device and publish `globalThis.graphiksDemoReport`; the reports are
  `build/reports/demos-js.json` and `demos-wasm.json`, separate from the acid-test reports.
- The checks detect a real regression: forcing the frame delta to zero made both ids fail with
  `Computed x was 0.0` and `Bounced x ... expected 0.95 but was 0.94`; reverting restored 2/2 on both
  targets.
- `device.limits` reads a limit property the implementation omits as zero; the demo runs the
  readback path that depends on it.
- Environment: `Chromium 140.0.7339.186` on `darwin`, launched headless with
  `--enable-unsafe-webgpu --enable-unsafe-swiftshader --use-angle=swiftshader`. The requested backend
  is `swiftshader`, which describes the launch flags; these are software-backend functional results,
  not physical-GPU results and not performance measurements.
- Manual interactive checks on both targets (headless Chromium, screenshots): `?demo=particles&lang=fr`
  renders colored particles (about 29k saturated pixels in the canvas region); pause toggles
  `Pause`/`Reprendre`; reset and a count change (4096→1024) keep the page healthy; a 1280×720 → 480×900
  viewport resize changes the canvas from 768×480 to 448×280 (same ratio); without the WebGPU flags
  the page shows the localized `unavailable` diagnostic; `?demo=unknown` shows an explicit error; the
  Validation link navigates back and hides the demo. The tab-hidden reset of the frame clock was
  inspected in code; it is not automatable in this headless setup. These are manual checks, not
  HTML-structure tests.
- Gallery: served under a `/suite/` prefix, the Demos page loads the localized card from the demo
  resource shared with the runner, links to `../run/js|wasm/?demo=particles&lang=…`, and builds the
  source links from the published demo report's `buildCommit`. With the reports removed, the source
  links fall back to the source directory on `master` and say so. The page has no horizontal overflow
  at 375 px, and the launch link opens the running demo.
- Compilation versus native execution: `suite-demos` compiles for JVM 25, JS, Wasm JS, Linux x64 and
  macOS ARM64 through its normal tasks. No native GPU execution is performed in this repository; that
  belongs to the consuming binding repositories, as for the acid tests.

## Readable report evidence

- The validation route (`run/js|wasm/`) and the demo verification route render a localized (EN/FR,
  `?lang=` first, then the stored locale) summary line, a case table with coloured statuses and
  diagnostics, and the raw JSON pretty-printed in a folded `details` block. The published
  `graphiksSuiteReport` and `graphiksDemoReport` strings are compact JSON for the collector; the
  DOM presentation is a separate rendering of the same report.
- Commands:

  ```sh
  ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
  ```

- Collector: **131 passed + 1 unsupported (optional `compute.shader-f16`) of 132 on JS and on
  Wasm**, and **2/2 demo cases on both targets**.
- Manual DOM checks in headless Chromium (Playwright, swiftshader): EN and FR render the summary
  (`2 passed · 0 failed · 0 unsupported — 2 cases` / `2 réussis · 0 échoués · 0 non pris en charge —
  2 cas`), the case rows show localized statuses, the folded JSON is pretty-printed, dark mode uses
  its dark palette, the Wasm distribution renders the same page as the JS one, and the demo route
  shows its own heading with the validation section hidden. No page error is logged. With the text
  resources blocked, the page shows the raw JSON in `#result` with an explicit diagnostic in the
  status line.

## Benchmark evidence (2026-09-28)

- Commands:

  ```sh
  ./gradlew :suite-benchmarks:compileKotlinJvm :suite-benchmarks:compileKotlinJs :suite-benchmarks:compileKotlinWasmJs
  ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=standard --backend=swiftshader
  node tools/build-site.mjs
  ```

- Result: **10/10 scenarios `completed` and GPU-verified on JS and 10/10 on Wasm**, for the `ci`
  profile (5 retained samples per scenario) and, on JS, for the `standard` profile (30 samples). No
  page errors and no fatal error. The protocol fields were `schemaVersion=1`,
  `protocol=foundations-v1`, `clock=kotlin.time.TimeSource.Monotonic`. The ten ids, the profile and the
  sample count were required by the collector; a missing, duplicated, non-`completed`, unverified or
  wrong-count scenario makes it exit non-zero. No threshold is applied to a duration.
- Control readback: each scenario reads its whole result buffer back before the warm-ups and after the
  last sample, against a value computed from the index. `transfer.write-buffer` verifies the word
  `i xor (0x9e3779b9 + j)` for the last write of a batch; `compute.encode-submit` verifies `i * 3 + 7`.
- The check detects a real GPU regression: replacing `+ 7u` with `+ 8u` in the compute shader made all
  four compute scenarios `failed` with a readback mismatch (the six transfer scenarios still passed),
  the collector exited non-zero, and restoring the shader returned **10/10 on both targets**.
- Clock and environment limitations observed: under the requested `swiftshader` backend, most
  `cpuIssueMs` values are exactly `0.000` — below the practical resolution of the clock — and are kept
  and counted as `zeroCpuSamples` rather than filtered or turned into a rate. `completionMs` resolves
  the compute wait (about 2.6 ms for one dispatch over 65 536 elements, about 17–19 ms for sixteen,
  and about 0.9–1.4 ms for sixteen 1 MiB writes). The reported adapter description was empty and is
  recorded as `null`/unknown; `isFallbackAdapter` was `true`. These are software-backend functional
  observations, not physical-GPU performances.
- Timeout classification: with the campaign budget temporarily reduced to one second, the in-flight
  scenario was reported as `failed` with `The campaign timed out after 16 minutes.` rather than the
  90-second scenario-timeout or a tab-hidden cancellation diagnostic; restoring the budget returned
  10/10.
- Collector options: `--profile=ci|standard` (default `ci` in the tool) and
  `--backend=swiftshader|default` (default `swiftshader`). `--backend=default` drops the SwiftShader
  flags and records `default`, never `hardware`. JS and Wasm were run sequentially, never in parallel.
- Site inspection: the assembled Benchmarks page was served under a local prefix and checked at a
  390 px mobile viewport in EN and FR, with the `ci` and `standard` profiles loaded side by side. It
  shows size, batch, warm-ups, retained/planned samples, the GPU check, and the median and min/max of
  both durations; p95 appears only from 20 samples. A displayed median and p95 were compared by hand
  with the raw samples of a real campaign and matched (`median 0.9 [0.8..11.4]`, `p95 1.1` for the
  sixteen 1 MiB writes). With a report removed the page shows "No published measurements: not run.",
  never a series of zeros. These are manual delivery checks, not HTML-structure or statistical tests.
- Cancellation and refusal, observed on both targets through the local page: cancelling a campaign
  marked the in-flight scenario `interrupted` and the rest `not-run`, kept the previous displayed
  result and its note, and published the incomplete report for tooling. Without WebGPU
  (`navigator.gpu` removed) the page refused to start with a visible message and published no report.
- Compilation: `suite-benchmarks` compiles for JVM 25, JS and Wasm JS through its normal tasks; its
  Linux x64 and macOS ARM64 targets are declared and publish with the same convention as the other
  suite contents. No native GPU execution and no Maven consumption test are performed here.

## Compilation and publication evidence

- Implementation notes record compilation for the five announced targets through their normal
  Gradle tasks (`compileKotlinJvm`, `compileKotlinJs`, `compileKotlinWasmJs`, `compileKotlinLinuxX64`,
  `compileKotlinMacosArm64`), including local Linux x64 cross-compilation on macOS ARM64.
  The merged module scripts declare these targets but do not explicitly wire `check` to all five
  compilation tasks. The suite CI explicitly compiles JVM and browser distributions; the Tests
  matrix invokes the general `check` task. That configuration alone does not establish a fresh
  successful compilation of every native target.
- The artifacts use the repository's existing publication convention: `buildSrc` supplies the POM,
  licence, sources and signature, and `.github/workflows/publish.yml` publishes
  `org.graphiks:suite-core`, `org.graphiks:suite-acid-tests`, `org.graphiks:suite-demos` and
  `org.graphiks:suite-benchmarks` alongside the four base modules under the shared `releaseVersion`.
- There is no dedicated consumer project, isolated Maven repository or publication/consumption test:
  the amended plan and spec exclude such an architecture test. Artifact resolution by external
  bindings is exercised in their own repositories, not here.

## Known limitations

- The pinned Playwright Chromium installer can stall during extraction on some machines; when that
  happens the matching `chromium-headless-shell` build is installed directly and marked complete.
  `tools/package.json` pins Playwright 1.63.0 and CI runs `playwright install --with-deps chromium`
  normally. A hand-assembled cache in that situation still exercises the pinned revision.
- The full `mkdocs build --strict` (Dokka embed plus `mkdocs-static-i18n`) was not run locally
  because that Python plugin is not installed here. The navigation entry and the relative suite link
  were validated statically; the assembled documentation site is exercised by the Documentation
  workflow in CI.
- Native GPU execution of the shared cases on JVM, Linux x64 or macOS ARM64 is not performed in this
  repository. Those results belong to the consuming binding repositories, which supply their own
  device. This repository compiles the shared modules and runs the browser cases.
- The results above are functional software-backend results. They are not performance measurements
  and do not represent a physical GPU.
- The ArrayBuffer bounds work adds correctness tests only: the common, JVM and Native suites in
  `webgpu-api`, and the Android instrumented checks in `arraybuffer-android-instrumentation`. Its duration
  measurements are informative and reported separately, never as a pass/fail threshold; see
  [ArrayBuffer CPU performance](arraybuffer-performance.md) and
  [ArrayBuffer bounds and capacities](arraybuffer-bounds.md).
