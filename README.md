# webgpu-ktypes
Kotlin types for webgpu

## Refreshing the specifications and generating bindings

Run these commands from the repository root. Fetching the upstream files and generating the Kotlin bindings are separate, explicit steps.

### 1. Fetch the WebGPU source files

```shell
./gradlew check-cache
```

This task downloads the WebGPU HTML specification from [W3C](https://www.w3.org/TR/webgpu/) and the WebGPU IDL from [GPUWeb](https://gpuweb.github.io/gpuweb/webgpu.idl). It stores them in `webgpu-specifications/src/jvmMain/resources/` as `webgpu.html` and `webgpu.idl`, and updates `cache.json` with their hashes and refresh times. It runs only when invoked; it is not automatically part of `build` or `check`.

### 2. Rebuild the API documentation JSON

```shell
./gradlew refresh-documentation-from-spec
```

This replaces `documentation.json` from the checked-in WebGPU HTML and IDL. It rebuilds the full set of documentation keys instead of using the existing JSON descriptions as input. Review the generated prose before regenerating bindings.

### 3. Generate additional missing documentation (optional)

```shell
./gradlew generate-doc-from-llm
```

This task uses the cached HTML and IDL to infer documentation for API keys still missing from `documentation.json`. It expects an OpenAI-compatible chat-completions server at `http://127.0.0.1:1234/v1`, serving the `mistral-small-3.1-24b-instruct-2503` model. Start that server before running the task.

### 4. Check documentation coverage

```shell
./gradlew check-missing-doc
```

This task prints the API documentation keys that are still missing from `documentation.json`. Review the output and add or generate the missing entries before converting the JSON to YAML and regenerating the bindings.

### 5. Convert the documentation to YAML

```shell
./gradlew tranform-json-doc-to-yaml
```

This converts `documentation.json` into `documentation.yaml`, which is consumed by the binding generator. The task is currently registered as `tranform-json-doc-to-yaml` (without the second “s” in “transform”).

### 6. Generate the Kotlin bindings

```shell
./gradlew generate-binding
```

This task reads `webgpu.idl` and `documentation.yaml`, then rewrites the generated Kotlin sources in `webgpu-api`, `webgpu-web`, and `webgpu-descriptors`. Do not edit those generated files by hand; update the source specifications or documentation data and run the generator again.
