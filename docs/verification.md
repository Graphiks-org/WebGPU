# Verification

This document records the reference contract, the implemented coverage, and the evidence observed
while building the Graphiks WebGPU Suite foundation increment. It is a report of what was actually
run, not a conformance certificate.

## Reference contract

- API and suite version: `0.1.0-SNAPSHOT`.
- Reference commit and source hashes: [`inventory/baseline.json`](../inventory/baseline.json).
- The inventory is extracted from the seven `commonMain` files of `webgpu-api` (not the JVM ABI
  snapshot alone), so it represents the shared signature surface.

## Inventory

`inventory/symbols.tsv` lists **834 declarations** across 14 families. `inventory/behaviors.json`
describes **54 observable behaviours**. The number of symbols is a measure of surface area only:
it is not a conformance percentage, and a symbol being listed never means it is tested.

| Family | Declarations |
| --- | ---: |
| textures/views/samplers | 220 |
| pipelines/render state | 183 |
| types de données/descripteurs/flags/swizzle | 115 |
| adapter/device/features/limits | 86 |
| rendu/passes/attachments | 39 |
| shaders/compilation | 35 |
| buffers/mapping | 33 |
| bind groups/layouts | 32 |
| erreurs/asynchronisme | 22 |
| transferts buffers/textures | 18 |
| compute | 17 |
| queries/timestamps | 15 |
| queue/commandes | 11 |
| render bundles | 8 |
| **Total** | **834** |

Families with no executable case in this increment: adapter/device/features/limits,
textures/views/samplers, rendu/passes/attachments, pipelines/render state, render bundles,
queries/timestamps. Their behavioural analysis remains to be deepened.

## Executable cases

The browser runner executes eleven foundation cases. The mapping from case to behaviour is in
[`inventory/contract.md`](../inventory/contract.md).

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

- Runner commit: `e8c58f4` (`suiteCommit` recorded in both reports); reference contract commit
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
  temporarily adding an unknown id to `inventory/foundation-case-ids.json`, observing exit code 1,
  and restoring the file.
- The Validation page was served under a `/suite/` prefix and at a 375 px viewport: 54 behaviour
  rows rendered, passed and not-run statuses displayed with text labels, both reports and both run
  distributions loaded, the local launch page published a report, and the page had no horizontal
  overflow. These are manual delivery checks, not HTML-structure tests.

## Maven consumption evidence

- Version: `0.1.0-verification-SNAPSHOT`.
- Published to an isolated Maven repository with
  `./gradlew --no-daemon -Dmaven.repo.local=<isolated> -PreleaseVersion=0.1.0-verification-SNAPSHOT
  :webgpu-api:publishToMavenLocal :webgpu-descriptors:publishToMavenLocal :suite-core:publishToMavenLocal
  :suite-acid-tests:publishToMavenLocal`.
- The standalone [`consumption`](../consumption/build.gradle.kts) build has no `includeBuild` and no
  local substitution; its only `org.graphiks` source is the isolated repository, configured as
  exclusive content. Its resolved tree is
  `org.graphiks:suite-acid-tests → suite-acid-tests-jvm → suite-core → suite-core-jvm → webgpu-api → webgpu-api-jvm`.
- Compiled consumer targets: JVM, JS, Wasm JS and macOS ARM64 (all `BUILD SUCCESSFUL`). This proves
  the published signatures resolve and compile; it is not a GPU execution.
- Linux x64: the artifacts (`suite-acid-tests-linuxx64`, `suite-core-linuxx64`) are published by the
  macOS host, and the external consumer is compiled for Linux x64 by the `consumption` job in
  [`.github/workflows/suite.yml`](../.github/workflows/suite.yml) on `ubuntu-latest`
  (`compileKotlinLinuxX64`). It is not compiled on this macOS host.

## Known limitations

- The pinned Playwright Chromium installer stalled during extraction on this machine. The matching
  `chromium-headless-shell` build was installed directly and marked complete; `tools/package.json`
  still pins Playwright 1.55.1 and CI runs `playwright install --with-deps chromium` normally. The
  local run therefore exercised the pinned revision but a hand-assembled cache layout.
- The full `mkdocs build --strict` (Dokka embed plus `mkdocs-static-i18n`) was not run locally
  because that Python plugin is not installed here. The navigation entry and the relative suite link
  were validated statically; the assembled documentation site is exercised by the Documentation
  workflow in CI.
- Native execution of the shared cases on JVM, Linux x64 or macOS ARM64 is not performed in this
  repository. Those results belong to the consuming binding repositories, which supply their own
  device. Only artifact resolution and compilation are verified here.
- The results above are functional software-backend results. They are not performance measurements
  and do not represent a physical GPU.
