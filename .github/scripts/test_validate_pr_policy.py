#!/usr/bin/env python3
"""Unit tests for the WebGPU pull request policy validator."""

import pathlib
import tomllib
import unittest

import validate_pr_policy as validator

POLICY_PATH = pathlib.Path(__file__).resolve().parent.parent / "contributing-policy.toml"


def load_policy() -> dict:
    with POLICY_PATH.open("rb") as stream:
        return tomllib.load(stream)


HUMAN_BODY = """## Description

Wrap borrowed native memory on Android and verify it with the ABI checks.

Documentation: not needed, the Android wrap stays inside the existing guides.
Changelog: not needed, the release notes already cover the buffer work.

## Type of Change

- [ ] `feat` — new feature
- [x] `fix` — bug fix

## Checklist

- [x] I ran `./gradlew check` or explained why it could not run in Description.
- [x] I ran relevant platform tests or explained the gap in Description.
- [ ] Documentation updated for a user-visible change.
- [x] No documentation change needed, with reason in Description.
- [ ] `CHANGELOG.md` updated for a user-visible change.
- [x] No changelog needed, with reason in Description.

## Screenshots (if applicable)

None.

## Additional Notes

None.
"""


def human_pr(**overrides):
    """Return the keyword arguments of a valid human contribution."""
    arguments = dict(
        title="fix(api): wrap borrowed native memory",
        body=HUMAN_BODY,
        branch="fix/android-borrowed-memory",
        commit_subjects=["fix(api): wrap borrowed native memory"],
        changed_files=["webgpu-api/src/commonMain/kotlin/ArrayBuffer.kt"],
        policy=load_policy(),
        base_ancestor=True,
        head_repository="alice-dev/WebGPU",
        base_repository="Graphiks-org/WebGPU",
        head_is_fork=True,
        author="alice-dev",
    )
    arguments.update(overrides)
    return arguments


def dependabot_pr(**overrides):
    """Return the keyword arguments of a valid automated dependency update."""
    arguments = dict(
        title="build(deps): bump ktor from 3.5.0 to 3.6.0",
        body="",
        branch="dependabot/gradle/io.ktor-ktor-3.6.0",
        commit_subjects=["build(deps): bump ktor from 3.5.0 to 3.6.0"],
        changed_files=["gradle/libs.versions.toml"],
        policy=load_policy(),
        base_ancestor=True,
        head_repository="Graphiks-org/WebGPU",
        base_repository="Graphiks-org/WebGPU",
        head_is_fork=False,
        author="dependabot[bot]",
    )
    arguments.update(overrides)
    return arguments


class HumanPullRequestPolicyTest(unittest.TestCase):
    def test_valid_human_pull_request_passes(self):
        errors = validator.validate_pr(**human_pr())
        self.assertEqual(errors, [])

    def test_human_pull_request_must_come_from_a_fork(self):
        errors = validator.validate_pr(**human_pr(head_is_fork=False))
        self.assertIn(
            "Pull request head must come from a fork of the base repository.", errors
        )


class DependencyUpdatePullRequestPolicyTest(unittest.TestCase):
    def test_valid_dependabot_pull_request_passes_without_template(self):
        errors = validator.validate_pr(**dependabot_pr())
        self.assertEqual(errors, [])

    def test_dependabot_pull_request_still_requires_master_ancestor(self):
        errors = validator.validate_pr(**dependabot_pr(base_ancestor=False))
        self.assertEqual(
            errors,
            ["Current base commit must be an ancestor of the pull request head."],
        )

    def test_dependabot_pull_request_title_must_be_conventional(self):
        errors = validator.validate_pr(
            **dependabot_pr(title="bump ktor from 3.5.0 to 3.6.0")
        )
        self.assertEqual(
            errors, ["PR title must be a Conventional Commit subject."]
        )

    def test_dependabot_pull_request_scope_stays_restricted(self):
        errors = validator.validate_pr(
            **dependabot_pr(title="build(api): bump ktor from 3.5.0 to 3.6.0")
        )
        self.assertEqual(errors, ["PR title uses an unknown scope: api."])

    def test_dependabot_pull_request_commits_must_be_conventional(self):
        errors = validator.validate_pr(
            **dependabot_pr(commit_subjects=["bump ktor from 3.5.0 to 3.6.0"])
        )
        self.assertEqual(
            errors, ["Commit 1 must be a Conventional Commit subject."]
        )


class DependencyUpdateLaneDetectionTest(unittest.TestCase):
    def test_bot_branch_without_bot_author_gets_full_policy(self):
        errors = validator.validate_pr(
            **dependabot_pr(
                author="alice-dev",
                head_is_fork=True,
                head_repository="alice-dev/WebGPU",
            )
        )
        self.assertIn(
            "Branch must use an allowed prefix and a nonempty name.", errors
        )
        self.assertIn("Heading 'Description' must appear exactly once.", errors)

    def test_bot_author_without_bot_branch_gets_full_policy(self):
        errors = validator.validate_pr(
            **dependabot_pr(
                branch="chore/sneaky",
                head_is_fork=True,
                head_repository="alice-dev/WebGPU",
            )
        )
        self.assertIn("Heading 'Description' must appear exactly once.", errors)


if __name__ == "__main__":
    unittest.main()
