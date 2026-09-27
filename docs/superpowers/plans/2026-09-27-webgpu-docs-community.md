# WebGPU Documentation and Community Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give WebGPU the bilingual documentation site, README, and contributor entry points approved in the spec.

**Architecture:** A `:docs` Gradle module embeds Dokka output in MkDocs Material. The root README is the short entry point; `TYPE_MAPPING.md` and the checked-in specification resources remain the technical sources. Community Markdown and GitHub templates state the same contribution contract consumed by the CI plan.

**Tech Stack:** Gradle 9.5.0, Kotlin 2.3.21, Dokka 2.2.0, `dev.opensavvy.dokka.mkdocs:dokka-mkdocs:0.6.3`, MkDocs Material, `mkdocs-static-i18n`, Markdown.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-repository-alignment-design.md`

## Global Constraints

- Base implementation on WebGPU `master` commit `762c250` or its descendants; group `org.graphiks`; modules `webgpu-api`, `webgpu-descriptors`, `webgpu-specifications`, `webgpu-web`.
- Keep specification fetching, LLM enrichment, and binding regeneration manual; preserve the exact `tranform-json-doc-to-yaml` task name.
- Keep `TYPE_MAPPING.md` canonical; do not maintain a second hand-edited type mapping.
- Document only verified commands, modules, contacts, and code examples.

## Review Focus

- A new contributor following README commands should reach existing Gradle tasks; Task 3 checks every command against `./gradlew tasks --all`.
- A French navigation link should resolve to a French page; Task 2 builds both locales and checks referenced paths.
- API generation should not alter tracked source or fetch a specification; Task 1 checks `git status` after the build.
- A type-mapping update should appear in the site without editing a duplicate; Task 1 checks the generated copy against `TYPE_MAPPING.md`.
- A security report should reach a real private route; Task 4 checks the repository advisory URL and all community links.

---

### Task 1: Buildable MkDocs and Dokka integration

**Files:**
- Modify: `settings.gradle.kts`, `buildSrc/build.gradle.kts`, `.gitignore`
- Create: `docs/build.gradle.kts`, `docs/mkdocs.yml`, `docs/docs/index.md`, `docs/docs/index.fr.md`

**Interfaces:**
- Consumes: Dokka already applied to the published Gradle modules by `buildSrc/src/main/kotlin/publish.gradle.kts`.
- Produces: `:docs:embedDokkaIntoMkDocs`; `docs/docs/generated/type-mapping.md` copied from root `TYPE_MAPPING.md`; site output `_site/`.

- [ ] **Step 1: Record the red check.** Run `./gradlew :docs:embedDokkaIntoMkDocs --offline` and `mkdocs build -f docs/mkdocs.yml`; expect missing project/configuration failures.
- [ ] **Step 2: Add the docs build.** Include `:docs` in settings; add `dokka-mkdocs:0.6.3` to `buildSrc`; apply `dev.opensavvy.dokka-mkdocs` in `docs/build.gradle.kts`. Wire the three public modules `:webgpu-api`, `:webgpu-descriptors`, and `:webgpu-web` to the embed task. Copy root `TYPE_MAPPING.md` to `docs/docs/generated/type-mapping.md` before MkDocs navigation is generated. Add `_site/` and generated docs paths to `.gitignore`. Install `mkdocs-material` and `mkdocs-static-i18n` for local site verification.
- [ ] **Step 3: Add the minimal site.** Configure MkDocs Material, English default and French locale via `mkdocs-static-i18n`, site URL `https://graphiks-org.github.io/WebGPU/`, repo URL `https://github.com/Graphiks-org/WebGPU`, and navigation for the two landing pages plus generated API and type mapping. Match Kalligraphie's Dokka navigation markers where the embed plugin requires them.
- [ ] **Step 4: Run the green check.** Run `./gradlew :docs:embedDokkaIntoMkDocs` then `mkdocs build -f docs/mkdocs.yml`; expect both to pass and `_site/` to exist. Compare generated type mapping with `TYPE_MAPPING.md` using `cmp`; verify `git status --short` shows only intentional source edits.
- [ ] **Step 5: Commit.** `git add settings.gradle.kts buildSrc/build.gradle.kts .gitignore docs && git commit -m "docs: add bilingual MkDocs and Dokka site"`.

### Task 2: WebGPU guides in both languages

**Files:**
- Create: `docs/docs/getting-started.md`, `docs/docs/getting-started.fr.md`, `docs/docs/architecture.md`, `docs/docs/architecture.fr.md`, `docs/docs/testing.md`, `docs/docs/testing.fr.md`, `docs/docs/specification-maintenance.md`, `docs/docs/specification-maintenance.fr.md`
- Modify: `docs/docs/index.md`, `docs/docs/index.fr.md`, `docs/mkdocs.yml`

