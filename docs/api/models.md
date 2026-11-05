---
title: Models and errors
description: Understand the primary Moo request, response, page, and error shapes.
---

# Models and errors

## Authentication

Authentication accepts `username` and `password`. A successful response
contains `jwt` and `refreshToken`. Refresh requests contain only
`refreshToken`.

## Users

User registration accepts username, name, password, email, avatar, and bio.
Returned user models also contain ID, follower/following counters, and verified
state. Search results omit password and email.

## Posts

Posts contain text, author ID, up to four image URLs, counters, optional original
and reply IDs, timestamp, hashtags, and mentions. Text is required and limited
to 240 characters.

## Pages

Paged responses use Spring Data fields including `content`, `number`, `size`,
`totalElements`, `totalPages`, `first`, and `last`.

## Handled errors

Controller exceptions return:

```json
{
  "statusCode": 400,
  "timestamp": "2026-11-04T10:00:00Z",
  "message": "Validation failed: username must not be blank",
  "description": "uri=/v1/users"
}
```

Malformed input returns 400, conflicting data returns 409, missing resources
return 404, rejected authorization returns 401 or 403, and unexpected failures
return 500 without exposing internal exception details.
