<!--
  Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
  Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->

# Contributing

First off, thank you for considering contributing to Moo! It's people like you
that make Moo such a great tool for the community.

This document provides guidelines and standards for contributing to this
project. Following these helps us maintain a high-quality, high-performance
codebase.

## Code of Conduct

By participating in this project, you are expected to uphold our
[Code of Conduct](CODE_OF_CONDUCT.md). Please report unacceptable behavior to
the project maintainers.

## Getting Started

### 1. Fork and Clone

- Fork the repository on GitHub.
- Clone your fork locally:

    ```bash
    git clone https://github.com/YOUR_USERNAME/twitter-backend-java.git
    cd twitter-backend-java
    ```

### 2. Environment Setup

- **JDK:** Ensure you have **Microsoft Build of OpenJDK 25** installed.
- **Maven:** Use the provided wrapper `./mvnw`.
- **Database:** Local MySQL or H2 (configured via profiles).
- **IDE:** IntelliJ IDEA is recommended (Google Java Format plugin encouraged).

## Development Standards

To keep the codebase clean and stable, we enforce the following rules:

### 1. Code Style and Formatting

- We use **Spotless** with **Google Java Format**.
- **Always** format your code before committing:

    ```bash
    ./mvnw spotless:apply
    ```

- The CI will fail if the code is not perfectly formatted.

### 2. Imports vs. Qualified Names

- **Rule:** Never use fully qualified package names inline (e.g. `java.util.List`).
- **Action:** Always use `import` statements at the top of the file.

### 3. Null Safety (JSpecify)

- Every Service and Repository method must be annotated with **JSpecify**.
- Use `@Nullable` for optional return types or parameters.
- Use `@NonNull` for mandatory values.
- Default to `@NonNull` unless there is a valid reason for nullability.

### 4. Robust Architecture

- **Layering:** strictly follow `Controller` -> `Service` -> `Repository`.
- **Models (DTOs):** Never expose JPA Entities directly in Controllers. Use
  `Model` classes and `Mapper` utilities.
- **Validation:** Use `jakarta.validation` constraints (`@NotBlank`, `@Size`,
  etc.) on all Models.
- **Scalability:** Always use `Pageable` for list endpoints. Returning `List<T>`
  is strictly prohibited for feed-based APIs.

### 5. Security

- Use the `Principal` object in Controllers to identify the authenticated user.
- Never use `UUID.randomUUID()` or hardcoded IDs for "current user" logic.

## Git Flow and Branching

1. **Branch Naming:**

    - Features: `feature/short-description`
    - Bug Fixes: `fix/issue-id-description`
    - Chores/Docs: `chore/description`

2. **Commit Messages:**

    - Use conventional commits (e.g., `feat: ...`, `fix: ...`, `docs: ...`).
    - Keep messages concise but descriptive.

3. **Rebasing:**

    - Always rebase your feature branch against the latest `master` before
      submitting a PR.

## Testing Requirements

- **Unit Tests:** Mandatory for all new logic in the `service` layer.
- **Integration Tests:** Required for new API endpoints.
- **Coverage:** Aim for high coverage. Ensure `mvn jacoco:report` shows no
  regressions.
- Run all tests locally before pushing:

    ```bash
    ./mvnw test
    ```

## Pull Request Process

### 1. Eligibility for Review (The "Green Gate")

To respect the time of our maintainers, a Pull Request will **not** be
reviewed until the following conditions are met:

- **Commit Signing:** GPG signing is recommended when configured; `Signed-off-by`
  trailers are not required.
- **Green CI:** The `PR Checker` workflow must pass successfully on GitHub.
- **No Conflicts:** The PR must be rebased against the latest `master`.

### 2. Implementation Steps

1. **Update Documentation:** If you add or change an API, ensure the
   Swagger/OpenAPI metadata is updated.

2. **Self-Review:** Go through your changes and ensure no "TODOs" or debug logs
   are left behind.

3. **Assign Reviewers:** Assign at least one maintainer for review once the CI
   is green. Be responsive to feedback!

## License

By contributing, you agree that your contributions will be licensed under the
**GNU Affero General Public License v3.0**.
