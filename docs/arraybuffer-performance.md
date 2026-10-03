# ArrayBuffer CPU performance

This guide records the `arraybuffer-cpu-v1` protocol, the commands that produce its reports, and the
first before/after reading of the Graphiks bounds checks. Durations are informative: the CI never
fails on a duration, only on a wrong result, a crash or an incomplete report.

## Protocol

- Schema `arraybuffer-cpu-v1`, implemented in `arraybuffer-benchmarks`. These are CPU-only
  measurements and are never compared with GPU-suite results.
- Scenario id format: `<workload>.bytes-<bytes>.<variant>`.
- Workloads: `scalar.write.i32`, `scalar.read.i32`, `scalar.write.f32`, `scatter.write.i32`,
  `bulk.bytes`, `bulk.floats`, `image.rgba8`, `vertices.p3n3uv2`.
- Variants: `Checked` uses the public `ArrayBuffer` methods; `Reference` uses the platform
  primitive the library uses today, without the new Graphiks checks; `BulkPrepared` copies a source
  array produced outside the timed window; `PrepareAndBulk` builds and copies that array inside the
  window.
- Profiles: `ci` (3 warm-ups, 5 samples, 1 launch) and `standard` (5 warm-ups and at least 2 s of
  warm-up, 30 samples, 5 launches).
- The runner reports the samples, seeds, calibration, environment and checksum. A zero duration is
  kept; it never becomes an infinite throughput.

## Commands

```sh
# Build the harness and run its unit tests
./gradlew :arraybuffer-benchmarks:jvmTest :arraybuffer-benchmarks:jsNodeTest \
  :arraybuffer-benchmarks:wasmJsNodeTest :arraybuffer-benchmarks:macosArm64Test
./gradlew :arraybuffer-benchmarks:jsBrowserDistribution \
  :arraybuffer-benchmarks:wasmJsBrowserDistribution :arraybuffer-benchmarks:linkReleaseExecutableMacosArm64

# JVM
./gradlew :arraybuffer-benchmarks:runJvmBenchmarks \
  --args="--profile=standard --output=build/reports/arraybuffer/baseline-jvm-run0.json --run-index=0"

# Native (macOS arm64; use linuxX64 on Linux)
arraybuffer-benchmarks/build/bin/macosArm64/releaseExecutable/arraybuffer-benchmarks.kexe \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-native-run0.json

# Browser (no GPU device is created)
node tools/run-arraybuffer-benchmarks.mjs js \
  arraybuffer-benchmarks/build/dist/js/productionExecutable \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-js-run0.json
node tools/run-arraybuffer-benchmarks.mjs wasm \
  arraybuffer-benchmarks/build/dist/wasmJs/productionExecutable \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-wasm-run0.json

# Android (emulator or device already connected)
./gradlew :arraybuffer-benchmarks-android:installRelease \
  :arraybuffer-benchmarks-android:installReleaseAndroidTest
node tools/run-arraybuffer-android.mjs \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-android-run0.json

# Compare a before/after pair. The candidate must reuse the baseline's repetition counts:
#   --calibration=<baseline report> works for every runner.
node tools/compare-arraybuffer-benchmarks.mjs \
  build/reports/arraybuffer/baseline-jvm-run0.json \
  build/reports/arraybuffer/post-jvm-run0.json \
  --output=build/reports/arraybuffer/compare-jvm.json
```

Validate or compare reports:

```sh
node tools/arraybuffer-report.mjs <report.json> --profile=standard
node --test tools/arraybuffer-report.test.mjs tools/compare-arraybuffer-benchmarks.test.mjs
```

## First reading

Environment: macOS (arm64), JDK 25, Chromium 153, Android emulator API 35 (`bench35`). One
`standard` launch per target was captured before the checks (`baseline-*`) and one after
(`post2-*`), with the baseline repetition count reused so the two runs share
`operationsPerSample`. The comparison is strict: a different environment or a different repetition
count is refused rather than silently normalised. A single launch on a workstation is **not** a
stable hardware benchmark reference; treat the numbers as directional.

Median change of the per-sample duration, by workload (positive = the checked version is slower):

| Target | scalar.write.i32 | scalar.read.i32 | scalar.write.f32 | scatter.write.i32 | bulk.bytes | bulk.floats | image.rgba8 | vertices.p3n3uv2 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| jvm | +61% | +0.3% | -0.6% | +0.2% | +3.7% | +2.7% | -2.3% | +2.6% |
| native | +266% | +277% | +74% | +279% | +7.1% | -3.6% | -3.0% | -3.7% |
| js | +1768% | +1607% | +2099% | +2262% | 0.0% | -1.3% | 0.0% | +1.8% |
| wasm | +14% | +15% | +17% | +17% | -2.0% | -0.9% | -0.5% | +2.2% |
| android (emulator) | +72% | +95% | +58% | +78% | +3.4% | +2.6% | -0.3% | -1.0% |

### Interpretation

- **Bulk copies, images and vertices are essentially flat.** Their checks run once per operation and
  the primitive itself dominates, so the extra validation is amortised. This is the intended shape:
  validate a range once, then copy or produce.
