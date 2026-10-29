# Moo: Twitter Clone Backend

[![Master CI](https://github.com/scaleracademy/twitter-backend-java/actions/workflows/master-ci.yml/badge.svg)](https://github.com/scaleracademy/twitter-backend-java/actions/workflows/master-ci.yml)
[![PR Checker](https://github.com/scaleracademy/twitter-backend-java/actions/workflows/pr-checker.yml/badge.svg)](https://github.com/scaleracademy/twitter-backend-java/actions/workflows/pr-checker.yml)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

A robust, high-performance Twitter clone backend built with the latest
industry standards.

## 🚀 Modern Tech Stack

For a detailed breakdown of our architectural choices, visit our
[Technology Selection](https://github.com/scaleracademy/twitter-backend-java/wiki/Technology-Selections)
wiki page.

- **Java 25** (Microsoft Build of OpenJDK)
- **Spring Boot 4.1** (Spring Framework 7.0)
- **Jakarta EE 11** (Jakarta Persistence & Servlet APIs)
- **Virtual Threads (Project Loom)** enabled for massive concurrency.
- **JWT-based Security** via Spring Security 7.0.
- **Spring Data JPA** with MySQL/H2 support.
- **OpenTelemetry** for native observability.
- **Springdoc OpenAPI 3** for Swagger documentation.

## 🛠️ Getting Started

### Prerequisites

- JDK 25+
- Maven 3.9+
- Docker & Docker Compose (optional)

### Quick Start

Choose the setup that fits your workflow. For more details, see our
[Installation Guide](https://github.com/scaleracademy/twitter-backend-java/wiki/Installation).

#### Option A: Zero-Installation (Full Stack)

Ideal for testing or a quick look. Runs everything in Docker.

1. `cp .env.example .env`
2. `docker compose up -d`
3. API: `http://localhost:8082` | DB Admin: `http://localhost:8083`

#### Option B: Native Development (Dependencies Only)

Ideal for coding. Runs DB in Docker, App in your IDE/CLI.

1. `cp .env.example .env`
2. `docker compose -f docker-compose.dev.yml up -d`
3. Configure your IDE using our [IDE Instructions](https://github.com/scaleracademy/twitter-backend-java/wiki/IDE-Instructions).
4. Run the **"Moo API"** configuration in IntelliJ IDEA, or use:
   `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
5. API: `http://localhost:8080` | DB Admin: `http://localhost:8083`

For advanced configuration, visit [Project Configuration](https://github.com/scaleracademy/twitter-backend-java/wiki/Project-Configuration).

State-changing requests use the configured CSRF cookie/header pair in addition
to JWT authentication. Timestamps in API responses use ISO-8601 UTC values.

## 📖 API Documentation

Access the Interactive Swagger UI at:
👉 `http://localhost:8082/swagger-ui/index.html`

Comprehensive endpoint details and data contracts are available at:

- [API Endpoints Overview](https://github.com/scaleracademy/twitter-backend-java/wiki/API-Endpoints)
- [Request and Response Models](https://github.com/scaleracademy/twitter-backend-java/wiki/Request-and-Response-Models)
- [How to Test Endpoints](https://github.com/scaleracademy/twitter-backend-java/wiki/How-to-Test-Endpoints)

## 🏗️ Architecture & Best Practices

- **Scalable Pagination**: All list endpoints use `Pageable`. See our
  [Entity Design](https://github.com/scaleracademy/twitter-backend-java/wiki/Entity-Design).
- **Distroless Containers**: Secure, minimal runtime environment.
- **Multi-Arch Support**: Images optimized for `amd64` and `arm64`.
- **Zero-Qualified Names**: The codebase strictly uses imports.
- **Null Safety**: Integrated **JSpecify** annotations.

### Quality and coverage

The coverage target is 100% line and branch coverage for behavior-bearing code.
Tests should prove authorization, validation, persistence, and error behavior;
they should not be added only to inflate a metric. Generated/data-only models
and framework bootstrap glue are excluded consistently from JaCoCo and Sonar.
Run `./mvnw clean verify` and inspect `target/site/jacoco/index.html` before
requesting review. The detailed testing strategy is in
[`docs/quality.md`](docs/quality.md).

## 🤖 CI/CD Pipeline

Our pipeline is optimized for robust speed and reliability. Learn more about
how we maintain quality in [Collaboration](https://github.com/scaleracademy/twitter-backend-java/wiki/Collaboration)
and [Static Analysis](https://github.com/scaleracademy/twitter-backend-java/wiki/Static-Analysis).

- **PR Checker**: Cross-platform linting and multi-arch smoke builds.
- **Master CI**: Artifact archival, CodeQL scanning, and official distribution.

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md)
and the following wiki sections:

- [Working with Issues](https://github.com/scaleracademy/twitter-backend-java/wiki/Working-with-issues)
- [Reviewing Pull Requests](https://github.com/scaleracademy/twitter-backend-java/wiki/Reviewing-pull-requests)
- [Recognizing Contributors](https://github.com/scaleracademy/twitter-backend-java/wiki/Recognizing-contributors)

For our future vision, check the [Roadmap](https://github.com/scaleracademy/twitter-backend-java/wiki/Roadmap).

## 📜 License

This project is licensed under the **GNU Affero General Public License v3.0**.
See [LICENSE](LICENSE) for details.
