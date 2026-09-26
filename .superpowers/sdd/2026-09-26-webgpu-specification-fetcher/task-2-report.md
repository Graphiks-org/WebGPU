# Task 2 report — explicit Gradle plugin task

## Changes

- Added `io.ygdrasil.webgpu-specification-fetcher` plugin registration and its implementation in `build-logic`.
- Added `WebGpuSpecificationFetcherExtension` with the conventional specifications resource path and the two approved WebGPU URL defaults.
- Added the `check-cache` task, delegating to `SpecificationRefreshService`, declaring its output directory, logging each file as changed or unchanged, and disabling up-to-date and build-cache reuse.
- Added TestKit coverage for default output placement, URL overrides, and two explicit invocations fetching each local-server endpoint twice.

Files: `build-logic/build.gradle.kts`, `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/WebGpuSpecificationFetcherPlugin.kt`, and `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/WebGpuSpecificationFetcherPluginTest.kt`.

## TDD evidence

- RED: `rtk ./gradlew -p build-logic test --tests 'io.ygdrasil.webgpu.fetcher.WebGpuSpecificationFetcherPluginTest'` — both TestKit cases failed because Gradle could not find `io.ygdrasil.webgpu-specification-fetcher`.
- GREEN: the same focused command passed both tests. Assertions verified the default and overridden output locations, `changed`/`unchanged` task results, and two requests to each server endpoint across two task runs.
- Full included-build suite: `rtk ./gradlew -p build-logic test` — `BUILD SUCCESSFUL`.
- `rtk git diff --cached --check` — clean before the implementation commit.

## Review and commit

- Self-review confirmed the plugin uses only the Task 1 refresh service and does not wire `check-cache` into normal build tasks. No Task 3/4 files or the SDD progress ledger were changed.
- Implementation commit: `6092659` (`feat: expose specification refresh as Gradle task`).

## Reservations

- Gradle emitted the existing Kotlin DSL/serialization plugin version warning (`embedded-kotlin` 2.3.20 versus serialization plugin 2.3.21); it did not prevent the tests from passing.
- Root `check` and live public-source retrieval are outside Task 2 and remain for the later plan tasks.
