# Specification maintenance

Run these commands from the repository root. Fetching source files and generating bindings are
separate, explicit operations; neither is part of `check`.

## 1. Fetch upstream source files

```sh
./gradlew check-cache
```

This downloads the WebGPU HTML specification from [W3C](https://www.w3.org/TR/webgpu/) and the
IDL from [GPUWeb](https://gpuweb.github.io/gpuweb/webgpu.idl), storing both under
`webgpu-specifications/src/jvmMain/resources/`. It also updates `cache.json`.

## 2. Rebuild documentation JSON

```sh
./gradlew refresh-documentation-from-spec
```

The task replaces `documentation.json` from the checked-in HTML and IDL. Review its prose before
generating bindings.

## 3. Fill optional missing descriptions

```sh
./gradlew generate-doc-from-llm
```

This optional command needs an OpenAI-compatible chat-completions server at
`http://127.0.0.1:1234/v1` serving `mistral-small-3.1-24b-instruct-2503`. It is never run by CI.

## 4. Check coverage and convert to YAML

```sh
./gradlew check-missing-doc
./gradlew tranform-json-doc-to-yaml
```

The first task prints missing keys. The second writes `documentation.yaml`; its task name is
currently spelled `tranform` without the second “s”.

## 5. Generate bindings

```sh
./gradlew generate-binding
```

The generator reads the checked-in IDL and YAML and rewrites generated Kotlin sources in
`webgpu-api`, `webgpu-descriptors`, and `webgpu-web`. Review the changes and run the
[business tests](testing.md). Do not edit generated sources directly.
