# WebGPU Documentation and Maven Publication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish WebGPU docs through GitHub Pages and align Maven Central publication with Kalligraphie's working credential model.

**Architecture:** One GitHub workflow builds docs on PRs and deploys from `master`; another keeps existing snapshot/tag release triggers but uses current WebGPU modules and Kalligraphie's exact secret mapping. The POM and README identify `Graphiks-org/WebGPU`. Actual deployment remains controlled by GitHub repository settings and secrets.

**Tech Stack:** GitHub Actions, Gradle 9.5.0, Vanniktech Maven Publish 0.36.0, Dokka/MkDocs from the docs plan, Maven Central, GitHub Pages.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-repository-alignment-design.md`

## Global Constraints

- Base on WebGPU `master` commit `762c250` or descendants; group `org.graphiks`; publish `webgpu-api`, `webgpu-descriptors`, and `webgpu-web`.
- Use GitHub secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD` exactly as Kalligraphie does; remove references to `SONATYPE_LOGIN`, `SONATYPE_PASSWORD`, `PGP_PRIVATE`, `PGP_PASSPHRASE`.
- No push, release tag, Maven upload, or Pages deployment occurs while implementing or verifying locally.
- Neither publication path invokes specification fetching, the LLM task, or binding regeneration.

## Review Focus

- A PR should build docs without deploying; Task 1 checks job conditions and the absence of Pages write permission in its build job.
- Missing Pages enablement should fail visibly in the deployment job; Task 1 documents the required repository setting and environment.
- A manual snapshot override without `-SNAPSHOT` must fail before upload; Task 2 tests the validation command.
- A tag `v` or `v1.0.0-SNAPSHOT` must fail before upload; Task 2 tests both cases.
- A release with Kalligraphie's four configured secrets must map each to the expected Gradle property; Task 2 inspects all eight job mappings.

---

### Task 1: Documentation validation and Pages deployment

**Files:**
- Create: `.github/workflows/docs.yml`
- Modify: `CONTRIBUTING.md` or `docs/docs/getting-started.md` from the documentation plan to state the Pages prerequisite

**Interfaces:**
- Consumes: `:docs:embedDokkaIntoMkDocs`, `docs/mkdocs.yml`, and `_site/` from `2026-09-27-webgpu-docs-community.md`.
- Produces: docs validation on every PR, Pages deployment after a successful `master` build, and manual dispatch.

- [ ] **Step 1: Confirm the local build inputs.** Run `./gradlew :docs:embedDokkaIntoMkDocs` followed by `mkdocs build --strict -f docs/mkdocs.yml`; expect `_site/` and no tracked-file changes.
- [ ] **Step 2: Add docs workflow.** On `pull_request`, `push` to `master`, and `workflow_dispatch`, install JDK 25 and Python 3, MkDocs Material, and `mkdocs-static-i18n`; run the two Step 1 commands. Give the build job `contents: read`. Upload `_site/` as a Pages artifact only when `github.ref == 'refs/heads/master'`; a manual dispatch from another ref validates but does not deploy. Put `pages: write` and `id-token: write` only on the conditional deployment job, which needs the build job and uses `actions/deploy-pages@v4`.
- [ ] **Step 3: Document external setup.** State that Pages must use GitHub Actions as source and permit the `github-pages` environment; report that absent settings cause the deployment job to fail and require maintainer action. Do not create or change repository settings in this task.
- [ ] **Step 4: Validate the workflow.** Check that PR conditions run the build but skip upload/deploy; pushes and manual dispatches on `master` can deploy. Run `git diff --check` and the local Step 1 build again; expect success.
- [ ] **Step 5: Commit.** `git add .github/workflows/docs.yml CONTRIBUTING.md docs/docs/getting-started.md && git commit -m "ci: validate and deploy WebGPU documentation"` (stage only the guidance file actually edited).

### Task 2: Maven Central release workflow and POM identity

**Files:**
- Modify: `.github/workflows/publish.yml`, `buildSrc/src/main/kotlin/publish.gradle.kts`
- Read: `build.gradle.kts`, `settings.gradle.kts`, `README.md`

**Interfaces:**
- Consumes: current `org.graphiks` Gradle group and `0.1.0-SNAPSHOT` default, three renamed published modules, and existing Gradle `check`.
- Produces: snapshot on `master`/manual dispatch and release on `v*` tags, with these exact mappings in both jobs:

| Gradle environment variable | GitHub secret |
| --- | --- |
| `ORG_GRADLE_PROJECT_mavenCentralUsername` | `MAVEN_CENTRAL_USERNAME` |
| `ORG_GRADLE_PROJECT_mavenCentralPassword` | `MAVEN_CENTRAL_PASSWORD` |
| `ORG_GRADLE_PROJECT_signingInMemoryKey` | `SIGNING_KEY` |
| `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` | `SIGNING_PASSWORD` |

- [ ] **Step 1: Record the red checks.** Inspect current `publish.yml` for the four old GitHub secret references and stale `0.0.10-SNAPSHOT`; inspect the generated POM or `publish.gradle.kts` for the old `wgpu4k/webgpu-ktypes` URL. These are the inputs to replace.
- [ ] **Step 2: Update POM identity.** Set POM URL, SCM connection, developer connection, and SCM URL to `https://github.com/Graphiks-org/WebGPU` / `.git` as appropriate. Preserve MIT license and developer attribution. Run `./gradlew :webgpu-api:generatePomFileForKotlinMultiplatformPublication` and inspect the generated POM for the new URLs and `org.graphiks` group.
- [ ] **Step 3: Update snapshot workflow.** Preserve `master` and manual triggers. For an empty manual version input, omit `-PreleaseVersion` so Gradle uses its own `0.1.0-SNAPSHOT` default; for a supplied override, require a nonempty `*-SNAPSHOT` value and pass it as `-PreleaseVersion`. Build/test before upload. Publish the three named modules, with the four exact mappings above. Make the job fail before upload when any required secret is empty.
- [ ] **Step 4: Update release workflow.** For a `v*` tag, strip `v`, require a nonempty release version that does not end with `-SNAPSHOT`, and pass it as `-PreleaseVersion`. Build/test before upload, publish the three modules, and use the same four secret mappings and pre-upload presence check.
- [ ] **Step 5: Validate without publishing.** Exercise the version-resolution shell snippets with empty/manual/release inputs, including rejected `1.0.0`, `v`, and `v1.0.0-SNAPSHOT` cases where relevant. Run `./gradlew :webgpu-api:publishToMavenCentral :webgpu-descriptors:publishToMavenCentral :webgpu-web:publishToMavenCentral -PsigningInMemoryKey=dry-run --dry-run`; the harmless property value registers publishing tasks, and `--dry-run` prevents signing or upload. Expect tasks to resolve. Inspect workflow for zero old-secret references and all four new mappings in both jobs. Run `git diff --check`.
- [ ] **Step 6: Commit.** `git add .github/workflows/publish.yml buildSrc/src/main/kotlin/publish.gradle.kts && git commit -m "ci: align WebGPU Maven publication with Kalligraphie"`.
