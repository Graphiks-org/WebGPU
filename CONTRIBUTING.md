# Contributing to WebGPU

This guide takes a change from a fresh branch to a reviewed pull request (PR). Read the [Code of Conduct](CODE_OF_CONDUCT.md) first. Use [Support](SUPPORT.md) for usage questions and the [private security route](SECURITY.md) for vulnerabilities.

## 1. Prepare your checkout

Install JDK 25 and use the repository's Gradle wrapper. **Every contribution starts from your own fork** of [Graphiks-org/WebGPU](https://github.com/Graphiks-org/WebGPU), including contributions from organization members. Fork the repository on GitHub, then clone your fork:

```sh
git clone https://github.com/<your-account>/<your-fork>.git
cd <your-fork>
git remote add upstream https://github.com/Graphiks-org/WebGPU.git
git fetch upstream
git switch -c feat/short-description upstream/master
```

Allowed branch prefixes are **only** `feat/`, `fix/`, and `chore/`; replace `feat/short-description` with the appropriate name. Do not create contribution branches in the primary repository. Before starting a substantial API or behavior change, open an issue to agree on the approach.

## 2. Make a focused change

Keep the PR limited to one purpose. Use the [architecture guide](docs/docs/architecture.md) to find the right module and the [type mapping](docs/docs/type-mapping/index.md) for WebGPU-to-Kotlin decisions. Add or update tests for behavior you change. For a user-visible change, update the relevant guide and add a short entry to the [Unreleased changelog](CHANGELOG.md).

The Kotlin bindings are generated from versioned WebGPU specification inputs. When changing them, update the inputs and generated source together. Follow the [specification maintenance guide](docs/docs/specification-maintenance.md) for the manual commands. Specification downloads, optional LLM enrichment, and binding regeneration are outside PR CI.

## 3. Verify locally

From the repository root, run the full business-test suite:

```sh
./gradlew check
```

Run focused platform tests for the code you touched; the [testing guide](docs/docs/testing.md) lists JVM, JS, Wasm JS, and ABI commands. Native tests depend on the host. Record any platform you could not test in the PR description.

For documentation changes, install MkDocs Material and `mkdocs-static-i18n` in a Python environment, then build the API reference and bilingual site:

```sh
python3 -m venv build/docs-venv
build/docs-venv/bin/python -m pip install mkdocs-material mkdocs-static-i18n
./gradlew :docs:embedDokkaIntoMkDocs
build/docs-venv/bin/mkdocs build --strict -f docs/mkdocs.yml
```

The example uses macOS/Linux paths; on Windows, use executables in `build/docs-venv/Scripts/`. Gradle compacts the generated API navigation automatically. Check `git status --short` before committing: generated files and the site output should not be included.

## 4. Commit and open a PR

Use a Conventional Commit subject for **every non-merge commit** and for the PR title. For example, `feat(api): add texture option` or `docs: clarify setup`. The allowed types are `feat`, `fix`, `build`, `chore`, `ci`, `docs`, `perf`, `refactor`, `test`, and `style`. Choose an optional scope from the current project responsibilities:

| Scope | Responsibility |
| --- | --- |
| `api` | Portable API in `webgpu-api` |
| `descriptors` | Descriptor implementations in `webgpu-descriptors` |
| `web` | Browser interop in `webgpu-web-bindings` and the `webgpu-browser` implementation |
| `specifications` | Versioned WebGPU inputs and maintenance tasks |
| `buildSrc` | Shared Gradle conventions |
| `build-logic` | Included Gradle build logic |
| `ci` | GitHub workflows and validation scripts |
| `docs` | Site guides and repository documentation |
| `release` | Versioning and Maven publication |
| `deps` | Automated dependency updates (reserved for Dependabot) |

Keep this table aligned with the [machine-readable PR policy](.github/contributing-policy.toml) when modules change. Commit your focused change first:

```sh
git add <changed-files>
git commit -m "feat(api): add texture option"
```

Replace `<changed-files>` with the paths you intend to submit. Before pushing, include the latest `master` commit in your branch; the PR policy checks this ancestry:

```sh
git fetch upstream master
git merge --no-edit upstream/master
./gradlew check
git push -u origin feat/short-description
```

Resolve any merge conflicts and rerun relevant tests before continuing. Check `git status --short` before pushing.

Open a PR **from your fork** against `Graphiks-org/WebGPU`'s `master` branch. Fill in the [PR template](.github/PULL_REQUEST_TEMPLATE.md): describe the behavior and tests, select **one** change type, select **one** documentation decision, and select **one** changelog decision. If an update is unnecessary, explain it in Description using `Documentation: ...` or `Changelog: ...`. The PR title type must match the selected type. Wait for the **PR policy** check, business tests, and documentation build to pass. Address failures and review comments with additional Conventional Commits, and update your branch from `upstream/master` if it advances.

Automated dependency updates are exempt from the fork, branch-prefix, and template rules: the PR policy accepts a pull request authored by `dependabot[bot]` on a `dependabot/` branch when its title and commits stay Conventional Commits with the reserved `deps` scope — for example `build(deps): bump ktor from 3.5.0 to 3.6.0` — and its head includes the current `master` commit. Documentation and changelog decisions are not required for these automated updates; maintainers fold notable dependency changes into the release notes.

## Maintainer publication

1. Confirm the PR has merged and the required checks have passed. Configure Pages with **GitHub Actions** as its source and allow the `github-pages` environment. Configure the Actions secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, and `SIGNING_PASSWORD`.
2. A push to `master` builds the site and publishes the default `0.1.0-SNAPSHOT` Maven artifacts. The Pages deployment job fails visibly if its repository settings are missing. To rerun documentation publication, dispatch the **Documentation** workflow from `master`.
3. To publish a different snapshot, dispatch **Publish to Maven Central** from `master`. Leave `version` empty for Gradle's current default or enter a version ending in `-SNAPSHOT`.
4. For a release, update [CHANGELOG.md](CHANGELOG.md), choose the release version, ensure `master` is current, then push a non-snapshot `v*` tag such as `v1.0.0`:

   ```sh
   git switch master
   git pull --ff-only upstream master
   git tag v1.0.0
   git push upstream v1.0.0
   ```

The Maven workflow runs `check` before publishing `webgpu-api`, `webgpu-descriptors`, `webgpu-web-bindings`, and `webgpu-browser`. Empty release tags and versions ending in `-SNAPSHOT` are rejected before upload. Only maintainers should initiate a release tag or manual publication.
