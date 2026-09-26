# WebGPU specification fetcher design

## Goal

Make retrieval of the WebGPU HTML and IDL resources reliable, and isolate that network operation from binding generation. The checked-in copies remain the inputs to normal builds and generation tasks.

## Current behavior

`RemoteFileManager` lives in `buildSrc` and serves both local resource lookup and remote refresh. Its download helper returns whenever the destination exists. The refresh path first creates an existing temporary file, so its download is skipped; the refresh path then also skips replacing the existing resource, but records the old file hash with a new timestamp. As a result, `check-cache` can report success without retrieving updated content.

The remote resources are stored under `webgpu-ktypes-specifications/src/jvmMain/resources`. The root CI workflow runs `check`, not `check-cache`.

## Architecture

Add an included Gradle build at `build-logic` that provides a small WebGPU specification-fetcher plugin. Register the existing root task name, `check-cache`, through this plugin. Configure its output directory from the root project to point at the resources directory above. The plugin is build tooling only; it does not create or publish a runtime artifact.

Keep the binding and documentation generation tasks in `buildSrc`. They read the checked-in resources locally and do not trigger network access. Keep a small local-only resource-path helper there, but remove all remote download and cache mutation responsibilities from `RemoteFileManager`.

The root `check` and publication builds must not depend on `check-cache` or contact the specification URLs. Refresh remains an explicit developer action.

## Refresh behavior

Each explicit invocation of `check-cache` retrieves both configured resources, regardless of cache age. The sources are:

- `https://www.w3.org/TR/webgpu/` → `webgpu.html`
- `https://gpuweb.github.io/gpuweb/webgpu.idl` → `webgpu.idl`

For each response, stream the bytes to a unique temporary file in the target directory, calculate its SHA-256 hash, and validate that the response is successful and non-empty. Retrieve and validate both resources before replacing either checked-in file. If a hash matches the current file, leave that file untouched. If it differs, replace that file atomically, with a replacement fallback only when the filesystem does not support atomic moves.

After all downloads validate and changed resources have been replaced, atomically write `cache.json`. Store one entry per resource, containing its current hash and the time it was successfully checked. Existing duplicate entries are normalized during this write. Do not update cache metadata before downloads and file replacements succeed. Always clean up temporary files.

If an HTTP request, response validation, file write, replacement, or cache write fails, fail the Gradle task with the source URL and operation in the error. Preserve the previous cache metadata. If a failure occurs after one resource has been atomically replaced, the old cache metadata remains, so the next explicit run will retrieve both resources and reconcile the hashes rather than claiming the pair is current.

## Testing and acceptance criteria

Test the fetcher against a local HTTP server so tests do not depend on public network availability. Cover:

1. Initial retrieval when target files or cache entries are absent.
2. An unchanged response: keep resource bytes unchanged and refresh the checked-at metadata.
3. Changed response: replace the resource and store the matching new SHA-256 hash.
4. HTTP failure: fail the task and keep both resources and cache metadata unchanged.
5. Cache normalization: repeated runs retain exactly one cache entry per resource.
6. Temporary-file cleanup after success and failure.

Run the included build's tests in CI in addition to the existing root `./gradlew check`. The normal root check must remain offline with respect to these two URLs. A manual end-to-end run of `./gradlew check-cache` should retrieve both URLs and report whether each local resource changed.

## Scope boundaries

This change extracts only specification fetching and cache refresh. It does not move the binding generator, documentation generator, or other `buildSrc` tasks. It does not make generation refresh resources automatically, and it does not change the checked-in WebGPU resource formats.
