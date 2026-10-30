# Repository Guidelines

## Project Structure & Module Organization

This is a single Spring Boot Maven application. Production code is under
`src/main/java/xyz/subho/clone/twitter/`, organized by responsibility:
`controller`, `service`, `repository`, `entity`, `model`, `security`, `config`,
`exception`, and `utility`. Runtime profiles and other resources are in
`src/main/resources/`. Tests mirror the Java package tree under
`src/test/java/`. Docker Compose files provide MySQL/Adminer and full-stack
development; `.github/workflows/` contains CI definitions.

## Build, Test, and Development Commands

- `./mvnw clean verify` - format, compile, test, and package the application.
- `./mvnw test` - run the JUnit/Spring Boot test suite.
- `./mvnw spotless:apply` - apply Google Java Format through Spotless.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` - run locally with
  the development profile.
- `docker compose up -d` - start the full stack; use
  `docker compose -f docker-compose.dev.yml up -d` for database dependencies
  only. On Windows, use `mvnw.cmd` equivalents.

## Coding Style & Naming Conventions

Use Java 25, four-space indentation, Google Java Format, and imports instead of
inline fully qualified names. Classes and interfaces use `PascalCase`; methods,
fields, and variables use `camelCase`; constants use `UPPER_SNAKE_CASE`.
Preserve `Controller -> Service -> Repository` layering, expose DTO/model
types rather than JPA entities, use Jakarta validation, and annotate service
and repository contracts with JSpecify nullability.

## Testing Guidelines

Add unit tests for new service logic and integration tests for new endpoints.
Name tests after the behavior they verify, and run `./mvnw test` before pushing.
Use `mvnw jacoco:report` to inspect coverage changes.

## Commit & Pull Request Guidelines

Use concise Conventional Commit subjects such as `feat:`, `fix:`, `docs:`, or
`chore:`. Use GPG signing when configured (`git commit -S` or
`commit.gpgsign=true`); do not add `Signed-off-by` trailers by default. Before
requesting review, rebase onto `master`, ensure PR Checker is green, update
Swagger/OpenAPI metadata for API changes, remove debug logs/TODOs, and include
a focused description plus linked issue context where applicable.

## Security & Configuration

Use the authenticated `Principal` for current-user behavior; never hardcode
user IDs or generate replacement IDs. Keep secrets in environment variables or
local profile configuration, never in commits. Follow `CODE_OF_CONDUCT.md` and
`CONTRIBUTING.md` for project policies.
