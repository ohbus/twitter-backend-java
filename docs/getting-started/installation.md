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

Set `DB_USER` to a non-root username, and set `DB_PASS` and `DB_ROOT_PASS` to
non-empty values in `.env`. Set `JWT_SECRET` to at least 32 UTF-8 bytes. Never
commit real secrets.

## Run the full stack

```bash
./mvnw clean verify
docker compose up -d
```

The Compose stack exposes the API on `http://localhost:8082` and Adminer on
`http://localhost:8083`.

## Run the application natively

Start only the development dependencies:

```bash
# For a new database, set DB_ROOT_PASS=root, a non-root DB_USER, and DB_PASS.
docker compose -f docker-compose.dev.yml up -d
```

Then run the API:

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
export DB_NAME=twitter_clone # Match DB_NAME in .env if you changed the default.
export SPRING_DATASOURCE_PASSWORD=root
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Compose reads `.env`, but Maven does not. An existing `moo-data` volume keeps
the root password used when it was first created. In that case, export that
password as `SPRING_DATASOURCE_PASSWORD` instead of `root`; do not delete the
volume or expect changing `DB_ROOT_PASS` to update it.
If you need data created by the former native profile, use
`export DB_NAME=twitter` before starting Maven.

The native API defaults to `http://localhost:8080`.

## Verify the checkout

```bash
./mvnw clean verify
```

This formats and compiles the code, runs tests, and creates the application JAR.
