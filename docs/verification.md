# Verification

This document records the reference contract, the implemented coverage, and the evidence observed
while building the Graphiks WebGPU Suite increments. It is a report of what was actually run, not a
conformance certificate.

Documentation review, 2026-09-28: the implementation is merged as `b4d750a` (PR #120).
The execution evidence below was recorded during implementation, before that merge; it is not a
fresh run of the merge commit. This review checked source configuration and authored inventory
resources, without rerunning GPU tests or confirming remote publication.

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
  `org.graphiks:suite-core`, `org.graphiks:suite-acid-tests` and `org.graphiks:suite-demos` alongside
  the four base modules under the shared `releaseVersion`.
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
