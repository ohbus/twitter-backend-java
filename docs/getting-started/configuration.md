---
title: Configuration
description: Understand Moo profiles, database settings, and security environment variables.
---

# Configuration

Moo has `dev`, `test`, and `prod` Spring profiles under
`src/main/resources/`. Select a profile explicitly when starting the
application.

## Security values

| Variable | Purpose | Default |
| --- | --- | --- |
| `JWT_SECRET` | HMAC signing secret; use at least 32 random bytes | Required outside tests |
| `JWT_EXPIRATION` | Access-token lifetime in milliseconds | `900000` |
| `JWT_REFRESH_EXPIRATION` | Refresh-token lifetime in milliseconds | `604800000` |

## Production database

The production profile reads:

- `MYSQL_DB_HOST`
- `MYSQL_DB_PORT`
- `MYSQL_DB_NAME`
- `MYSQL_DB_UNAME`
- `MYSQL_DB_PASSWD`

The development profile uses MySQL on `localhost:3306` with the local Compose
credentials. Tests use an in-memory H2 database in MySQL compatibility mode.

## API documentation

Springdoc serves `/v3/api-docs` and `/swagger-ui/index.html` outside the
production profile. Production disables both endpoints; the generated public
reference is available from this documentation site.
