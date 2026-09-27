# Testing

Run the standard business-test lifecycle from the repository root:

```sh
./gradlew check
```

Gradle runs tasks supported by the current host. The `webgpu-api` suites exercise buffer and
enumeration behavior; `webgpu-web-bindings` and `webgpu-browser` cover JS/Wasm interop and
conversion without a GPU. On a suitable host, useful targeted commands include:

```sh
./gradlew :webgpu-api:jvmTest :webgpu-api:checkKotlinAbi
./gradlew :webgpu-api:jsNodeTest :webgpu-web-bindings:jsNodeTest :webgpu-browser:jsNodeTest
./gradlew :webgpu-api:wasmJsNodeTest :webgpu-web-bindings:wasmJsNodeTest :webgpu-browser:wasmJsNodeTest
```

## Browser GPU tests

`integration-tests/browser` is a standalone build that runs real WebGPU compute, canvas, and error
tests under a Chromium with a WebGPU backend. It has no Node target and is not included in the root
build.

```sh
./gradlew -p integration-tests/browser jsBrowserTest wasmJsBrowserTest
```

Without `-PpublicationRepository`, the build uses a composite build and substitutes the repository
projects. To test artifacts from an isolated local Maven repository instead, set
`publicationRepository` and `testedVersion`:

```sh
./gradlew -p integration-tests/browser \
  -PpublicationRepository="$MAVEN_REPO" \
  -PtestedVersion=0.1.0-browser-verification-SNAPSHOT \
  jsBrowserTest wasmJsBrowserTest
```

Set `CHROME_BIN` when the browser is not on the default path. The custom launcher passes the
WebGPU SwiftShader flags; a runner without a usable backend fails the job rather than skipping it.

Kotlin/Native test tasks depend on the host. CI runs the business tests on Linux, macOS, and
Windows, and a dedicated browser-GPU workflow. Specification downloads, optional LLM documentation,
and binding regeneration are manual maintenance steps; they are not PR test gates.
