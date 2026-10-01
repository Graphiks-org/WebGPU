# Benchmarks

The Graphiks WebGPU Suite ships two portable workloads that measure how a WebGPU implementation
performs `queue.writeBuffer` and the encoding and submission of a compute load. The portable module
is `suite-benchmarks`; the browser runner supplies the device and publishes a report; the
[Benchmarks page](suite/benchmarks/) presents the published results.

These are observations of CPU issue and completion, not GPU time. A duration is only meaningful when
the GPU work and its execution context are valid: every published result names the target, the
profile, the requested backend and the environment.

## Protocol `foundations-v1`

The protocol is identified by the string `foundations-v1`, carried by every report. Changing a
workload or a temporal boundary changes the identifier; results from different protocol identifiers
must not be compared silently.

| Workload | Size | Operations per batch | Scenarios |
| --- | --- | --- | --- |
| `transfer.write-buffer` | 4 KiB, 64 KiB or 1 MiB | 1 or 16 `writeBuffer` calls | 6 |
| `compute.encode-submit` | 1 024 or 65 536 `u32` elements | 1 or 16 dispatches | 4 |

The ten scenarios have deterministic ids and a fixed order:

```
transfer.write-buffer.bytes-4096.batch-1
transfer.write-buffer.bytes-4096.batch-16
transfer.write-buffer.bytes-65536.batch-1
transfer.write-buffer.bytes-65536.batch-16
transfer.write-buffer.bytes-1048576.batch-1
transfer.write-buffer.bytes-1048576.batch-16
compute.encode-submit.elements-1024.batch-1
compute.encode-submit.elements-1024.batch-16
compute.encode-submit.elements-65536.batch-1
compute.encode-submit.elements-65536.batch-16
```

Transfer scenarios run by size ascending, then compute scenarios by element count ascending; for each
size the batch of 1 runs before the batch of 16. JS and Wasm are separate runs, each on its own
adapter, and are shown separately. The order is published so a reader can follow it; it does not
justify a causal comparison between targets.

## Profiles

| Profile | Warm-ups | Retained samples |
| --- | ---: | ---: |
| `standard` | 5 | 30 |
| `ci` | 3 | 5 |

Both profiles run the same workloads at the same sizes and batch sizes; only the number of warm-ups
and retained samples changes. The `ci` profile is a working check with a few observations, not a
statistical campaign. The collector defaults to `ci`; the local page defaults to `standard`.

## Measured quantities

Each sample stores two durations read from the same clock, `kotlin.time.TimeSource.Monotonic`:

- `cpuIssueMs` — from the start of the GPU calls until they return synchronously. For
  `compute.encode-submit` this includes submission.
- `completionMs` — from the same start until `queue.onSubmittedWorkDone()` resolves.

Both are durations for the whole batch. Dividing either by the number of operations gives an
amortised per-operation average and never an individual latency. `completionMs - cpuIssueMs` is not
presented as GPU time.

The measurement window excludes, for both workloads, the creation of buffers, shader, pipeline and
bind group; the CPU-side payload preparation; and the readbacks. Before each sample the runner drains
the queue with `queue.onSubmittedWorkDone()`, outside the window. The transfer payloads are prepared
once before the campaign.

## Control readback

The runner reads the whole result buffer back and checks it against a value computed from the index,
never from a previous result:

- `transfer.write-buffer` writes the word `i xor (0x9e3779b9 + j)` for write `j`; the last write of a
  batch is the verified value.
- `compute.encode-submit` writes `i * 3 + 7` at element `i`; dispatches may repeat the same values.

The readback runs once before the warm-ups and once after the last sample. A mismatch, a validation
error, an uncaptured error or a timeout invalidates the whole scenario: no duration is published as
usable, and the scenario is `failed`. The runner still continues with the other scenarios, except
after a user cancellation or a hidden tab.

## Statuses and presentation

A scenario is `completed` (with `outputVerified = true`), `failed`, `interrupted` or `not-run`. Only
`completed` scenarios are aggregated. Warm-ups, sizes, batch sizes and the raw sample pairs are
serialized with each result, and the raw report is downloadable.

The page computes a median (middle value, or the mean of the two middle values) and the min/max
interval for each duration, and a p95 only for at least twenty samples, using the nearest-rank
convention above after sorting (`ceil(0.95 * n) - 1`). Zero and repeated values are kept; a zero
`cpuIssueMs` is reported as a low-resolution limit of the environment, and no sample is filtered and
no throughput is derived by dividing by zero. There is no global score, no JS/Wasm ratio, no speed
factor and no regression threshold in this increment.

## Interpretation limits

- A duration is not pure GPU time and depends on the runtime, the browser and the machine. Results
  from different environments are not a controlled comparison.
- The CI collector runs Chromium with the SwiftShader flags; those results are software-backend
  functional observations, not physical-GPU performances. The `--backend=default` collector mode
  lets Chromium choose and records `default`, never `hardware`.
- The compute workload is deliberately simple: the dispatches write the same values. It is not a
  model of a general GPU application.
- No GPU timestamp query and no particle benchmark are part of this protocol.

## Run the benchmarks locally

```sh
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/build-site.mjs
```

The runner writes `build/reports/benchmarks-<target>.json` and exits non-zero when a scenario is
missing, duplicated, not `completed`, not verified, or has the wrong sample count. It never fails on
the value of a duration.

## Use the workloads from a binding

A native binding supplies a device it owns and calls the portable functions directly. The functions
close the resources they allocate and never close the device; the caller keeps the error scopes,
timeouts, uncaptured errors and metadata, as the browser runner does.

```kotlin
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.suite.benchmarks.BenchmarkProfile
import org.graphiks.webgpu.suite.benchmarks.benchmarkCompute
import org.graphiks.webgpu.suite.benchmarks.benchmarkWriteBuffer

suspend fun measure(device: GPUDevice) {
    val transfer = benchmarkWriteBuffer(device, 65536, 16, BenchmarkProfile.Standard)
    val compute = benchmarkCompute(device, 65536, 16, BenchmarkProfile.Standard)
    // Each result carries the raw samples and the protocol fields; the binding adds its own
    // environment, versions and commit, and publishes them with the same statuses as the runner.
}
```

A returned `BenchmarkResult` means the scenario finished and its GPU readbacks matched. A failure
throws; the caller records the diagnostic and does not publish the samples as usable.

## CPU ArrayBuffer benchmarks

Separately from the GPU `foundations-v1` protocol, the `arraybuffer-benchmarks` module measures the
CPU cost of the `ArrayBuffer` bounds checks. Its contract and capacities are in
[ArrayBuffer bounds and capacities](arraybuffer-bounds.md); the protocol, commands and the first
before/after reading are in [ArrayBuffer CPU performance](arraybuffer-performance.md).
