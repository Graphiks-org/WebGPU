# ArrayBuffer CPU performance

This guide records the `arraybuffer-cpu` protocol, the commands that produce its reports, and two
readings of the Graphiks bounds checks: the first before/after comparison, then five-launch
medians of the checked path against the raw platform primitive. Durations are informative: the CI
never fails on a duration, only on a wrong result, a crash or an incomplete report.

## Protocol

- Protocol `arraybuffer-cpu`, implemented in `arraybuffer-benchmarks`. These are CPU-only
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
./gradlew :arraybuffer-android-instrumentation:installRelease \
  :arraybuffer-android-instrumentation:installReleaseAndroidTest
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

Median change of the per-sample duration, by workload (positive = the checked build is slower):

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
  `arraybuffer-cpu-writers` companion protocol exist precisely to move the checks out of the
  per-element loop on this target.
- The JVM `scalar.read.i32` and `scalar.write.f32` medians moved by less than 1%; the write loop is
  the clearest regression, consistent with two non-inlined checks per store.

### What this does not say

- The scalar deltas are **not** attributions of a precise instruction cost; the checks are not
  inlined and no profiler was attached to this reading. `allocationMeasurement` is `unavailable`.
- The logical throughput of a bulk copy is not the DRAM traffic of the copy.
- `of` shares the backing store in JS and copies on the other targets; no cross-target conclusion
  about "a faster runtime" follows.

## Five-launch medians

Same workstation as the first reading (physical macOS arm64), merged branch tip `94dfcbd`
(2026-10-06), JDK 25.0.1, Chromium 153.0.8010.12, five `standard` launches per target (reports
`post3-<target>-run0..4.json`). Where the first reading compares two builds, this one compares two
variants inside each report: `Checked` and `Reference` share the repetition count of their layout
group, so each ratio is computed per launch and the median of the five launches is shown with its
range. The ratio therefore measures the whole public checked path — validation plus the API layer —
against the raw platform primitive on the same build. The library sources of that build are unchanged
since the first reading (identical `libraryHash` on `94dfcbd`), so both readings describe the same
code, and the cost of the checks alone remains the first reading's percentages. The same caveat
applies: this is a workstation, not a benchmark rig.

Median of the per-launch `Checked`/`Reference` ratios (range of the five launches in parentheses):

| Target | scalar.write.i32 | scalar.read.i32 | scalar.write.f32 | scatter.write.i32 | bulk ≥ 64 KiB | bulk 256 B |
| --- | --- | --- | --- | --- | --- | --- |
| jvm | 1.63–1.91× (1.62–2.38×) | ≈0.99× (0.98–1.19×) | 1.61–1.94× (1.58–1.95×) | 0.15–0.17× (0.14–10.20×) | 0.83–1.12× (0.69–1.55×) | 1.02–1.03× (0.98–1.05×) |
| native | ≈4.3× (4.00–4.41×) | ≈7.3× (6.85–7.48×) | 6.97–9.58× (6.92–9.71×) | 4.32–6.12× (4.25–11.67×) | 1.00–1.03× (0.90–1.13×) | 1.33–1.46× (1.31–1.47×) |
| js | ≈27.7× (26.7–36.2×) | ≈27.8× (26.3–36.8×) | ≈27.5× (26.8–36.3×) | ≈29.2× (26.5–34.7×) | 1.00–1.01× (0.97–1.03×) | 2.24–4.36× (2.18–4.91×) |
| wasm | ≈1.18× (1.15–1.33×) | ≈1.22× (1.18–1.42×) | ≈1.20× (1.03–1.24×) | ≈1.25× (1.23–1.28×) | 0.94–0.95× (0.93–0.97×) | 0.94–0.96× (0.93–0.97×) |

Per-element checked stores against the validate-once bulk copy (`Checked`/`BulkPrepared`, median,
range):

| Target | image 256² | image 1024² | image 4096² | vertices 1 K | vertices 64 K | vertices 1 M |
| --- | --- | --- | --- | --- | --- | --- |
| jvm | 29.5× (28.4–164×) | 40.3× (30.2–176×) | 26.9× (26.1–111×) | 10.4× (7.6–43.8×) | 13.3× (11.1–66.0×) | 5.9× (5.9–28.7×) |
| native | 260× (252–262×) | 262× (255–268×) | 236× (225–243×) | 90.4× (39.3–98.2×) | 116× (114–117×) | 109× (85.5–110×) |
| js | 138× (135–194×) | 161× (156–196×) | 161× (158–201×) | n/a | 871× (651–996×) | 764× (753–1003×) |
| wasm | 3.67× (3.66–3.72×) | 3.68× (2.39–3.70×) | 3.68× (3.64–3.73×) | 3.45× (3.38–3.53×) | 3.41× (3.31–3.44×) | 3.40× (3.21–3.44×) |

### Reading

- **The checks are free on the paths the API is meant for.** At 64 KiB and above, every checked
  bulk copy sits between 0.83× and 1.12× across the four targets: the validation runs once per call
  and the copy dominates. On Wasm the checked bulk path is even consistently around 5% faster than
  the raw primitive (0.94–0.96×, range 0.93–0.97×); the Reference primitive is not always the floor.
- **Scalar access pays per element.** JVM writes pay +61–94% and reads stay flat (≈0.99×); Native
  pays ≈4.3× per int store and ≈7.3× per load; JavaScript pays ≈27× because the unsigned-offset
  arithmetic of the checks is emulated per element; Wasm pays +18–25%, the smallest scalar cost of
  the four.
- The JVM scatter cell is bimodal across launches (per-launch ratios from 0.14× to 10.2×): the
  scattered `ByteBuffer` primitive is itself subject to a JIT pathology and neither variant
  dominates on that target. Treat the cell as unstable, not as a checked-path win.
- Building an image or a vertex stream through per-element checked stores instead of the
  validate-once bulk copy costs from 3.4× (Wasm) to 262× (Native). The JavaScript 1 K-vertex bulk
  median rounds to a zero duration and is not reported, as in the writers table below.
- The image and vertex ranges are wide on the JVM because a single launch can warm the bulk-copy
  path unevenly; the medians of the per-launch ratios remain the robust reading.

The instrumented Android campaign keeps the single launch of the first reading; five launches on a
device remain to be produced for it.

## Prevalidated writers (`arraybuffer-cpu-writers`)

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
(`.github/workflows/test.yml`): its `./gradlew check` matrix (Ubuntu, macOS, Windows) covers the
`webgpu-api` and `arraybuffer-benchmarks` suites, each host's native targets and `checkKotlinAbi`.
The instrumented Android matrix is not part of CI; it is run by hand on a device or an emulator
with the documented command. No benchmark campaign runs on a pull request — a shared runner is
not a performance reference.
Campaigns, their tooling and the tooling's unit tests belong to the machines where the campaigns are
run; the commands are listed above, and durations are never a CI gate.

## Campaigns not executed

- A lifecycle companion campaign (allocation + fill, `of` + consumption) is not implemented yet.
- Allocation profiling (JFR on the JVM, Perfetto on Android, DevTools sampling in Chromium, an
  allocation profiler on the host for Native) was not run; every report keeps
  `allocationMeasurement: "unavailable"`.
- Five `standard` launches per target were captured on the first reading's workstation for JVM,
  Native, JavaScript and Wasm (2026-10-06); the medians are recorded above. The instrumented
  Android campaign remains a single launch; five launches on a device remain to be produced.
