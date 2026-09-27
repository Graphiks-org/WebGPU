# `org.graphiks` Namespace Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move this project's public packages and published Maven group from `io.ygdrasil` to `org.graphiks`.

**Architecture:** Keep module names and repository identity as they are. Update first-party Kotlin packages and generated code, Gradle publication/plugin metadata, Android namespaces, and ABI snapshots. Preserve the separate native-specs dependency under its existing group.

**Tech Stack:** Kotlin Multiplatform, Gradle Kotlin DSL, Kotlin ABI validation, buildSrc generator, Gradle convention plugin.

**Spec:** `docs/superpowers/specs/2026-09-27-org-graphiks-namespace-design.md`

## Global Constraints

- First-party package prefix: `org.graphiks.webgpu`.
- First-party publication group: `org.graphiks`.
- Android namespaces: `org.graphiks.webgpu.ktypes` and `org.graphiks.webgpu.ktypes.descriptors`.
- Specification fetcher plugin ID: `org.graphiks.webgpu-specification-fetcher`.
- Preserve external dependency `io.ygdrasil:wgpu4k-native-specs-jvm` and its repository filter.
- Preserve repository URLs, root project name, and module names.
- Do not add or run tests in this task.

## Review Focus

- A generated Kotlin file still uses the old package; verify generator templates and checked-in generated files together.
- A module retains an old Android namespace; inspect both Android library modules.
- The plugin ID changes while its implementation class or package stays old; verify plugin metadata and implementation declaration together.
- ABI snapshots retain the old public package or KLIB unique name; scan all `webgpu-api/api` files.
- The unrelated native-specs dependency stops resolving; verify its catalog coordinate and Sonatype snapshot content filter are unchanged.

---

### Task 1: Migrate first-party Kotlin packages and generated output

**Files:**
- Modify: all first-party Kotlin files under `webgpu-api/src`, `webgpu-descriptors/src`, and `webgpu-web/src` that declare or import `io.ygdrasil.webgpu`.
- Modify: `buildSrc/src/main/kotlin/generator/mapper/Enumeration.kt`.
- Modify: `buildSrc/src/main/kotlin/generator/tasks/ModelWriter.kt`.
- Move: `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/*.kt` to `build-logic/src/main/kotlin/org/graphiks/webgpu/fetcher/`.
- Modify: the moved build-logic Kotlin files' package declarations.

**Interfaces:**
- Produces public package `org.graphiks.webgpu` for the API, descriptor, and web source sets.
- Produces build-logic implementation class `org.graphiks.webgpu.fetcher.WebGpuSpecificationFetcherPlugin`.

- [x] Replace first-party `io.ygdrasil.webgpu` package declarations and imports with `org.graphiks.webgpu` throughout the three library modules, including tests.
- [x] Update `Enumeration.kt`'s KotlinPoet `ClassName` to `org.graphiks.webgpu` and `ModelWriter.kt`'s emitted package strings to `org.graphiks.webgpu`.
- [x] Move the specification fetcher Kotlin files into the matching `org/graphiks/webgpu/fetcher` directory and update each package declaration.
- [x] Scan first-party Kotlin sources and generator templates for remaining `io.ygdrasil.webgpu`; expected result: no matches.

### Task 2: Update Gradle publication and Android/plugin namespaces

**Files:**
- Modify: `build.gradle.kts`.
- Modify: `webgpu-api/build.gradle.kts`.
- Modify: `webgpu-descriptors/build.gradle.kts`.
- Modify: `build-logic/build.gradle.kts`.
- Modify: root `build.gradle.kts` plugin declaration.
- Preserve: `buildSrc/build.gradle.kts` content filter and `gradle/libs.versions.toml` native-specs coordinate.

**Interfaces:**
- Produces Maven coordinates with group `org.graphiks` and module artifact names unchanged.
- Produces plugin ID `org.graphiks.webgpu-specification-fetcher` backed by `org.graphiks.webgpu.fetcher.WebGpuSpecificationFetcherPlugin`.

- [x] Set the root all-projects `group` to `org.graphiks`.
- [x] Change the plugin declaration and plugin registration ID to `org.graphiks.webgpu-specification-fetcher`, and align `implementationClass` with Task 1.
- [x] Change Android namespaces to `org.graphiks.webgpu.ktypes` and `org.graphiks.webgpu.ktypes.descriptors`.
- [x] Confirm the catalog entry `io.ygdrasil:wgpu4k-native-specs-jvm` and `includeGroup("io.ygdrasil")` snapshot repository filter are untouched.
- [x] Review the Gradle metadata together for matching group, plugin ID, implementation class, and Android namespaces.

### Task 3: Refresh ABI snapshots and run the namespace audit

**Files:**
- Modify: `webgpu-api/api/android/webgpu-api.api`.
- Modify: `webgpu-api/api/jvm/webgpu-api.api`.
- Modify: `webgpu-api/api/webgpu-api.klib.api`.

**Interfaces:**
- Produces ABI snapshots matching the `org.graphiks.webgpu` package and `org.graphiks:webgpu-api` KLIB identity.

- [x] Refresh the `webgpu-api` ABI snapshots from the migrated sources using the project's Kotlin ABI validation task; if the Gradle wrapper remains unavailable, update only the old namespace and KLIB unique-name entries in the checked-in snapshots and report that regeneration could not be confirmed. The wrapper could not create its cache lock, so the snapshots were updated from the compiler's mangling rules; ABI task regeneration remains unconfirmed.
- [x] Scan the repository for first-party `io.ygdrasil` package/group/plugin references; expected result: only the deliberately preserved external native-specs coordinate and its repository content filter remain.
- [x] Confirm the external coordinate and repository filter remain exact, and that repository URLs, root project name, and module names did not change.
