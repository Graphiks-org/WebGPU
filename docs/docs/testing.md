# Testing

Run the standard business-test lifecycle from the repository root:

```sh
./gradlew check
```

Gradle runs tasks supported by the current host. The `webgpu-api` suites exercise buffer and
enumeration behavior; `webgpu-web-bindings` covers JS/Wasm interop. On a suitable host, useful
targeted commands include:

```sh
./gradlew :webgpu-api:jvmTest :webgpu-api:checkKotlinAbi
./gradlew :webgpu-api:jsNodeTest :webgpu-web-bindings:jsNodeTest
./gradlew :webgpu-api:wasmJsNodeTest :webgpu-web-bindings:wasmJsNodeTest
```

Kotlin/Native test tasks depend on the host. CI runs the business tests on Linux, macOS, and
Windows. Specification downloads, optional LLM documentation, and binding regeneration are
manual maintenance steps; they are not PR test gates.

## Browser acid cases

The eleven foundation cases run on real WebGPU through a separate browser workflow; `check`
alone does not execute this runner. With JDK 25 and Node.js 22:

```sh
npm ci --prefix tools
npm exec --prefix tools -- playwright install chromium
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/build-site.mjs
```

Compilation generates the catalogue from the annotated cases. The runner produces separate JS and
Wasm reports and fails on missing, failed or incomplete execution. The `--benchmark` mode requires
all ten benchmark scenarios `completed` and GPU-verified with the profile's sample count; it never
fails on the value of a duration. The [Validation page](suite.md) distinguishes behaviour coverage
from run results, and the [Benchmarks page](suite.md) presents the published measurements and their
protocol. Tests assert WebGPU data, errors and states; there is no dedicated Maven consumer or
publication-architecture test project, and no statistical/presentation test framework.
