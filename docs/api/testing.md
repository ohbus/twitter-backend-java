---
title: Test the API
description: Exercise Moo locally with Swagger UI or curl.
---

# Test the API

When the API runs outside production, open Swagger UI at
`http://localhost:8080/swagger-ui/index.html`. The full-stack Compose port is
`8082` instead.

With curl, first create a cookie jar and capture CSRF:

```bash
curl -c cookies.txt http://localhost:8080/v1/csrf
```

Read the `XSRF-TOKEN` value from `cookies.txt`, then use it in a write request:

```bash
curl -b cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: <cookie-value>' \
  -d '{"username":"alice","password":"correct-horse-battery-staple"}' \
  http://localhost:8080/v1/authenticate
```

Add `-H 'Authorization: Bearer <jwt>'` for protected endpoints. Use disposable
local credentials and never paste production tokens into command history.
