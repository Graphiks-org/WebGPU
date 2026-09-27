# Contributing to WebGPU

Thanks for helping improve WebGPU. Read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating. For usage questions, see [Support](SUPPORT.md); report vulnerabilities through the [private security route](SECURITY.md).

## Prepare a change

Open an issue for substantial API or behavior changes so the approach can be discussed. Branch from `master`; use `feat/`, `fix/`, `chore/`, or `codex/` as appropriate. Keep each pull request focused and describe observable behavior. The [architecture](docs/docs/architecture.md), [testing](docs/docs/testing.md), and [specification maintenance](docs/docs/specification-maintenance.md) guides explain the project boundaries.

Use Conventional Commits such as `feat: add texture option` or `fix: correct buffer offset`. Kotlin bindings come from checked-in WebGPU specifications; update generated source and its inputs together when changing those bindings. Specification downloads and LLM enrichment are manual activities.

## Verify

Use JDK 25 and run `./gradlew check`. Run the relevant platform tests for your change and mention any target you could not exercise. For documentation changes, build the site with `./gradlew :docs:embedDokkaIntoMkDocs` and `mkdocs build --strict -f docs/mkdocs.yml` after installing MkDocs Material and `mkdocs-static-i18n`. The Gradle task compacts generated API navigation automatically; check that `docs/mkdocs.yml` remains unchanged before committing.

The documentation workflow validates every pull request. Publication from `master` requires repository Pages settings with **GitHub Actions** selected as the source and the `github-pages` environment allowed. If these settings are missing, the deployment job fails and a maintainer must enable them.

## Submit a pull request

Use the [pull request template](.github/PULL_REQUEST_TEMPLATE.md). Select one change type, explain testing, and state whether the changelog and documentation were updated or why they are unnecessary. Add an entry under [Unreleased](CHANGELOG.md) for a user-visible change. A maintainer reviews the request and may ask for changes before merging into `master`.
