import importlib.util
import pathlib
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[3]
SCRIPT = ROOT / ".github" / "scripts" / "validate_pr_policy.py"
spec = importlib.util.spec_from_file_location("validate_pr_policy", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

POLICY = {
    "types": ["feat", "fix", "build", "chore", "ci", "docs", "perf", "refactor", "test", "style"],
    "scopes": ["api", "descriptors", "web", "specifications", "buildSrc", "build-logic", "ci", "docs", "release"],
    "branch_prefixes": ["feat/", "fix/", "chore/", "codex/"],
    "required_sections": ["Description", "Type of Change", "Checklist", "Screenshots (if applicable)", "Additional Notes"],
    "changelog_file": "CHANGELOG.md",
}

BODY = """## Description

Add a portable API. Changelog: entry added.

## Type of Change

- [x] `feat` — new feature
- [ ] `fix` — bug fix

## Checklist

- [x] Documentation updated for a user-visible change.
- [ ] No documentation change needed, with reason in Description.
- [x] `CHANGELOG.md` updated for a user-visible change.
- [ ] No changelog needed, with reason in Description.

## Screenshots (if applicable)

None.

## Additional Notes

None.
"""


class PolicyTests(unittest.TestCase):
    def validate(self, title="feat(api): add texture option", body=BODY,
                 branch="codex/texture-option", commits=None, files=None):
        if commits is None:
            commits = ["feat(api): add texture option"]
        if files is None:
            files = ["CHANGELOG.md", "webgpu-api/src/commonMain/Foo.kt"]
        return module.validate_pr(title, body, branch, commits, files, POLICY)

    def test_valid_codex_branch_and_scoped_title(self):
        self.assertEqual([], self.validate())

    def test_invalid_title(self):
        self.assertTrue(any("title" in e.lower() for e in self.validate(title="Add texture option")))

    def test_disallowed_branch(self):
        self.assertTrue(any("branch" in e.lower() for e in self.validate(branch="feature/texture")))

    def test_missing_heading(self):
        self.assertTrue(any("heading" in e.lower() for e in self.validate(body=BODY.replace("## Additional Notes", "## Other"))))

    def test_no_change_type(self):
        self.assertTrue(any("type" in e.lower() for e in self.validate(body=BODY.replace("- [x] `feat`", "- [ ] `feat`"))))

    def test_two_change_types(self):
        self.assertTrue(any("type" in e.lower() for e in self.validate(body=BODY.replace("- [ ] `fix`", "- [x] `fix`"))))

    def test_no_changelog_decision(self):
        self.assertTrue(any("changelog" in e.lower() for e in self.validate(body=BODY.replace("- [x] `CHANGELOG.md`", "- [ ] `CHANGELOG.md`"))))

    def test_two_changelog_decisions(self):
        self.assertTrue(any("changelog" in e.lower() for e in self.validate(body=BODY.replace("- [ ] No changelog", "- [x] No changelog"))))

    def test_no_changelog_requires_reason(self):
        body = BODY.replace("Changelog: entry added.", "No entry.").replace("- [x] `CHANGELOG.md`", "- [ ] `CHANGELOG.md`").replace("- [ ] No changelog", "- [x] No changelog")
        self.assertTrue(any("reason" in e.lower() for e in self.validate(body=body, files=[])))

    def test_no_documentation_decision(self):
        body = BODY.replace("- [x] Documentation updated", "- [ ] Documentation updated")
        self.assertTrue(any("documentation" in e.lower() for e in self.validate(body=body)))

    def test_two_documentation_decisions(self):
        body = BODY.replace("- [ ] No documentation", "- [x] No documentation")
        self.assertTrue(any("documentation" in e.lower() for e in self.validate(body=body)))

    def test_no_documentation_requires_reason(self):
        body = BODY.replace("- [x] Documentation updated", "- [ ] Documentation updated").replace("- [ ] No documentation", "- [x] No documentation")
        self.assertTrue(any("reason" in e.lower() for e in self.validate(body=body)))

    def test_invalid_authored_commit(self):
        self.assertTrue(any("commit" in e.lower() for e in self.validate(commits=["update stuff"])))


if __name__ == "__main__":
    unittest.main()
