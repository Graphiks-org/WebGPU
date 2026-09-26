# WebGPU Specification Fetcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make explicit `check-cache` calls reliably refresh the checked-in WebGPU HTML and IDL files while keeping network fetching outside normal generation and build logic.

**Architecture:** Add an included Gradle build named `build-logic` that owns a specification-fetcher plugin, refresh service, cache format, and `check-cache` task. Keep `buildSrc` responsible for generators, changing its remote file manager into a local-only resource-path helper. Apply the plugin only to the root build and run its local-server tests in CI.

**Tech Stack:** Kotlin DSL, Kotlin, Gradle included build and TestKit, kotlinx.serialization JSON, JDK `HttpURLConnection` and `HttpServer`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-26-webgpu-specification-fetcher-design.md`

## Global Constraints

- Normal root `check` and publication builds must not depend on `check-cache` or contact the specification URLs.
- Each explicit `check-cache` invocation retrieves both resources regardless of cache age.
- Fetch `https://www.w3.org/TR/webgpu/` as `webgpu.html` and `https://gpuweb.github.io/gpuweb/webgpu.idl` as `webgpu.idl`.
- Download each response to a unique temporary file in the target directory; require a successful HTTP response and non-empty content.
- Retrieve and validate both resources before replacing either target; keep unchanged target files untouched.
- Compare downloaded hashes with the actual on-disk target files, not with cache records; create a missing target even when its cached hash matches the download.
- Replace changed resource targets atomically, falling back only if atomic moves are unsupported; write `cache.json` using a strict atomic replacement with no non-atomic fallback, after successful retrieval and resource replacement.
- Keep the existing `cache.json` schema (`cachedFiles`, `name`, `hash`, `updateDate`); write exactly one record per fetched resource and preserve ISO local date-time serialization.
- Preserve the previous cache metadata on any failure, include the source URL and failed operation in task errors, and clean up temporary files on success and failure.
- Do not move binding generation, documentation generation, or other `buildSrc` tasks into `build-logic`.
- Use a local HTTP server for automated tests; CI must run both `./gradlew check` and `./gradlew -p build-logic check`.

## Review Focus

- The second download fails after the first is staged: neither checked-in source nor `cache.json` changes. Pin this in Task 1 with `refreshDoesNotReplaceAnySourceWhenSecondDownloadFails`.
- A successful HTTP response has an empty body: fail with the source URL and remove staged files. Pin this in Task 1 with `refreshRejectsEmptyResponseAndCleansTemporaryFiles`.
- `cache.json` contains duplicate records from older runs: retain one current record for each source. Pin this in Task 1 with `refreshNormalizesDuplicateCacheEntries`.
- The filesystem rejects `ATOMIC_MOVE`: use the permitted resource fallback, but propagate other move failures and never fall back for cache metadata. Pin these cases in Task 1 with `resourceMoveFallsBackOnlyWhenAtomicMoveIsUnsupported`, `resourceMovePropagatesOtherFailures`, and `cacheMoveHasNoNonAtomicFallbackAndPreservesOldBytes`.
- Gradle sees the resource directory unchanged between explicit calls: still issue both HTTP requests each time. Pin this in Task 2 with `checkCacheAlwaysFetchesBothConfiguredSources`.

## File Map

- `build-logic/settings.gradle.kts`: own build settings and import the root version catalog.
- `build-logic/build.gradle.kts`: Kotlin DSL, serialization, plugin registration, JUnit and TestKit dependencies.
- `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/SpecificationRefreshService.kt`: source definitions, retrieval, validation, hashing, cache update, temporary cleanup, and refresh result.
- `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/AtomicFileReplacer.kt`: same-directory resource replacement and narrowly scoped unsupported-atomic fallback.
- `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/AtomicCacheWriter.kt`: strict atomic replacement for `cache.json`, without non-atomic fallback.
- `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/WebGpuSpecificationFetcherPlugin.kt`: extension defaults and `check-cache` task registration.
- `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/SpecificationRefreshServiceTest.kt`: deterministic refresh and failure tests using a local HTTP server and temporary directories.
- `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/AtomicFileReplacerTest.kt`: resource fallback and strict cache-move policy tests.
- `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/WebGpuSpecificationFetcherPluginTest.kt`: TestKit coverage of task registration, output location, and repeated explicit execution.
- `settings.gradle.kts`, `build.gradle.kts`: include `build-logic` for plugin resolution and apply the plugin to the root build.
- `buildSrc/src/main/kotlin/generator/files/SpecificationResources.kt`: local-only resource directory and path lookup for generator tasks.
- Existing `buildSrc` generator/task files that import `RemoteFileManager`: switch to `SpecificationResources` and preserve the same local resource names and paths.
- `buildSrc/src/main/kotlin/generator.gradle.kts`: remove the old `check-cache` registration and obsolete imports.
- `buildSrc/src/main/kotlin/generator/CheckCacheTask.kt`, `buildSrc/src/main/kotlin/generator/files/RemoteFileManager.kt`, `buildSrc/src/main/kotlin/generator/files/FileCache.kt`, `buildSrc/src/main/kotlin/generator/files/LocalDateTimeSerializer.kt`: delete once all consumers are migrated and the included plugin owns these responsibilities.
- `.github/workflows/test.yml`: run included-build checks alongside the existing root check.

