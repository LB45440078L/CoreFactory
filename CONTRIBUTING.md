# Contributing

Thanks for wanting to help. CoreFactory is licensed under the [PolyForm Strict License](LICENSING.md), which does not allow modified copies or redistribution. For that reason, **contributions are by invitation only**.

## Before you start

1. **Open an issue first.** Use the [bug report](.github/ISSUE_TEMPLATE/bug_report.yml) or [feature request](.github/ISSUE_TEMPLATE/feature_request.yml) template. Wait for a reply before writing code.
2. **Do not send a pull request for an unrequested change.** Unrequested pull requests are closed without review.

## Submitting a contribution

Only after an issue is accepted:

1. Fork the repository and create a branch for one change.
2. Keep the change focused. One feature or fix per pull request.
3. Follow the existing style. Match the Javadoc, naming and layout of the code around your change.
4. Add or update tests. Core logic is tested without a server, and the Paper module is tested the same way.
5. Run the full build: `mvn -B -q test` from the repository root. Check the `target/surefire-reports` folders for failures.
6. Update the documentation in `docs/` and the YAML comments when behaviour or configuration changes.
7. Fill in the [pull request template](.github/PULL_REQUEST_TEMPLATE.md).

## Rules

- No AI-generated text in code, comments, docs or commit messages without disclosure. Say so in the pull request.
- Do not commit build output, server data, logs, or anything that contains personal information or credentials.
- Do not change the licence, the copyright headers or the `LICENSING.md` page.
- Be respectful in issues and reviews.

## Licence of contributions

By submitting a contribution, you agree that it is licensed under the same PolyForm Strict License 1.0.0, and that the copyright holder may use it without restriction. See [LICENSING.md](LICENSING.md).
