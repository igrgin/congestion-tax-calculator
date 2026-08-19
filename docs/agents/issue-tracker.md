# GitHub workflow

Issues and specifications live in the [GitHub repository](https://github.com/igrgin/congestion-tax-calculator). Use the `gh` CLI for issue operations.

## Planning

- Use one parent issue for a specification.
- Use vertical-slice sub-issues for implementation work.
- Record blocking relationships in GitHub.
- Keep the parent issue status table current.
- Do not use triage labels or a GitHub Project board.

Use `grill-with-docs` when an issue needs design decisions. Use `to-spec` for a new specification and `to-tickets` for its implementation issues.

## Implementation

1. Assign the issue to `igrgin` after its blockers are closed.
2. Create a branch named `<issue-number>-<short-description>`.
3. Implement and verify the issue on that branch.
4. Use small commits and include the issue number in each commit message.
5. Push the branch and open a pull request into `main`.
6. Wait for required GitHub Actions checks and user approval.
7. Merge with a normal merge commit.
8. Comment on the issue with the completed work, then close it.

The user owns pull-request review and merge approval.

## Completion

An issue is complete when its behavior works, `./mvnw verify` passes, affected documents are current, and the pull request is approved and merged.
