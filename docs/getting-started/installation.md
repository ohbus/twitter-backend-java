---
title: Installation
description: Install Java 25 and run Moo with Maven or Docker Compose.
---

# Installation

## Prerequisites

- Git
- JDK 25
- Docker with Docker Compose, when using containerized dependencies

Maven does not need a separate installation; use the checked-in Maven Wrapper.

## Clone and configure

```bash
git clone git@github.com:ohbus/twitter-backend-java.git
cd twitter-backend-java
cp .env.example .env
```

The example values are for local development only. Never commit real secrets.

## Run the full stack

```bash
docker compose up -d
```

The Compose stack exposes the API on `http://localhost:8082` and Adminer on
`http://localhost:8083`.

## Run the application natively

Start only the development dependencies:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Then run the API:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The native API defaults to `http://localhost:8080`.

## Verify the checkout

```bash
./mvnw clean verify
```

This formats and compiles the code, runs tests, and creates the application JAR.
