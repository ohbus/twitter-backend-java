# Moo: Twitter Clone Backend

[![Master CI](https://github.com/ohbus/twitter-backend-java/actions/workflows/master-ci.yml/badge.svg)](https://github.com/ohbus/twitter-backend-java/actions/workflows/master-ci.yml)
[![PR Checker](https://github.com/ohbus/twitter-backend-java/actions/workflows/pr-checker.yml/badge.svg)](https://github.com/ohbus/twitter-backend-java/actions/workflows/pr-checker.yml)
[![Documentation](https://github.com/ohbus/twitter-backend-java/actions/workflows/docs.yml/badge.svg)](https://subho.xyz/twitter-backend-java/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

A Spring Boot backend for a Twitter-style social network, built with Java 25,
Spring Boot 4.1, Spring Security, Spring Data JPA, and MySQL.

## Documentation

The complete, searchable documentation is published at
**[subho.xyz/twitter-backend-java](https://subho.xyz/twitter-backend-java/)**.
Its Markdown source lives in [`docs/`](docs/README.md), next to the code it
describes.

- [Getting started](docs/getting-started/README.md)
- [Architecture](docs/architecture/README.md)
- [API guide](docs/api/README.md)
- [Quality and CI](docs/quality.md)
- [Contributing](CONTRIBUTING.md)

## Technology

- Java 25 with virtual threads
- Spring Boot 4.1 and Spring Framework 7
- Spring MVC, Spring Security, and Jakarta Validation
- Spring Data JPA with MySQL and H2
- JWT access tokens and rotating refresh tokens
- Springdoc OpenAPI, Micrometer, and OpenTelemetry
- JaCoCo, Sonar, CodeQL, and multi-architecture containers

## Quick start

### Full stack

```bash
cp .env.example .env
# Set DB_USER to a non-root username; set DB_PASS and DB_ROOT_PASS in .env.
# Set JWT_SECRET to at least 32 UTF-8 bytes.
./mvnw clean verify
docker compose up -d
```

The API is available at `http://localhost:8082` and Adminer at
`http://localhost:8083`.

### Backend development

```bash
cp .env.example .env
# For a new database, set a non-root DB_USER, DB_PASS, and DB_ROOT_PASS=root.
docker compose -f docker-compose.dev.yml up -d
export JWT_SECRET="$(openssl rand -hex 32)"
export DB_NAME=twitter_clone # Match DB_NAME in .env if you changed the default.
export SPRING_DATASOURCE_PASSWORD=root
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Compose reads `.env`, but Maven does not. For an existing `moo-data` volume,
set `SPRING_DATASOURCE_PASSWORD` to the root password used when that volume was
first created; changing `DB_ROOT_PASS` does not update an existing database.
If you need data created by the former native profile, use
`export DB_NAME=twitter` before starting Maven.

The native API is available at `http://localhost:8080`. Swagger UI is at
`/swagger-ui/index.html` outside production.

State-changing requests require the `XSRF-TOKEN` cookie value in the
`X-XSRF-TOKEN` header, including public POST endpoints. Start with
`GET /v1/csrf`; authenticated endpoints also require a Bearer JWT. See the
[authentication guide](docs/api/authentication.md) for the complete flow.

## Verify

```bash
./mvnw clean verify
```

Coverage output is written to `target/site/jacoco/index.html`.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) and the
[documentation workflow](docs/contributing/workflow.md) before opening a pull
request. Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

Moo is licensed under the [GNU Affero General Public License v3.0](LICENSE).