- **Scalar access pays the whole control cost per element.** On Native the check roughly triples the
  time of a raw store; on the JVM it is around +60% for the int write loop; the Android emulator is
  in the same range. The `Reference` variant of the same run stays near its baseline (for example,
  JS `scalar.read.i32` reference moves from ~9.1 ms to ~9.9 ms for the same repetition count), so the
  delta is the added controls, not a change of environment.
- **Kotlin/JS is by far the most affected.** The unsigned arithmetic in the checks is emulated in
  JavaScript, which is far more expensive than a typed-array element access; a per-element scalar
  access becomes an order of magnitude slower. This is a design signal, not a measurement artefact:
  the `Reference` variant in the same run is unchanged. The prevalidated writers prototyped in the
  `arraybuffer-cpu-writers-v1` companion protocol exist precisely to move the checks out of the
  per-element loop on this target.
- The JVM `scalar.read.i32` and `scalar.write.f32` medians moved by less than 1%; the write loop is
  the clearest regression, consistent with two non-inlined checks per store.

### What this does not say

- The scalar deltas are **not** attributions of a precise instruction cost; the checks are not
  inlined and no profiler was attached to this reading. `allocationMeasurement` is `unavailable`.
- The logical throughput of a bulk copy is not the DRAM traffic of the copy.
- `of` shares the backing store in JS and copies on the other targets; no cross-target conclusion
  about "a faster runtime" follows.

## Prevalidated writers (`arraybuffer-cpu-writers-v1`)

The internal prototypes `RgbaWriterPrototype`/`VertexWriterPrototype` validate a whole layout once
(rows, stride, range, base alignment), then perform the pixel or vertex stores with the platform
primitive. They expose no handle and no callback. Their companion campaign re-measures its own
controls:

```sh
./gradlew :arraybuffer-benchmarks:runJvmBenchmarks \
  --args="--writers --profile=ci --output=build/reports/arraybuffer/writers-jvm.json --run-index=0"
node tools/run-arraybuffer-benchmarks.mjs js <dist> --writers --profile=ci
node tools/run-arraybuffer-android.mjs --writers --profile=ci
```

Every variant of one layout group shares a single repetition count (`commonRepetitions`), so the raw
medians are directly comparable inside a campaign. `writer/bulk` below is the writer median divided
by the `BulkPrepared` median; below 1 means the writer wins. The JavaScript 1 K-vertex cell is not
reported: the bulk median rounds to a zero duration in that run.

| Target | image 256² | image 1024² | image 4096² | 1 K vertices | 64 K vertices | 1 M vertices |
| --- | --- | --- | --- | --- | --- | --- |
| jvm | 9.3 | 14.7 | 12.5 | 4.1 | 4.7 | 3.0 |
| native | 21.5 | 12.9 | 19.9 | 5.2 | 7.8 | 5.8 |
| js | 0.50 | 0.67 | 0.67 | n/a | 1.33 | 1.15 |
| wasm | 1.00 | 1.01 | 1.00 | 1.11 | 1.07 | 1.10 |
| android (emulator) | 129 | 128 | 118 | 21 | 35 | 26 |

Against the **scalar** checked path the writers are faster everywhere (`writer/checked` ranges from
0.49 on the JVM down to ~0.06 on Native/Android and ~0.00 on JS, whose checked path is dominated by
unsigned arithmetic). Against **`BulkPrepared`**, however, the writers are 3–21× slower on the JVM,
Native and Android, only competitive on JavaScript images (≈0.5–0.67) and roughly equal on Wasm.

### Decision

**Keep the writers as internal prototypes; do not expose a public writer API on this evidence.**

- Everywhere the checked scalar path is slow, a prepared CPU array plus `setBytes`/`setFloats`
  (`BulkPrepared`/`PrepareAndBulk`) already matches or beats the writer (3–21× faster on JVM, Native
  and Android; equal on Wasm), with no new public surface.
- The only clear writer win is JavaScript images (≈0.5–0.67 of the bulk copy), where the checked
  scalar path is an order of magnitude slower; a public API justified by one CI launch on one target
  is not warranted.
- The per-target dispersion is of the same order as several of the differences, so a decision needs
  five `standard` launches on a stable host, not a single `ci` run.

## CI

There is no dedicated ArrayBuffer workflow. Pull requests run the standard **Tests** workflow
(`.github/workflows/test.yml`): its `./gradlew check` matrix (Ubuntu, macOS, Windows) already covers
the `webgpu-api` and `arraybuffer-benchmarks` suites, each host's native targets and
`checkKotlinAbi`. Its `android` job runs the instrumented `ArrayBufferAndroidBusinessCases` matrix on
an emulator, mirroring the Android `ByteBuffer` implementation as a peer of the other test targets.
No benchmark campaign runs on a pull request — a shared runner is not a performance reference.
Campaigns, their tooling and the tooling's unit tests belong to the machines where the campaigns are
run; the commands are listed above, and durations are never a CI gate.

## Campaigns not executed in this increment

- The `arraybuffer-cpu-lifecycle-v1` companion (allocation + fill, `of` + consumption) is not
  implemented yet.
- Allocation profiling (JFR on the JVM, Perfetto on Android, DevTools sampling in Chromium, an
  allocation profiler on the host for Native) was not run; every report keeps
  `allocationMeasurement: "unavailable"`.
- Only one `standard` launch per target was captured; the five-launch medians remain to be produced
  on a stable host. The `arraybuffer-cpu-writers-v1` companion is defined in the next task.
