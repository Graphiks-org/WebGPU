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
