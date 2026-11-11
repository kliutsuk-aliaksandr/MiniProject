# GitHub Branch Ruleset — MiniProject

**Status: pending manual application in GitHub Settings.** This document is the exact configuration to create. No ruleset has been created by this session.

## Why this is not yet active

- Repository metadata confirms the connected account has `admin: true`.
- `GET /repos/kliutsuk-aliaksandr/MiniProject/rulesets?includes_parents=true` returned an empty list (`[]`).
- The connected GitHub tool exposes Rulesets through a read-only `fetch` action; it has no create/update ruleset operation.
- Reading classic branch protection returned HTTP 403, so classic protection status could not be verified.

The settings page is: https://github.com/kliutsuk-aliaksandr/MiniProject/settings/rules

## Create this ruleset

Open **Settings → Rules → Rulesets → New ruleset → New branch ruleset**.

| Setting | Value |
|---|---|
| Ruleset name | `Protect main and dev` |
| Enforcement status | **Active** |
| Target branches | Include by pattern: `main` and `dev` |
| Bypass list | No bypass actors |

### Branch protections

Enable:

1. **Restrict deletions** — protects both long-lived branches from accidental deletion.
2. **Require a pull request before merging**.
3. **Require status checks to pass before merging**.
4. **Require branches to be up to date before merging** (strict checks).
5. **Block force pushes**.

For pull request approvals, keep the required approval count at **0** while this is maintained by a single active developer; self-approval does not provide meaningful review and would block the PR workflow. Once another maintainer/reviewer is available, raise this to **1 approving review** (and optionally dismiss stale approvals).

### Required status checks

Add these exact GitHub Actions check names:

- `Build, tests and release bundle`
- `Verify Gradle Wrapper on Windows`
- `UI instrumentation tests (API 23)`
- `UI instrumentation tests (API 29)`
- `UI instrumentation tests (API 36)`

These check names were observed as successful on the current feature commit. Select the **GitHub Actions** source when the UI offers an expected source selector.

### Merge policy

The repository currently allows merge commits, squash merges and rebase merges. Prefer **Squash and merge** for feature → dev and dev → main to keep the long-lived branch history readable. Do not enable **Require linear history** until merge commits are disabled in repository merge settings; otherwise the selected merge methods and ruleset would conflict.

## Expected workflow

`feature/* → dev → main`

- Feature work is checked by the PR workflow when the pull request targets `dev`.
- `dev` receives changes through PRs.
- Releases move from `dev` to `main` through a separate PR.
- Direct pushes, force pushes and deletions on `main`/`dev` should be blocked by the ruleset.

## Verify after saving

1. Confirm the ruleset is listed as **Active** on the Rulesets page.
2. Confirm its target list includes only `main` and `dev`.
3. Confirm the five required checks are shown.
4. A direct push to either protected branch should be rejected; the feature PR #3 should remain eligible to merge when all checks pass.
