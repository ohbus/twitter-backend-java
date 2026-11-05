---
title: Contribution workflow
description: Use the repository branch, test, commit, and pull-request conventions.
---

# Contribution workflow

## Branch and change

Fork when you do not have write access, then create a descriptive branch from
`master`. Keep the change scoped to one issue and preserve the existing
Controller → Service → Repository design.

## Verify

```bash
./mvnw clean verify
```

For API changes, update OpenAPI metadata and Markdown guidance. For endpoint
changes, include integration tests; for service behavior, include unit tests.

## Commit and rebase

Use a concise Conventional Commit subject such as `fix:`, `feat:`, `docs:`, or
`chore:`. GPG signing is welcome. A Signed-off-by trailer is not required.

Before review:

```bash
git remote add upstream https://github.com/ohbus/twitter-backend-java.git # once
git fetch upstream
git rebase upstream/master
```

Do not force-push shared branches. A contributor may use
`git push --force-with-lease` on their own pull-request branch after a rebase.
