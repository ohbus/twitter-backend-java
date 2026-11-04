---
title: API guide
description: Integrate with the version 1 Moo REST API.
---

# API guide

All application routes use the `/v1` prefix. JSON is the primary request and
response format, timestamps are ISO-8601 UTC instants, and list endpoints use
Spring Data page responses.

Start with [authentication and CSRF](authentication.md), browse the
[endpoint guide](endpoints.md), then inspect the [generated reference](reference.md).

## Common conventions

- Bearer tokens use `Authorization: Bearer <jwt>`.
- State-changing requests require the CSRF cookie and matching header.
- Page requests accept `page`, `size`, and `sort`.
- The default page size is 20 and the maximum is 100.
- Validation failures and handled exceptions use the documented error model.

## In this section

| Page | Focus |
| --- | --- |
| [Authentication and CSRF](authentication.md) | Token lifecycle and browser-safe write requests |
| [Endpoints](endpoints.md) | All current `/v1` routes and access requirements |
| [Models and errors](models.md) | Request, response, page, and error shapes |
| [Test the API](testing.md) | Swagger UI and curl examples |
| [Generated reference](reference.md) | Code-derived OpenAPI parameters and schemas |