---

### Task 1: Implement and unit-test the refresh engine

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/SpecificationRefreshService.kt`
- Create: `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/AtomicFileReplacer.kt`
- Create: `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/AtomicCacheWriter.kt`
- Test: `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/SpecificationRefreshServiceTest.kt`
- Test: `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/AtomicFileReplacerTest.kt`

**Interfaces:**
- `SpecificationSource(fileName: String, url: URI)` identifies one remote resource.
- `RefreshResult(changedFiles: Set<String>, hashes: Map<String, String>)` reports replacements and checked hashes.
- `SpecificationRefreshService.refresh(resourceDirectory: Path, sources: List<SpecificationSource>): RefreshResult` performs the refresh.
- `SpecificationRefreshException(message: String, cause: Throwable)` reports a failed operation and its source URL(s) for Gradle to surface.
- `FileMoveOperation.move(source: Path, target: Path, atomic: Boolean)` is the injected filesystem move seam used by both writers.
- `AtomicFileReplacer(move: FileMoveOperation = NioFileMoveOperation()).replaceResource(stagedFile: Path, targetFile: Path, operationContext: String)` replaces a resource atomically and retries non-atomically only for `AtomicMoveNotSupportedException`; the context includes its source URL.
- `AtomicCacheWriter(move: FileMoveOperation = NioFileMoveOperation()).replace(stagedCache: Path, cacheFile: Path, operationContext: String)` uses `ATOMIC_MOVE` without fallback; the context includes both checked URLs. A failure leaves the old `cache.json` intact.
- Read/write cache entries with the existing `FileCache` JSON shape and `LocalDateTime` ISO format. A missing cache means an empty cache; malformed or unreadable existing cache data is an error and must not be overwritten.
- Compare each downloaded SHA-256 with the actual target file's SHA-256; cache hashes are metadata, not the source of truth for whether a target needs repair.
- Inject a `Clock` and move operation so timestamps and move failures are deterministic in tests. Use JDK `HttpServer` on loopback for response/status cases.

- [x] **Step 1: Bootstrap the included build and write failing refresh tests**

  Configure `build-logic/settings.gradle.kts` with `pluginManagement.repositories { gradlePluginPortal() }` and `dependencyResolutionManagement.repositories { mavenCentral() }`, importing `../gradle/libs.versions.toml` as the `libs` catalog. In `build-logic/build.gradle.kts`, apply `kotlin-dsl` and the catalog's Kotlin serialization plugin, and depend on `libs.kotlinx.serialization.json`, `gradleTestKit()`, and JUnit 4 via `kotlin("test-junit")`. Configure its `test` task to use JUnit 4. Add the public class/function signatures from the Interfaces block with placeholder bodies so the tests compile but fail on behavior.

  Add local-server tests in `SpecificationRefreshServiceTest` named `refreshDownloadsMissingFilesAndCache`, `refreshKeepsUnchangedFilesAndRefreshesCheckedAt`, `refreshReplacesChangedFilesAndStoresNewHashes`, `refreshRepairsModifiedTargetEvenWhenCacheHashMatches`, `refreshCreatesMissingTargetEvenWhenCacheHashMatches`, `refreshDoesNotReplaceAnySourceWhenSecondDownloadFails`, `refreshRejectsEmptyResponseAndCleansTemporaryFiles`, `refreshNormalizesDuplicateCacheEntries`, `refreshPreservesCacheWhenResourceReplacementFails`, `refreshPreservesPreviousCacheWhenCacheCommitFailsAndNextRunReconciles`, and `refreshFailsOnMalformedCacheWithoutOverwritingIt`. Use a fixed `Clock`, local HTTP responses including a 503 and an empty 200 body, and assert URL plus operation in errors. Add `AtomicFileReplacerTest` cases `resourceMoveFallsBackOnlyWhenAtomicMoveIsUnsupported`, `resourceMovePropagatesOtherFailures`, and `cacheMoveHasNoNonAtomicFallbackAndPreservesOldBytes`.

  Pin the principal behaviors with assertions such as:

  ```kotlin
  @Test fun resourceMoveFallsBackOnlyWhenAtomicMoveIsUnsupported() {
      replacerWithAtomicMoveUnsupported().replaceResource(staged, target, sourceUrl.toString())

      assertEquals(1, atomicMoveAttempts)
      assertEquals(1, nonAtomicMoveAttempts)
      assertContentEquals(stagedBytes, target.readBytes())
  }

  @Test fun resourceMovePropagatesOtherFailures() {
      assertFailsWith<AccessDeniedException> {
          replacerFailingAtomicMove().replaceResource(staged, target, sourceUrl.toString())
      }

      assertEquals(1, atomicMoveAttempts)
      assertEquals(0, nonAtomicMoveAttempts)
  }

  @Test fun refreshRepairsModifiedTargetEvenWhenCacheHashMatches() {
      writeCache(hash = sha256(downloadedBytes))
      target.writeBytes("local edit".toByteArray())

      service.refresh(directory, sources)

      assertContentEquals(downloadedBytes, target.readBytes())
      assertEquals(sha256(downloadedBytes), cachedHash("webgpu.idl"))
  }

  @Test fun refreshCreatesMissingTargetEvenWhenCacheHashMatches() {
      writeCache(hash = sha256(downloadedBytes))
      assertFalse(target.exists())

      service.refresh(directory, sources)

      assertContentEquals(downloadedBytes, target.readBytes())
  }

  @Test fun refreshDoesNotReplaceAnySourceWhenSecondDownloadFails() {
      val oldTargets = snapshotTargets()
      val oldCache = cacheFile.readBytes()

      assertFailsWith<SpecificationRefreshException> { service.refresh(directory, sources) }

      assertEquals(oldTargets, snapshotTargets())
      assertContentEquals(oldCache, cacheFile.readBytes())
      assertTrue(temporaryFiles(directory).isEmpty())
  }

  @Test fun cacheMoveHasNoNonAtomicFallbackAndPreservesOldBytes() {
      val oldCache = cacheFile.readBytes()

      assertFailsWith<AtomicMoveNotSupportedException> {
          cacheWriterWithAtomicMoveUnsupported().replace(stagedCache, cacheFile, "checking $htmlUrl and $idlUrl")
      }

      assertEquals(1, atomicCacheMoveAttempts)
      assertEquals(0, nonAtomicCacheMoveAttempts)
      assertContentEquals(oldCache, cacheFile.readBytes())
  }
  ```

  For the unchanged response, assert the resource replacement hook was not called while the checked-at value changes. For cache-commit failure, retry the refresh and assert it reconciles both targets and writes matching hashes. Assert every failure case leaves no download or cache temp files.

- [x] **Step 2: Run the focused tests to verify they fail for missing implementation**

  Run: `rtk ./gradlew -p build-logic test --tests 'io.ygdrasil.webgpu.fetcher.*Test'`

  Expected: the test classes compile and behavior assertions fail because the placeholder refresh and move methods are not implemented.

- [x] **Step 3: Implement staged retrieval, validation, and hashing**

  Implement `SpecificationRefreshService.refresh` so it creates the destination directory, loads existing metadata without modifying it, downloads every source into a unique sibling temp file, checks 2xx status and non-empty body, and computes SHA-256. Do not replace a target until all sources have been downloaded and validated. Wrap failures with the relevant source URL and operation.

- [x] **Step 4: Implement replacement, cache commit, and cleanup**

  Implement `AtomicFileReplacer.replaceResource` with `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` and retry without `ATOMIC_MOVE` only after `AtomicMoveNotSupportedException`. Implement `AtomicCacheWriter.replace` with `ATOMIC_MOVE` and no fallback. Replace a resource only when its downloaded hash differs from the hash of its current on-disk bytes; create a missing target regardless of any matching cache entry. Then write cache JSON to a unique sibling temp file, normalize duplicate names to one record per configured source, update checked timestamps, and pass it to `AtomicCacheWriter`. Delete download and cache temp files in `finally` paths. If cache writing fails after resource replacement, leave the old cache file intact so a later run can reconcile against the actual files.

- [x] **Step 5: Run the focused tests and commit Task 1**

  Run: `rtk ./gradlew -p build-logic test --tests 'io.ygdrasil.webgpu.fetcher.*Test'`

  Expected: all refresh-service and file-move policy tests pass, including fallback, strict cache atomicity, repair from stale metadata, retry after cache-commit failure, and cleanup assertions.

  Commit: `feat: add WebGPU specification refresh engine`

### Task 2: Expose an explicit Gradle plugin task

**Files:**
- Create: `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/WebGpuSpecificationFetcherPlugin.kt`
- Create: `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/WebGpuSpecificationFetcherPluginTest.kt`
- Modify: `build-logic/build.gradle.kts`

**Interfaces:**
- Plugin ID: `io.ygdrasil.webgpu-specification-fetcher`.
- `WebGpuSpecificationFetcherExtension` exposes `resourceDirectory: DirectoryProperty`, `htmlUrl: Property<String>`, and `idlUrl: Property<String>`.
- Default URLs are the two exact WebGPU URLs in Global Constraints; default resource directory is `webgpu-ktypes-specifications/src/jvmMain/resources` under the project applying the plugin.
- Register task type `CheckCacheTask` under task name `check-cache`; it consumes the three extension properties and delegates to `SpecificationRefreshService.refresh` with file names `webgpu.html` and `webgpu.idl`.
- Disable task up-to-date skipping and build-cache reuse so every explicit invocation makes both requests.

- [x] **Step 1: Write TestKit tests for plugin registration and repeated invocations**

  Add `pluginRegistersCheckCacheWithDefaultResourceDirectory` to run the task with local-server URL overrides and assert both downloaded files and `cache.json` land under the conventional specifications resource path. Add `checkCacheAlwaysFetchesBothConfiguredSources` using a local server and an overridden temporary output directory; run the task twice with unchanged response bytes and assert each endpoint receives exactly two requests. Use TestKit's `withPluginClasspath()` for the plugin-under-test.

- [x] **Step 2: Run the focused TestKit tests to verify they fail**

  Run: `rtk ./gradlew -p build-logic test --tests 'io.ygdrasil.webgpu.fetcher.WebGpuSpecificationFetcherPluginTest'`

  Expected: test build fails because the plugin ID, extension, or task type is not registered.

- [x] **Step 3: Register the plugin and task**

  Add the plugin descriptor in `gradlePlugin`, implement the extension conventions and task type, construct the two `SpecificationSource` values from its URL properties, and invoke the Task 1 refresh service. Mark the output directory as a task output but configure the task to run every time and never use the build cache. Log one result per file as changed or unchanged using `RefreshResult.changedFiles`.

- [x] **Step 4: Run the focused TestKit tests and commit Task 2**

  Run: `rtk ./gradlew -p build-logic test --tests 'io.ygdrasil.webgpu.fetcher.WebGpuSpecificationFetcherPluginTest'`

  Expected: both plugin tests pass, with four total local-server requests across the two task executions; the service unit test separately verifies refreshed timestamps.

  Commit: `feat: expose specification refresh as Gradle task`

### Task 3: Wire the included plugin and make generators local-only

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/generator.gradle.kts`
- Create: `buildSrc/src/main/kotlin/generator/files/SpecificationResources.kt`
- Modify: `buildSrc/src/main/kotlin/generator/CheckMissingDocumentationTask.kt`
- Modify: `buildSrc/src/main/kotlin/generator/GenerateBindingTask.kt`
- Modify: `buildSrc/src/main/kotlin/generator/LLMDocGeneratorTask.kt`
- Modify: `buildSrc/src/main/kotlin/generator/TransformJsonDocToYamlTask.kt`
- Modify: `buildSrc/src/main/kotlin/generator/tasks/ModelGenerator.kt`
- Modify: `buildSrc/src/main/kotlin/generator/lm/DocumentGeneratorManager.kt`
- Delete: `buildSrc/src/main/kotlin/generator/CheckCacheTask.kt`
- Delete: `buildSrc/src/main/kotlin/generator/files/RemoteFileManager.kt`
- Delete: `buildSrc/src/main/kotlin/generator/files/FileCache.kt`
- Delete: `buildSrc/src/main/kotlin/generator/files/LocalDateTimeSerializer.kt`

