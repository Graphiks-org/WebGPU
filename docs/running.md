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
[adding-a-case.md](adding-a-case.md) for the case layout and localization keys, and
[acid-coverage.md](acid-coverage.md) for the coverage balance, the optional-feature status and the
remaining contract gaps.

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

`--demo-check` opens both `?demo=particles&verify=1` and
`?demo=reaction-diffusion&verify=1` in fresh pages, requires all five ids to pass, and writes
one aggregated `build/reports/demos-<target>.json`, separate from the acid-test reports.
Missing routes, invalid JSON, mismatched build identities, missing/duplicate/unexpected ids,
failed cases and page errors all make the collector exit non-zero; partial real results are
retained as failures, never fabricated successes. The gallery at `site/demos/` presents both
demos with local launches and scene source links. Demos are illustrations, not benchmarks:
no GPU time, frame-rate measurement or ranking is claimed.

## Run the reaction-diffusion demo

- `?demo=reaction-diffusion&lang=en|fr` — interactive Gray–Scott simulation on a fixed 256 × 256
  grid. Coral, Labyrinth and Spots select feed/kill parameters and reset the same nine seed squares.
  Mouse or touch paints a six-cell-radius disk of B, with periodic boundaries. Painting also works
  while paused. Only one pointer and its latest position per frame are consumed; strokes are not
  interpolated.
- `?demo=reaction-diffusion&verify=1` — three GPU checks, published in `globalThis.graphiksDemoReport`.

Speed selects 1–16 fixed simulation steps **per frame**, default 8, not physical seconds.
Pause freezes the reaction; One step advances exactly one iteration and stays paused. Reset
restores the state but keeps the current parameters and palette. Feed and kill range from 0 to 0.1.
Ocean, Ember and Grayscale change only the view; Display A/B shows each concentration separately.
The optional learning panel explains diffusion, reaction, texture ping-pong and the additional
brush pass, and displays the commented WGSL actually compiled by the scene.

Two RGBA32Float textures hold `[A, B, 0, 1]`. The compute passes read one and write the other,
then swap. Reading uses `textureLoad` without optional float filtering features. The render pass
maps the resulting B concentration to a palette. Device loss stops animation, disables GPU controls
and offers reload; unavailable WebGPU is a visible error, not a CPU fallback.

The combined demo report requires these five ids:

```text
particles.compute-render-readback
particles.bounds-pause-reset
reaction-diffusion.compute-render-readback
reaction-diffusion.pause-step-reset
reaction-diffusion.brush-boundaries
```

Run the CPU/control/oracle and browser regressions with:

```sh
./gradlew :suite-demos:jvmTest :suite-browser:jsBrowserTest :suite-browser:wasmJsBrowserTest
node --test tools/demo-reports.test.mjs tools/demos-gallery.browser.test.mjs
node --test tools/reaction-diffusion.browser.test.mjs
GRAPHIKS_DISTRIBUTION=suite-browser/build/dist/wasmJs/productionExecutable node --test tools/reaction-diffusion.browser.test.mjs
```

The last two commands use the distributions built above and software WebGPU flags. To retain
canvas captures after at least 2000 GPU steps of each preset, set `GRAPHIKS_CAPTURE_DIR` to a local
output directory. Screenshots are exploratory evidence, not exact cross-backend reference images.

A native binding can use the portable scene without this DOM runner:

```kotlin
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDiffusionScene
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset

// device, format and targetView are supplied and owned by the binding.
ReactionDiffusionScene.create(device, format).use { scene ->
    device.createCommandEncoder().use { encoder ->
        scene.encodeFrame(encoder, targetView, width, height, 8, ReactionPreset.Coral.parameters)
        encoder.finish().use { device.queue.submit(listOf(it)) }
    }
}
```

Submit once after each `encodeFrame`, before encoding another frame with different uniform values.
Keep the scene alive across animation frames in a real application. `reset()` writes both state
textures; `encodeStateCopy(encoder, buffer)` copies the current state into a borrowed CopyDst buffer
of at least 1,048,576 bytes (4096 bytes per row). The scene closes its resources, never the supplied
device. This repository runs browser GPU checks; native compilation is not native GPU execution.

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

A native binding depends on the published artifact and supplies the device it owns. `AcidCase.run`
takes an `AcidContext`: the borrowed device plus an adapter factory the binding provides. That
factory must return a **fresh** adapter on every call and must not be a stub, because context cases
(`adapter.request-and-capabilities`, `device.required-limits`, `device.reject-excess-limit`,
`errors.uncaptured-error`) request their own adapter and device from it:

```kotlin
dependencies {
    implementation("org.graphiks:suite-acid-tests:<version>")
}
```

```kotlin
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPURequestAdapterOptions
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.acid.foundationCases

// Supplied by the binding; each call must return a fresh adapter.
suspend fun validateSuppliedDevice(
    device: GPUDevice,
    requestAdapter: suspend (GPURequestAdapterOptions?) -> Result<GPUAdapter>,
) {
    val context = AcidContext(device = device, requestAdapter = requestAdapter)
    for (case in foundationCases()) {
        check(case.requiredFeatures.all { it in device.features })
        case.run(context)
    }
}
```

The individual case functions still take a `GPUDevice` and can be called directly, which is
convenient for a binding that only wants the device cases:

```kotlin
import org.graphiks.webgpu.suite.acid.buffers.mappedAtCreation

suspend fun checkOne(device: GPUDevice) = mappedAtCreation(device)
```

The runner decides its own isolation and manages the device lifecycle; the browser runner creates a
fresh adapter and device per case. A case closes the resources it creates and never destroys
resources supplied by the runner. A context case owns the extra adapter and device it asks for and
closes them itself.

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

## CPU ArrayBuffer campaigns

The `arraybuffer-benchmarks` module runs its own CPU-only campaigns (no GPU device) on JVM, JS, Wasm,
Native and Android. The runner commands are in
[ArrayBuffer CPU performance](arraybuffer-performance.md); the bounds contract is in
[ArrayBuffer bounds and capacities](arraybuffer-bounds.md).
