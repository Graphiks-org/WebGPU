#!/usr/bin/env python3
"""Validate a WebGPU pull request from local metadata files."""

import argparse
import pathlib
import re
import tomllib


SUBJECT = re.compile(r"^(?P<type>[a-z]+)(?:\((?P<scope>[^()]+)\))?!?: .+\S$")
HEADING = re.compile(r"^## (.+)$", re.MULTILINE)
CHECKBOX = re.compile(r"^- \[([ xX])\] (.+)$", re.MULTILINE)


def _subject_error(subject: str, policy: dict) -> str | None:
    match = SUBJECT.fullmatch(subject.strip())
    if match is None:
        return "must be a Conventional Commit subject"
    if match.group("type") not in policy["types"]:
        return f"uses an unknown type: {match.group('type')}"
    scope = match.group("scope")
    if scope is not None and scope not in policy["scopes"]:
        return f"uses an unknown scope: {scope}"
    return None


def _section(body: str, heading: str) -> str:
    match = re.search(rf"^## {re.escape(heading)}\s*$", body, re.MULTILINE)
    if match is None:
        return ""
    end = re.search(r"^## ", body[match.end():], re.MULTILINE)
    return body[match.end():match.end() + end.start()] if end else body[match.end():]


def validate_pr(title: str, body: str, branch: str,
                commit_subjects: list[str], changed_files: list[str], policy: dict) -> list[str]:
    """Return every policy violation; no network or GitHub API is used."""
    errors = []
    title_error = _subject_error(title, policy)
    if title_error:
        errors.append(f"PR title {title_error}.")
    if not any(branch.startswith(prefix) and len(branch) > len(prefix)
               for prefix in policy["branch_prefixes"]):
        errors.append("Branch must use an allowed prefix and a nonempty name.")

    headings = HEADING.findall(body)
    for heading in policy["required_sections"]:
        if headings.count(heading) != 1:
            errors.append(f"Heading '{heading}' must appear exactly once.")

    type_choices = [(mark, text) for mark, text in CHECKBOX.findall(_section(body, "Type of Change"))
                    if re.match(r"`[a-z]+`", text)]
    selected_types = [re.match(r"`([^`]+)`", text).group(1)
                      for mark, text in type_choices if mark.lower() == "x"]
    if len(selected_types) != 1 or selected_types[0] not in policy["types"]:
        errors.append("Type of Change must select exactly one allowed type.")
    elif title_error is None and selected_types[0] != SUBJECT.fullmatch(title.strip()).group("type"):
        errors.append("Type of Change must match the PR title type.")

    checklist = CHECKBOX.findall(_section(body, "Checklist"))
    documentation = {
        "updated": [mark for mark, text in checklist if text.startswith("Documentation updated")],
        "unneeded": [mark for mark, text in checklist if text.startswith("No documentation change needed")],
    }
    documentation_selected = [key for key, marks in documentation.items()
                              if any(mark.lower() == "x" for mark in marks)]
    if any(len(marks) != 1 for marks in documentation.values()) or len(documentation_selected) != 1:
        errors.append("Documentation checklist must select exactly one decision.")
    elif documentation_selected[0] == "unneeded" and not re.search(
            r"(?im)^\s*Documentation:\s*\S.{2,}$", _section(body, "Description")):
        errors.append("No documentation change needs a reason in Description as 'Documentation: ...'.")

    decisions = {
        "updated": [mark for mark, text in checklist
                    if text.startswith(f"`{policy['changelog_file']}` updated")],
        "unneeded": [mark for mark, text in checklist if text.startswith("No changelog needed")],
    }
    selected = [key for key, marks in decisions.items() if any(mark.lower() == "x" for mark in marks)]
    if any(len(marks) != 1 for marks in decisions.values()) or len(selected) != 1:
        errors.append("Changelog checklist must select exactly one decision.")
    elif selected[0] == "updated" and policy["changelog_file"] not in changed_files:
        errors.append(f"Changelog is marked updated but {policy['changelog_file']} is absent from changed files.")
    elif selected[0] == "unneeded":
        description = _section(body, "Description")
        if not re.search(r"(?im)^\s*Changelog:\s*\S.{2,}$", description):
            errors.append("No changelog needed requires a reason in Description as 'Changelog: ...'.")

    for index, subject in enumerate(commit_subjects, 1):
        commit_error = _subject_error(subject, policy)
        if commit_error:
            errors.append(f"Commit {index} {commit_error}.")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy", type=pathlib.Path, required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--body-file", type=pathlib.Path, required=True)
    parser.add_argument("--branch", required=True)
    parser.add_argument("--changed-files-file", type=pathlib.Path, required=True)
    parser.add_argument("--commit-subjects-file", type=pathlib.Path, required=True)
    args = parser.parse_args()

    with args.policy.open("rb") as stream:
        policy = tomllib.load(stream)
    body = args.body_file.read_text(encoding="utf-8")
    changed_files = args.changed_files_file.read_text(encoding="utf-8").splitlines()
    commits = args.commit_subjects_file.read_text(encoding="utf-8").splitlines()
    errors = validate_pr(args.title, body, args.branch, commits, changed_files, policy)
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        return 1
    print("Pull request policy passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