**Interfaces:**
- Add `includeBuild("build-logic")` inside root `pluginManagement` so the root can resolve the included plugin.
- Apply `id("io.ygdrasil.webgpu-specification-fetcher")` in root `build.gradle.kts`; leave it out of subproject build files.
- `SpecificationResources(projectDirectory: Path)` exposes `specificationsSourcePath: Path`, `findFilePath(fileName: String): Path?`, and `Files` constants for `webgpu.html`, `webgpu.idl`, `documentation.yaml`, and `documentation.json`. It must only resolve local paths; it must not read or mutate cache metadata or access the network.
- Keep existing generator behavior and file names while replacing each `RemoteFileManager` use with `SpecificationResources`.

- [x] **Step 1: Migrate generator consumers to a local-only helper and remove old refresh registration**

  Add the helper and update all listed consumers. Remove the old `check-cache` registration and obsolete imports. Before deleting old implementation files, search for remaining references while excluding those definitions with `rtk rg -n 'RemoteFileManager|FileCache|LocalDateTimeSerializer|CheckCacheTask' buildSrc/src/main/kotlin -g '!**/generator/CheckCacheTask.kt' -g '!**/generator/files/RemoteFileManager.kt' -g '!**/generator/files/FileCache.kt' -g '!**/generator/files/LocalDateTimeSerializer.kt'`; remove any remaining consumer references. Then delete the four obsolete files and verify a full `rtk rg -n 'RemoteFileManager|FileCache|LocalDateTimeSerializer|CheckCacheTask' buildSrc/src/main/kotlin` returns no matches.

