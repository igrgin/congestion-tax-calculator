# Issue Tracker: GitHub

Issues and specifications for this repository live in GitHub Issues:

https://github.com/igrgin/congestion-tax-calculator

Use the `gh` CLI for issue operations.

## Conventions

- Publish a specification as one parent issue.
- Publish implementation tickets as sub-issues of the specification.
- Use GitHub issue dependencies for blocking relationships.
- Do not create or apply triage labels.
- Do not treat pull requests as requests for new work.
- Do not use a GitHub Project board for this repository.
- Keep an issue and status summary table in the parent specification.

## Development Workflow

- After planning is complete, the user runs `to-spec` to publish the parent specification. The user then runs `to-tickets` to propose, review, and publish its vertical-slice sub-issues and native blocking relationships.
- The repository decision to use no triage labels overrides skill defaults that request a `ready-for-agent` label.
- Create tickets as vertical slices of the specification.
- Assign each ticket to the GitHub account `igrgin` before work starts.
- Create one branch for each ticket. Use `<issue-number>-<short-description>` in kebab case, with no namespace prefix.
- Each feature ticket owns the logs and metrics required by its behavior. Add a custom metric only when existing metrics cannot answer the feature's operational question. Follow `CONTRIBUTING.md` for event levels, safe context, and metric design.
- Implement and verify the ticket on its branch.
- Use small coherent commits and include the issue number in each commit message.
- When the user and agent agree that the ticket is complete, push the branch and open a pull request into `main`.
- GitHub Actions must run the required verification for each pull request.
- The user owns pull-request review. Merge only after required checks pass and the user approves the pull request. Preserve the useful branch history with a normal merge commit.
- After merge, add an issue comment that states what was completed, and close the ticket.
- Keep the parent specification's status summary current.

More than one issue can be active when its blocking issues are complete. Each active issue uses its own branch and pull request. Do not start an issue while any declared blocker remains open.

Use `grill-with-docs` when an issue needs more design work. Use `implement` when an issue is ready for development. The implementation workflow uses test-driven development at the agreed seams where practical, runs regular focused verification, runs the full test suite at the end, performs a code review, and commits to the current issue branch.

## Definition of Complete

The application is complete when all agreed assignment behavior is implemented, the application starts and works correctly, local and GitHub Actions tests pass, all documentation is correct and current, `README.md` and `questions.md` are complete, all pull requests are merged, and all implementation issues are closed.
