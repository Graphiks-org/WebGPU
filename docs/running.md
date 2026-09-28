# Running the Graphiks WebGPU Suite

The suite validates the public Graphiks WebGPU contract with portable acid tests that execute
against a real browser WebGPU implementation. `suite-core` defines the execution contract,
`suite-acid-tests` holds the cases, and `suite-browser` is the web runner. The same content modules
are published as artifacts so native bindings can run them on their own targets.

## Requirements

- JDK 25 and the repository Gradle wrapper for the suite modules and distributions.
- Node.js 22 for the Chromium runner. Playwright is pinned in `tools/package.json`; run
  `npm exec --prefix tools -- playwright install chromium` to install the browser it expects.

## Version and published matrix

The suite and the API share the repository version (`releaseVersion`, default `0.1.0-SNAPSHOT`).
Each delivery documents the API version it is built against in
[`inventory/baseline.json`](../inventory/baseline.json).

The two shared artifacts, `org.graphiks:suite-core` and `org.graphiks:suite-acid-tests`, are
published for:

| Target | Built by |
| --- | --- |
| JVM 25 | normal `check` compilation (local and CI) |
| JS (browser) | browser execution and normal compilation |
| Wasm JS (browser) | browser execution and normal compilation |
| Linux x64 | normal `check` compilation on the Linux CI host |
| macOS ARM64 | normal `check` compilation on the macOS CI host |

Other API targets can be added progressively; do not assume support for a target that is not
listed. The browser runner only executes JS and Wasm JS here. Native GPU execution results belong
to the consuming binding repositories; this repository compiles the shared modules and runs the
browser cases.

## Run the browser suite

Build the distributions and run each target. The runner writes `build/reports/<target>.json` and
exits non-zero when the run is incomplete or failed; it never turns a missing adapter or a timeout
into a success.

```sh
npm install --prefix tools
npm exec --prefix tools -- playwright install chromium

./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
node tools/build-site.mjs
```

`tools/build-site.mjs` assembles `build/site/`, including the two reports and the two runnable
distributions. It fails when a report or the inventory is missing, so an absent report is never
published as a success. The Validation page shows JS and Wasm results separately and offers local
launch links that do not modify the published reports.

## Consume the artifacts from a binding

A native binding depends on the published artifact and supplies the device it owns:

```kotlin
dependencies {
    implementation("org.graphiks:suite-acid-tests:<version>")
}
```

```kotlin
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.suite.acid.foundationCases

suspend fun validateSuppliedDevice(device: GPUDevice) {
    for (case in foundationCases()) {
        check(case.requiredFeatures.all { it in device.features })
        case.run(device)
    }
}
```

The runner decides its own isolation and manages the device lifecycle; the browser runner creates a
fresh adapter and device per case. A case closes the resources it creates and never destroys
resources supplied by the runner.

## Statuses

A report uses four statuses and never conflates them:

- **passed**: the case ran and its expectations held.
- **failed**: the case ran and an expectation failed, or execution threw or timed out.
- **unsupported**: a required and declared optional feature is missing, with a reason.
- **not-run**: no usable execution is available.

Coverage (does a case exist?) and execution (what happened in one environment?) are separate. A
behaviour with no case is reported as *to be tested*, never as passing.