- [x] **Step 2: Include and apply the plugin, then verify task wiring**

  Add the included build to `pluginManagement` and apply the plugin to the root build. Run `rtk ./gradlew check-cache --dry-run` and verify the task is registered exactly once. Run `rtk ./gradlew check --dry-run` and verify its task graph contains no `check-cache` task.

- [x] **Step 3: Run the root build check and commit Task 3**

  Run: `rtk ./gradlew check`

  Expected: root checks pass, and no fetcher task runs as part of the `check` task graph.

  Commit: `refactor: isolate specification fetching from generators`

### Task 4: Add CI coverage and verify live retrieval

**Files:**
- Modify: `.github/workflows/test.yml`
- Modify if upstream content differs: `webgpu-ktypes-specifications/src/jvmMain/resources/webgpu.html`
- Modify if upstream content differs: `webgpu-ktypes-specifications/src/jvmMain/resources/webgpu.idl`
- Modify: `webgpu-ktypes-specifications/src/jvmMain/resources/cache.json` (successful explicit checks refresh checked-at timestamps)
- Modify only if needed: `build-logic` tests or plugin wiring from Tasks 1–3.

**Interfaces:**
- CI runs `./gradlew check` and `./gradlew -p build-logic check` on the existing OS matrix.
- Public URL requests remain confined to the manual `check-cache` task; automated tests use loopback HTTP servers.

- [x] **Step 1: Add the included-build check to the existing CI workflow**

  Add a separate “Build logic checks” step that runs `./gradlew -p build-logic check`; retain the existing root `./gradlew check` step.

- [x] **Step 2: Run all automated checks**

  Run: `rtk ./gradlew -p build-logic check`

  Expected: plugin unit and TestKit tests pass on the current platform.

  Run: `rtk ./gradlew check`

  Expected: root project check passes without invoking `check-cache`.

- [x] **Step 3: Run the explicit live refresh and inspect the result**

  Run: `rtk ./gradlew check-cache`

  Expected: both public sources are requested; output identifies which files changed; `cache.json` has exactly one record per source with hashes matching the resulting files; no refresh temp files remain.

- [x] **Step 4: Review the final diff and commit Task 4**

  Run: `rtk git diff --check` and `rtk git status --short`.

  Expected: no whitespace errors; only the intended plugin, migration, test, CI, and live cache/resource changes appear. Review any upstream source diff as a deliberate refreshed snapshot before including it.

  Commit: `test: verify WebGPU specification refresh`
