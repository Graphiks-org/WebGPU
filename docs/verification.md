# Verification

This document records the reference contract, the implemented coverage, and the evidence observed
while building the Graphiks WebGPU Suite increments. It is a report of what was actually run, not a
conformance certificate.

Documentation review, 2026-09-28: the implementation is merged as `b4d750a` (PR #120).
The execution evidence below was recorded during implementation, before that merge; it is not a
fresh run of the merge commit. This review checked source configuration and authored inventory
resources, without rerunning GPU tests or confirming remote publication.

## Acid test expansion progress (2026-09-28)

The plan `docs/superpowers/plans/2026-09-28-webgpu-suite-acid-expansion.md` extends the eleven cases
to eighty-three. This section records the increments actually delivered and is updated as each lot
passes on JS and Wasm. It lists no result that was not observed.

- **Task 1 — targeted acid selection and declared optional features** (runner only; no new case).
  `--cases=id1,id2` / `?cases=id1,id2` runs a non-empty subset of known ids; a targeted run writes
  `build/reports/selected-<target>.json` with a `selectedCaseIds` field and never replaces
  `<target>.json`; `CaseResult.missingFeatures` records declared-but-absent features and the acid
  collector accepts `unsupported` only for them, counting passed and unsupported separately.
  Evidence on the eleven-case catalogue: `--cases=does.not-exist`, an empty selection and
  `--cases` with `--benchmark` each exit 2 before a browser launches; `--cases=transfers.write-offsets`
  (JS) and `--cases=buffers.mapped-at-creation,compute.auto-layout-constants` (Wasm) each wrote their
  selected report while `js.json` stayed byte-identical; the full JS run printed "reported 11 passed
  and 0 unsupported of 11 acid cases"; `tools/build-site.mjs` still consumes only the full reports and
  no `selected-*` file reached `build/site/`. No case declares an optional feature yet, so the
  `unsupported` present→executed path is deferred to Lot I.
- **Task 2 — readback and render helpers** (`Readback.kt`, `RenderSupport.kt`). `readBufferBytes`,
  `readRgba8`, `assertPixel`, the fullscreen-triangle shader and `createColorTarget` are `internal`
  helpers in `suite.acid`. They are committed with Lot A, their first consumer; `readBufferBytes` is
  validated by reading real GPU data in the Lot A cases, while `readRgba8`, `assertPixel` and the
  render helpers compile but are first exercised by their consuming lots (B and E).
- **Lot A — buffers, bindings and compute commands** (8 cases: `buffers.zero-initialized`,
  `buffers.map-write-roundtrip`, `transfers.clear-buffer-range`, `transfers.copy-remaining`,
  `bindings.buffer-range`, `bindings.dynamic-uniform-offsets`, `compute.indirect-dispatch`,
  `compute.ordered-passes`). Result: **8/8 passed on JS and 8/8 on Wasm** in targeted runs; the full
  catalogue is **19/19 passed on JS and 19/19 on Wasm**. The bindings read the device's
  `minUniformBufferOffsetAlignment` instead of assuming 256. Regression check: changing `clearBuffer`'s
  offset from 8 to 4 without touching the expectation fails `transfers.clear-buffer-range`
  (`Array elements differ at index 1 ... actual <[287454020, 0, 0, 0, ...]>`); restoring returns 8/8 and
  19/19. Environment unchanged from the reference run: Chromium 140.0.7339.186 headless with
  `--enable-unsafe-webgpu --enable-unsafe-swiftshader --use-angle=swiftshader` (software backend), JS
  and Wasm run sequentially. Inventory: 49 behaviours, 19 with cases; `transfers.clear-buffer` moved
  from the uncovered list into `transfers.clear-buffer-range`.

## Reference contract

- API and suite version: `0.1.0-SNAPSHOT`.
- Reference commit and source hashes: generated at build time (published at
  `suite/inventory/baseline.json`), from the commit and the SHA-256 of the seven API source files.
- The inventory is extracted from the seven `commonMain` files of `webgpu-api` (not the JVM ABI
  snapshot alone), so it represents the shared signature surface.

## Inventory

The inventory is **generated at build time** from the case annotations and the API sources; it is not
versioned. The generated `symbols.tsv` lists **884 declarations** across 14 families, and the
localized behaviour files describe **42 behaviours** (11 covered by a case, 31 to be tested). The
number of symbols is a measure of surface area only: it is not a conformance percentage, and a
symbol being listed never means it is tested.

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

Families with no executable case in this increment: adapter/device/features/limits,
textures/views/samplers, rendu/passes/attachments, pipelines/render state, render bundles,
queries/timestamps. Their behavioural analysis remains to be deepened.

## Executable cases

The browser runner executes eleven foundation cases. The mapping from case to behaviour is in the
generated `contract.md`, published at `suite/inventory/contract.md`.

| Case | Exercised on |
| --- | --- |
| `buffers.mapped-at-creation` | JS, Wasm |
| `transfers.copy-offsets` | JS, Wasm |
| `transfers.write-offsets` | JS, Wasm |
| `transfers.write-remaining` | JS, Wasm |
| `buffers.partial-map-remap` | JS, Wasm |
| `compute.auto-layout-constants` | JS, Wasm |
| `compute.explicit-layout-entrypoint` | JS, Wasm |
| `errors.empty-scope` | JS, Wasm |
| `errors.invalid-buffer-usage` | JS, Wasm |
| `errors.map-alignment` | JS, Wasm |
| `buffers.map-destroyed` | JS, Wasm |

## Browser execution evidence

- Execution commit: the `suiteCommit` recorded in both browser reports; reference contract commit
  `918de19`.
- Commands:

  ```sh
  ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
  node tools/build-site.mjs
  ```

- Result: **11/11 passed on JS and 11/11 passed on Wasm**, no page errors, no fatal error. The
  runner received `Chromium 140.0.7339.186` (Playwright 1.55.1, browser build revision 1193) on
  `darwin`, launched headless with `--enable-unsafe-webgpu --enable-unsafe-swiftshader
  --use-angle=swiftshader`. The reported `requestedBackend` is `swiftshader`, which describes the
  launch flags rather than a detected physical adapter. The adapter description reported by the
  cases was empty in this environment; it is recorded as observed and not interpreted.
- Failure path: with WebGPU disabled the same runner reported every case as `failed` with
  `No WebGPU adapter is available.` — never `passed` and never `unsupported`. The completeness gate
  exits non-zero when a case id is missing, duplicated or not passed; this was checked by
  temporarily adding an unknown id to
  `suite-acid-tests/build/suite-inventory/foundation-case-ids.json`, observing exit code 1,
  and restoring the file.
- An earlier Validation page was served under a `/suite/` prefix and at a 375 px viewport: 54 behaviour
  rows rendered, passed and not-run statuses displayed with text labels, both reports and both run
  distributions loaded, the local launch page published a report, and the page had no horizontal
  overflow. These are manual delivery checks, not HTML-structure tests.
  The 54-row observation predates the current 42-behaviour resources and should not be used as
  the expected row count for the merged inventory.

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

## Particle demo evidence (2026-09-28)

- Implementation commit: `5362712` (`suite-demos` scene and the browser demo); the fix below is
  `f1badc5`; the gallery and publication description are added by the commit that records this
  section.
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
- Acid tests: **11/11 passed on JS and 11/11 passed on Wasm**, unchanged by this increment.
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
  source links from the published demo report's `suiteCommit`. With the reports removed, the source
  links fall back to the source directory on `master` and say so. The page has no horizontal overflow
  at 375 px, and the launch link opens the running demo.
- Fix exercised by the demo: reading `device.limits` on Kotlin/Wasm used to throw when the
  implementation omits a limit property (Chromium 140 omits `maxImmediateSize`). The `webgpu-browser`
  mapper now treats an absent limit as zero (`fix(web): tolerate absent device limits`), with no
  change to present limits.
- Compilation versus native execution: `suite-demos` compiles for JVM 25, JS, Wasm JS, Linux x64 and
  macOS ARM64 through its normal tasks. No native GPU execution is performed in this repository; that
  belongs to the consuming binding repositories, as for the acid tests.

## Benchmark evidence (2026-09-28)

- Implementation commits: `af212a4` (portable transfer workload), `e2311db` (compute encoding and
  submission), `779e318` (browser runner and protocol), `5131fcf` (collector and Benchmarks page).
  The CI, publication and documentation integration is recorded by the commit that adds this section.
- Commands (local shell runs were prefixed by `rtk proxy`, a local output filter; the runner command
  itself is unchanged):

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
  the collector exited non-zero, and restoring the shader returned **10/10 on both targets**. The
  mutation was not committed.
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
  10/10. The temporary change was not committed.
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

## Known limitations

- The pinned Playwright Chromium installer stalled during extraction on this machine. The matching
  `chromium-headless-shell` build was installed directly and marked complete; `tools/package.json`
  still pins Playwright 1.55.1 and CI runs `playwright install --with-deps chromium` normally. The
  local run therefore exercised the pinned revision but a hand-assembled cache layout.
- The full `mkdocs build --strict` (Dokka embed plus `mkdocs-static-i18n`) was not run locally
  because that Python plugin is not installed here. The navigation entry and the relative suite link
  were validated statically; the assembled documentation site is exercised by the Documentation
  workflow in CI.
- Native GPU execution of the shared cases on JVM, Linux x64 or macOS ARM64 is not performed in this
  repository. Those results belong to the consuming binding repositories, which supply their own
  device. This repository compiles the shared modules and runs the browser cases.
- The results above are functional software-backend results. They are not performance measurements
  and do not represent a physical GPU.
