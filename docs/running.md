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
Each delivery records the API version, the reference commit and the source hashes in the generated
`baseline.json` (published at `suite/inventory/baseline.json`).

The shared artifacts `org.graphiks:suite-core`, `org.graphiks:suite-acid-tests`,
`org.graphiks:suite-demos` and `org.graphiks:suite-benchmarks` are published for:

| Target | Built by |
| --- | --- |
| JVM 25 | explicit compilation in the suite CI workflow |
| JS (browser) | browser execution and normal compilation |
| Wasm JS (browser) | browser execution and normal compilation |
| Linux x64 | `:suite-core:compileKotlinLinuxX64`, `:suite-acid-tests:compileKotlinLinuxX64`, `:suite-demos:compileKotlinLinuxX64` and `:suite-benchmarks:compileKotlinLinuxX64` on a compatible host |
| macOS ARM64 | `:suite-core:compileKotlinMacosArm64`, `:suite-acid-tests:compileKotlinMacosArm64`, `:suite-demos:compileKotlinMacosArm64` and `:suite-benchmarks:compileKotlinMacosArm64` on a compatible host |

Other API targets can be added progressively; do not assume support for a target that is not
listed. The browser runner only executes JS and Wasm JS here. Native GPU execution results belong
to the consuming binding repositories; this repository compiles the shared modules and runs the
browser cases.

The module builds declare these targets; this table is not a record of a successful remote
publication. The current suite workflow explicitly compiles JVM and the browser distributions.
The module scripts do not add explicit dependencies from `check` to all five compilation tasks.

## Generated inventory

Browser compilation runs `:suite-acid-tests:generateSuiteInventory`, supplied by the
`org.graphiks.webgpu-suite-inventory` plugin in `build-logic`. It generates `ApiSymbols.kt` and
`FoundationCases.kt` under `suite-acid-tests/build/generated/suite/commonMain/kotlin/`, and the case
manifest and baseline under `suite-acid-tests/build/suite-inventory/`.

`tools/build-inventory.mjs`, invoked by `tools/build-site.mjs`, combines these outputs with the API
sources, `inventory/uncovered-behaviours.json` and the EN/FR resources to produce
`build/site/inventory/`. Edit annotations and authored resources, not generated outputs. See
[adding-a-case.md](adding-a-case.md) for the case layout and localization keys.

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

### Run a subset while developing

`--cases=id1,id2` (or `?cases=id1,id2` on the page) runs only the named acid cases. The selection
must be non-empty and name only known ids; an unknown id fails before the browser starts. A targeted
run writes its own `build/reports/selected-<target>.json` with a `selectedCaseIds` field and never
replaces the full `<target>.json`, so the site still consumes only complete campaigns. Without
`--cases`, every case runs and the full report is written. The collector waits
`cases × 30 s + 60 s`, capped at one hour; a longer budget never turns a stopped case into a success.

A case is `unsupported` only when it declares optional features and the adapter lacks them, and the
report records them in `missingFeatures` using the generated manifest's enum names. The collector
accepts that status for exactly those declared features; every other non-`passed` status, and any
`unsupported` without a matching declaration, fails the run. The summary counts passed and
unsupported separately.

## Run the particle demo

The `?demo=particles` route runs a portable particle scene. `suite-demos` owns the GPU resources and
the runner supplies the device, the texture format and the render target. Two routes are available:

- `?demo=particles&lang=en|fr` — the interactive demo. The count select offers 256, 1024, 4096,
  16384 and 65536 particles, filtered by `maxParticleCount(device.limits)`; the default is 4096 and
  reset reproduces the same layout for a given count (fixed seed `1u`).
- `?demo=particles&verify=1` — the two GPU checks, published in `globalThis.graphiksDemoReport`.

Build and run the distributions as for the suite, then add `--demo-check`:

```sh
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
```

`--demo-check` opens `?demo=particles&verify=1`, requires both demo ids to pass and writes
`build/reports/demos-<target>.json`, separate from the acid-test reports. The gallery page at
`site/demos/` presents the demo with local launches and links to the scene sources. The demo is an
illustration, not a benchmark: this increment records no frame rate presented as a measurement, no
GPU time and no ranking.

## Run the benchmarks

The `?benchmark=foundations&profile=standard|ci` route runs the two portable workloads — writing
buffers and encoding/submitting compute — and publishes `globalThis.graphiksBenchmarkReport`. The
route accepts `autorun=1` for the collector; the page shows a Start button otherwise. The protocol,
the ten scenarios and the temporal boundaries are documented in [benchmarks.md](benchmarks.md).

Build and run the distributions as for the suite, then add `--benchmark`:

```sh
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
```

`--benchmark` opens `?benchmark=foundations&profile=<profile>&autorun=1`, requires all ten scenarios
`completed` with `outputVerified = true` and the profile's sample count, and writes
`build/reports/benchmarks-<target>.json`, separate from the acid-test and demo reports. It never fails
on the value of a duration. `--profile=ci` (default in the tool) keeps 5 samples per scenario;
`--profile=standard` keeps 30. `--backend=swiftshader` (default) forces the software backend with
explicit flags; `--backend=default` lets Chromium choose and records `default`, not `hardware`. Run
JS and Wasm sequentially: never measure both targets in parallel on the same machine. The Benchmarks
page at `site/benchmarks/` presents the two reports separately, with the raw samples downloadable.

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

A native binding can reuse the demo scene the same way. `ParticleScene` is portable and takes the
device, the target format and the initial particle data:

```kotlin
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles

val scene = ParticleScene.create(device, format, initialParticles(4096))
try {
    val encoder = device.createCommandEncoder()
    try {
        scene.encodeFrame(encoder, targetView, width, height, deltaSeconds)
        val commands = encoder.finish()
        try { device.queue.submit(listOf(commands)) } finally { commands.close() }
    } finally {
        encoder.close()
    }
} finally {
    scene.close()
}
```

One `encodeFrame` call is one submission: the application owns the encoder, the target view and the
device and closes them, while the scene owns `particleBuffer`, the parameter buffer, the pipelines
and the bind groups. `particleBuffer` is exposed for readback-style composition but must not be
closed by the consumer. `deltaSeconds` must stay within `0f..0.05f`; a zero delta renders the
current state without a compute pass, so a paused runner can repaint on reset or resize.

A native binding can measure the portable workloads the same way. The functions take the device the
binding owns, close the resources they allocate and never close the device:

```kotlin
import org.graphiks.webgpu.suite.benchmarks.BenchmarkProfile
import org.graphiks.webgpu.suite.benchmarks.benchmarkWriteBuffer

val result = benchmarkWriteBuffer(device, 65536, 16, BenchmarkProfile.Standard)
```

The binding keeps its own error scopes, timeouts, uncaptured errors and environment metadata, as the
browser runner does. See [benchmarks.md](benchmarks.md) for the protocol and the interpretation
limits.

## Statuses

A report uses four statuses and never conflates them:

- **passed**: the case ran and its expectations held.
- **failed**: the case ran and an expectation failed, or execution threw or timed out.
- **unsupported**: a required and declared optional feature is missing, with a reason.
- **not-run**: no usable execution is available.

Coverage (does a case exist?) and execution (what happened in one environment?) are separate. A
behaviour with no case is reported as *to be tested*, never as passing.