**Interfaces:**
- Consumes: Task 1 site layout and `:docs:embedDokkaIntoMkDocs`.
- Produces: stable guide paths for README and contributor links; English/French navigation.

- [ ] **Step 1: Establish the red check.** Add the planned guide paths to `docs/mkdocs.yml`, then run `mkdocs build --strict -f docs/mkdocs.yml`; expect missing-page failures.
- [ ] **Step 2: Write the guides.** Cover published coordinates, the three public modules and specification module, source/target boundaries, `./gradlew check`, supported platform tasks, `TYPE_MAPPING.md`, and the six manual generation commands from the old README. Keep the local LLM server optional and explicitly outside CI. Explain that public Kotlin code is generated from checked-in WebGPU sources. Write a matched `.fr.md` page for each English page.
- [ ] **Step 3: Validate content.** Run `./gradlew :docs:embedDokkaIntoMkDocs` and `mkdocs build --strict -f docs/mkdocs.yml`; expect pass. Inspect both locale outputs for broken navigation and ensure no guide links to old `webgpu-ktypes-*` module paths.
- [ ] **Step 4: Commit.** `git add docs && git commit -m "docs: explain WebGPU usage testing and specification maintenance"`.

### Task 3: README as the repository entry point

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: Task 2 guide paths and current Gradle coordinates.
- Produces: a concise, linked README for consumers and contributors.

- [ ] **Step 1: Verify example and command inputs.** Confirm `GPUTextureSwizzle().toWebGpuString()` produces `rgba` in `webgpu-api`'s existing test, and inspect `./gradlew tasks --all` for README commands. Check that `org.graphiks:webgpu-api`, `org.graphiks:webgpu-descriptors`, and `org.graphiks:webgpu-web` match Gradle project names.
- [ ] **Step 2: Rewrite README.** Include purpose, status and CI/license badges, module table, Kotlin/Gradle dependency snippet using the `org.graphiks` group and a documented version placeholder, the verified swizzle example, supported targets, build/test/docs commands, and links to all guides and community files. Move the detailed generation sequence to `specification-maintenance.md`.
- [ ] **Step 3: Verify README.** Resolve each relative link locally and compare all commands and module names with the Gradle task/project inventory. Run the existing `:webgpu-api:jvmTest` that covers the example; expect pass.
- [ ] **Step 4: Commit.** `git add README.md && git commit -m "docs: make README the WebGPU entry point"`.

### Task 4: Community documents and GitHub templates

**Files:**
- Create: `CHANGELOG.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `CODE_OF_CONDUCT.fr.md`, `SECURITY.md`, `SECURITY.fr.md`, `SUPPORT.md`, `SUPPORT.fr.md`, `.github/ISSUE_TEMPLATE/bug_report.md`, `.github/ISSUE_TEMPLATE/feature_request.md`, `.github/ISSUE_TEMPLATE/config.yml`, `.github/PULL_REQUEST_TEMPLATE.md`

**Interfaces:**
- Consumes: Tasks 2 and 3 guide/README paths.
- Produces: PR headings `Description`, `Type of Change`, `Checklist`, `Screenshots (if applicable)`, `Additional Notes`; change types `feat`, `fix`, `build`, `chore`, `ci`, `docs`, `perf`, `refactor`, `test`, `style`; explicit changelog and documentation decisions for the CI policy validator.

- [ ] **Step 1: Establish a link/heading check.** List the required files and headings above; verify they are absent before adding them.
- [ ] **Step 2: Create the documents.** Adapt Kalligraphie's community content to `Graphiks-org/WebGPU`. Keep English and French versions of conduct, security and support; use GitHub private security advisories for vulnerability reports and the repository issues route for ordinary support. Start `CHANGELOG.md` with an Unreleased section; do not invent past releases. In `CONTRIBUTING.md`, specify `master`, `feat/`, `fix/`, `chore/`, `codex/`, `./gradlew check`, Conventional Commits, review and changelog expectations.
- [ ] **Step 3: Create templates.** Add bug and feature templates and a PR template with exactly the five headings and one-selectable change-type checklist. Include mutually exclusive `CHANGELOG.md updated` and `No changelog needed, with reason` options; include a docs decision.
- [ ] **Step 4: Validate.** Verify all internal links resolve and the advisory destination is the repository's private report route. Check template headings/types against the Task 4 Interfaces block and run `git diff --check`; expect no errors.
- [ ] **Step 5: Commit.** `git add CHANGELOG.md CONTRIBUTING.md CODE_OF_CONDUCT* SECURITY* SUPPORT* .github/ISSUE_TEMPLATE .github/PULL_REQUEST_TEMPLATE.md && git commit -m "docs: add WebGPU contributor and community templates"`.
