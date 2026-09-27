# WebGPU Business Tests and CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make existing WebGPU business tests and contribution rules dependable PR checks without adding binding-regeneration gates.

**Architecture:** Keep the current multiplatform test suites and run only Gradle tasks declared by the current build. A small Python validator owns PR policy; its TOML configuration matches the PR template from the documentation/community plan, and GitHub Actions calls that same validator.

**Tech Stack:** Gradle 9.5.0, Kotlin Multiplatform, GitHub Actions, Python 3.11 standard library (`argparse`, `tomllib`, `unittest`).

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-repository-alignment-design.md`

## Global Constraints

- Base on WebGPU `master` commit `762c250` or descendants; group `org.graphiks`; modules `webgpu-api`, `webgpu-descriptors`, `webgpu-specifications`, `webgpu-web`.
- Test business behavior and existing ABI compatibility; do not add golden regeneration, specification download, or LLM checks to PR CI.
- PR policy accepts `feat/`, `fix/`, `chore/`, and `codex/` branches and the PR headings/types in the documentation/community plan.
- Keep CI read-only on pull requests; run publication only in the publication plan.

## Review Focus

- A docs-only PR must still receive a PR-policy result; Task 2 checks the workflow trigger has no path filters.
- A `codex/` branch with a valid title must pass the naming rule; Task 2 adds a positive fixture.
- A PR that checks both changelog choices must fail; Task 2 adds a negative fixture.
- A PR from a fork must not need secrets; Task 1 checks that test jobs use read-only permissions and no credentials.
- CI must not call a task unavailable on its runner; Task 1 records `./gradlew tasks --all` results before editing the matrix.

---

### Task 1: Business-test workflow

**Files:**
- Modify: `.github/workflows/test.yml`
- Test: existing `webgpu-api/src/commonTest`, `webgpu-api/src/jvmTest`, `webgpu-api/src/nativeTest`, `webgpu-web/src/commonTest`, `webgpu-web/src/jsTest`, `webgpu-web/src/wasmJsTest`

**Interfaces:**
- Consumes: existing Gradle `check`, `:webgpu-api:checkKotlinAbi`, `:webgpu-api:jvmTest`, `:webgpu-api:jsNodeTest`, `:webgpu-api:wasmJsNodeTest`, `:webgpu-web:jsNodeTest`, `:webgpu-web:wasmJsNodeTest`, `:webgpu-api:linuxX64Test`, `:webgpu-api:mingwX64Test`, `:webgpu-api:iosSimulatorArm64Test` as confirmed by `tasks --all` on the 2026-09-27 baseline.
- Produces: one business-test check on each Linux, macOS, and Windows PR runner, plus public ABI validation on macOS.

- [ ] **Step 1: Record the current task inventory.** Run `./gradlew tasks --all --offline` and check that every Gradle task named in this task's Interfaces block still exists after rebasing. If a task vanished, use the newly declared equivalent and update this plan before changing CI.
- [ ] **Step 2: Run the baseline locally.** Run `./gradlew :webgpu-api:jvmTest :webgpu-api:checkKotlinAbi :webgpu-web:jsNodeTest :webgpu-web:wasmJsNodeTest`; record any pre-existing failure and repair only failures caused by this task's changes.
- [ ] **Step 3: Tighten `test.yml`.** Trigger on every PR and on pushes to `master`; keep Linux, macOS, Windows runner coverage with `fail-fast: false` and `contents: read`. Use one Gradle setup/cache mechanism. Run `./gradlew --no-daemon check` everywhere; explicitly invoke JS/Wasm Node tests on Linux and `:webgpu-api:checkKotlinAbi` on macOS if `check` does not already run them. Keep native tests with the hosts that declare them, without a job for an unsupported target. No secrets or network-dependent specification task may appear.
- [ ] **Step 4: Validate the workflow.** Run the Gradle commands that match the current host and `git diff --check`. Inspect the workflow task names against Step 1's inventory and its triggers/permissions against this plan. Expect successful business tests and no missing-task references.
- [ ] **Step 5: Commit.** `git add .github/workflows/test.yml && git commit -m "ci: verify WebGPU business tests across platforms"`.

### Task 2: PR policy validator and workflow

**Files:**
- Create: `.github/contributing-policy.toml`, `.github/scripts/validate_pr_policy.py`, `.github/scripts/tests/test_validate_pr_policy.py`, `.github/workflows/pr-policy.yml`
- Read: `.github/PULL_REQUEST_TEMPLATE.md`, `CONTRIBUTING.md`, `CHANGELOG.md`

**Interfaces:**
- Consumes: Task 4 of `2026-09-27-webgpu-docs-community.md` produces the five required PR headings, ten change types, and changelog choices.
- Produces: `validate_pr(title: str, body: str, branch: str, commit_subjects: list[str], changed_files: list[str], policy: dict) -> list[str]`, returning all rule errors; CLI exits 0 for an empty list and 1 after printing errors otherwise.

- [ ] **Step 1: Write failing validator tests.** Add `unittest` cases for valid `feat(api): ...` on `codex/...`, invalid title, disallowed branch, missing heading, zero/two selected change types, zero/two changelog decisions, missing reason for no changelog, and an invalid authored commit subject. Assert error categories, not full prose.
- [ ] **Step 2: Confirm red.** Run `python3 -m unittest discover -s .github/scripts/tests -v`; expect missing validator failures.
- [ ] **Step 3: Implement policy.** In TOML, define the exact allowed types above and WebGPU scopes (`api`, `descriptors`, `web`, `specifications`, `buildSrc`, `build-logic`, `ci`, `docs`, `release`), branch prefixes, required sections, and `CHANGELOG.md`. Implement `validate_pr` with Python 3.11 standard library, Conventional Commit subject checks, exact heading/checkbox parsing, and explicit changelog decision. Keep the validator free of GitHub API calls.
- [ ] **Step 4: Confirm green.** Run `python3 -m unittest discover -s .github/scripts/tests -v`; expect all fixtures to pass. Add a `--help` smoke check for the CLI arguments `--policy`, `--title`, `--body-file`, `--branch`, `--changed-files-file`, and `--commit-subjects-file`.
- [ ] **Step 5: Wire GitHub Actions.** Trigger `pr-policy.yml` on all pull requests to `master`, without path filters. Check out the PR head with full history, collect the base/head diff and non-merge commit subjects, write PR body to a file, and call the validator with those six arguments. Use `contents: read` only. Ensure the checked-out head script is treated as repository code and that no PR secrets are exposed.
- [ ] **Step 6: Validate and commit.** Run the unittest suite and `git diff --check`; inspect the workflow's CLI flags against `--help`, then `git add .github/contributing-policy.toml .github/scripts .github/workflows/pr-policy.yml && git commit -m "ci: enforce WebGPU pull request policy"`.
