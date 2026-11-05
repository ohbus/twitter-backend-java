---
title: Technology choices
description: Review the current platform and the role of each major dependency.
---

# Technology choices

| Technology | Role |
| --- | --- |
| Java 25 | Runtime, records, and virtual threads |
| Spring Boot 4.1 / Framework 7 | Application platform and dependency management |
| Spring MVC | REST controllers and validation |
| Spring Security | JWT authentication, authorization, and CSRF |
| Spring Data JPA / Hibernate | Persistence and pagination |
| MySQL / H2 | Runtime and test databases |
| MapStruct | Entity-to-model mapping |
| Springdoc OpenAPI | Runtime API description and Swagger UI |
| Micrometer / OpenTelemetry | Metrics and traces |
| JaCoCo / Sonar / CodeQL | Coverage and static analysis |

JSpecify annotates service and repository contracts for nullness. The public
API uses records and Jakarta Validation. Lombok is deliberately absent.
